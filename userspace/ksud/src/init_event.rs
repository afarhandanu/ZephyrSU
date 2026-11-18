#[cfg(target_arch = "aarch64")]
use crate::kpm;
use crate::module::{ScriptWait, handle_updated_modules, prune_modules};
use crate::utils::is_safe_mode;
use crate::{
    assets, defs, ksucalls, magic_mount, metamodule, restorecon,
    utils::{self},
};
use anyhow::{Context, Result};
use log::{error, info, warn};
use std::{path::Path, time::Instant};

/// Run a built-in Zygisk boot step. A failure is logged, never propagated: a broken
/// built-in Zygisk must not abort the module stage.
fn builtin_zygisk_step(step: &str, action: impl FnOnce() -> Result<()>) {
    if let Err(e) = action() {
        warn!("built-in Zygisk {step} failed: {e}");
    }
}

pub fn on_post_fs_data() -> Result<()> {
    if let Err(e) = ksucalls::ensure_uapi_version_matched() {
        error!("{e:#}, skip on_post_fs_data");
        return Ok(());
    }

    ksucalls::report_post_fs_data();

    // Start before safe-mode/module early returns; expired leases survive reboot.
    if let Err(e) = crate::temporary_root::start() {
        error!("start temporary root watcher failed: {e:#}");
    }

    utils::umask(0);

    // Clear all temporary module configs early
    if let Err(e) = crate::module_config::clear_all_temp_configs() {
        warn!("clear temp configs failed: {e}");
    }

    let _ = catch_bootlog("logcat", &["logcat", "-b", "all"]);
    let _ = catch_bootlog("dmesg", &["dmesg", "-w", "-r"]);

    if utils::has_magisk() {
        warn!("Magisk detected, skip post-fs-data!");
        return Ok(());
    }

    let safe_mode = crate::utils::is_safe_mode();
    let wait = ScriptWait::Until(Instant::now() + defs::BOOT_STAGE_TIMEOUT);

    if safe_mode {
        // we should still ensure module directory exists in safe mode
        // because we may need to operate the module dir in safe mode
        warn!("safe mode, skip common post-fs-data.d scripts");
    } else {
        // Then exec common post-fs-data scripts
        if let Err(e) = crate::module::exec_common_scripts("post-fs-data.d", wait) {
            warn!("exec common post-fs-data scripts failed: {e}");
        }
    }

    assets::ensure_binaries(true).with_context(|| "Failed to extract bin assets")?;

    crate::hot_install::clean_trash();

    // if we are in safe mode, we should disable all modules
    if safe_mode {
        warn!("safe mode, skip post-fs-data scripts and disable all modules!");
        if let Err(e) = crate::module::disable_all_modules() {
            warn!("disable all modules failed: {e}");
        }
        if let Err(e) = crate::kernel_module::disable_all_for_safe_mode() {
            warn!("disable managed kernel modules failed: {e}");
        }
        return Ok(());
    }

    if let Err(e) = handle_updated_modules() {
        warn!("handle updated modules failed: {e}");
    }

    if let Err(e) = prune_modules() {
        warn!("prune modules failed: {e}");
    }

    // Refresh /metadata/watchdog/ksu/{modules.rc,preinit/} so the next boot's kernel hook sees the
    // current module set. Acts as a safety net when state was changed outside
    // of ksud's normal mutation commands.
    if let Err(e) = crate::module::regenerate_preinit() {
        warn!("regenerate preinit failed: {e:#}");
    }

    if let Err(e) = restorecon::restorecon() {
        warn!("restorecon failed: {e}");
    }

    // load sepolicy.rule
    if crate::module::load_sepolicy_rule().is_err() {
        warn!("load sepolicy.rule failed");
    }

    if let Err(e) = crate::builtin_zygisk::apply_sepolicy() {
        warn!("apply builtin zygisk sepolicy failed: {e}");
    }

    if let Err(e) = crate::profile::apply_sepolies() {
        warn!("apply root profile sepolicy failed: {e}");
    }

    crate::feature::init_forced_features();

    // load feature config
    if is_safe_mode() {
        warn!("safe mode, skip load feature config");
    } else if let Err(e) = crate::feature::init_features() {
        warn!("init features failed: {e}");
    }

    crate::extension::init_forced_extensions();
    if is_safe_mode() {
        warn!("safe mode, skip load extension config");
    } else if let Err(e) = crate::extension::init_extensions() {
        warn!("init extensions failed: {e}");
    }

    // Apply a persisted UTS View template (no-op without the kernel driver;
    // failures are recorded as a runtime error, never fail the boot).
    crate::uts_view::boot_apply();

    // Reapply persisted network isolation (no-op without the kernel driver;
    // failures are only logged, never fail the boot).
    crate::netisolate::boot_apply();

    match crate::kernel_module::autoload_with_startup_timeout() {
        Ok(Some(report)) => {
            if !report.failures.is_empty() {
                for failure in &report.failures {
                    warn!(
                        "kernel module autoload {} failed: {}",
                        failure.id, failure.message
                    );
                }
            }
            info!(
                "kernel module autoload: {} attempted, {} loaded, {} skipped",
                report.attempted.len(),
                report.loaded.len(),
                report.skipped.len()
            );
        }
        Ok(None) => warn!("kernel module autoload is still running in the background"),
        Err(e) => warn!("kernel module autoload scheduler failed: {e:#}"),
    }

    #[cfg(target_arch = "aarch64")]
    if let Err(e) = kpm::booted_load() {
        warn!("KPM: Failed to start KPM watcher: {e}");
    }

    // A disabled built-in kept its files for the rest of the session so a running
    // monitor was not disturbed; this boot is where they go.
    builtin_zygisk_step("cleanup", crate::builtin_zygisk::cleanup_disabled);

    // bootstrap built-in zygisk before module post-fs-data
    builtin_zygisk_step("bootstrap", crate::builtin_zygisk::bootstrap);

    if let Err(e) = crate::sui::post_fs_data(safe_mode) {
        warn!("Sui startup skipped/failed: {e:#}");
    }

    // execute metamodule post-fs-data script first (priority)
    if let Err(e) = metamodule::exec_stage_script("post-fs-data", wait) {
        warn!("exec metamodule post-fs-data script failed: {e}");
    }

    // exec modules post-fs-data scripts
    if let Err(e) = crate::module::exec_stage_script("post-fs-data", wait) {
        warn!("exec post-fs-data scripts failed: {e}");
    }

    // exec lua script on post-fs-data
    #[cfg(all(target_os = "android", target_arch = "aarch64"))]
    if let Err(e) = crate::module::exec_stage_lua("post-fs-data", true, "kernelsu") {
        warn!("Failed to exec post-fs-data lua: {e}");
    }

    // Plugin callbacks share the same boot budget as the scripts above: a plugin
    // that no longer fits is left for the next boot rather than extending the wait.
    #[cfg(all(target_os = "android", target_arch = "aarch64"))]
    if let Err(e) = crate::plugin::stage::run(
        Path::new(defs::PLUGIN_DIR),
        Path::new(defs::PLUGIN_LOCK_DIR),
        Path::new(defs::PLUGIN_STATE_DIR),
        "post-fs-data",
        match wait {
            ScriptWait::Until(instant) => Some(instant),
            _ => None,
        },
    ) {
        warn!("exec plugin post-fs-data callbacks failed: {e}");
    }

    crate::boot_hide::boot_apply();

    // load system.prop
    if let Err(e) = crate::module::load_system_prop() {
        warn!("load system.prop failed: {e}");
    }

    // Folk Mount: run the selected provider (metamodule script or the built-in
    // executor; never both) before the post-mount stage to preserve the
    // KernelSU contract.
    if let Err(e) = magic_mount::mount_selected_provider() {
        warn!("module mount failed: {e:#}");
    }

    // Load umount config and apply to kernel
    if let Err(e) = crate::umount::load_umount_config() {
        warn!("load umount config failed: {e}");
    }

    run_stage("post-mount", wait);

    std::env::set_current_dir("/").with_context(|| "failed to chdir to /")?;

    Ok(())
}

pub fn run_stage(stage: &str, wait: ScriptWait) {
    utils::umask(0);

    if utils::has_magisk() {
        warn!("Magisk detected, skip {stage}");
        return;
    }

    if crate::utils::is_safe_mode() {
        warn!("safe mode, skip {stage} scripts");
        return;
    }

    if let Err(e) = crate::module::exec_common_scripts(&format!("{stage}.d"), wait) {
        warn!("Failed to exec common {stage} scripts: {e}");
    }

    // execute metamodule stage script first (priority)
    if let Err(e) = metamodule::exec_stage_script(stage, wait) {
        warn!("Failed to exec metamodule {stage} script: {e}");
    }

    // execute regular modules stage scripts
    if let Err(e) = crate::module::exec_stage_script(stage, wait) {
        warn!("Failed to exec {stage} scripts: {e}");
    }

    // run lua stage script
    #[cfg(all(target_os = "android", target_arch = "aarch64"))]
    if let Err(e) =
        crate::module::exec_stage_lua(stage, !matches!(wait, ScriptWait::NoWait), "kernelsu")
    {
        warn!("Failed to exec {stage} lua: {e}");
    }

    // Later stages do not block the boot, so plugin callbacks are dispatched in
    // the background. The bounded stages above keep their deadline instead.
    #[cfg(all(target_os = "android", target_arch = "aarch64"))]
    if matches!(wait, ScriptWait::NoWait)
        && let Err(e) = crate::plugin::stage::background(stage)
    {
        warn!("Failed to dispatch plugin {stage} callbacks: {e}");
    }
}

pub fn on_services() {
    if let Err(e) = ksucalls::ensure_uapi_version_matched() {
        error!("{e:#}, skip on_services");
        return;
    }

    match ksucalls::report_services() {
        Ok(true) => {}
        Ok(false) => {
            info!("services already started, skipping");
            return;
        }
        Err(e) => {
            error!("Failed to report services: {e:#}");
            return;
        }
    }

    info!("on_services triggered!");
    // Make sure the monitor survived post-fs-data, but do not talk to it: the zygote is
    // being started right now, and a `ctl rearm` would race the fork injection it is
    // already performing. The late attach belongs to boot completed.
    builtin_zygisk_step("monitor", crate::builtin_zygisk::ensure_monitor);

    if let Err(e) = crate::sui::services() {
        warn!("Sui health check failed: {e:#}");
    }
    run_stage("service", ScriptWait::NoWait);
}

pub fn on_boot_completed() {
    if let Err(e) = ksucalls::ensure_uapi_version_matched() {
        error!("{e:#}, skip on_boot_completed");
        return;
    }

    ksucalls::report_boot_complete();
    info!("on_boot_completed triggered!");

    // Safety net for a full boot: if the post-fs-data monitor never armed, start one
    // and attach the zygote/system_server that are already running. Idempotent when
    // the early boot already injected them.
    builtin_zygisk_step("arm", crate::builtin_zygisk::ensure_armed);

    // BootGuard receipt: a boot that confirmed injection clears the latch, a
    // boot that never did keeps its counted attempt. Never fails the boot.
    builtin_zygisk_step("boot-guard", crate::boot_guard::on_boot_completed);

    crate::preinit::on_boot_completed();

    run_stage("boot-completed", ScriptWait::NoWait);
}

fn catch_bootlog(logname: &str, command: &[&str]) -> Result<()> {
    use std::os::unix::process::CommandExt;
    use std::process::Stdio;

    let logdir = Path::new(defs::LOG_DIR);
    utils::ensure_dir_exists(logdir)?;
    let bootlog = logdir.join(format!("{logname}.log"));
    let oldbootlog = logdir.join(format!("{logname}.old.log"));

    if bootlog.exists() {
        std::fs::rename(&bootlog, oldbootlog)?;
    }

    let bootlog = std::fs::File::create(bootlog)?;

    let mut args = vec!["-s", "9", defs::BOOTLOG_TIMEOUT];
    args.extend_from_slice(command);
    // timeout -s 9 30s logcat > boot.log
    let result = unsafe {
        std::process::Command::new("timeout")
            .process_group(0)
            .pre_exec(|| {
                utils::switch_cgroups();
                Ok(())
            })
            .args(args)
            .stdout(Stdio::from(bootlog))
            .spawn()
    };

    if let Err(e) = result {
        warn!("Failed to start logcat: {e:#}");
    }

    Ok(())
}
