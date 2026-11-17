#![allow(clippy::missing_errors_doc, clippy::must_use_candidate)]

//! BootGuard: startup-failure latch for the built-in Zygisk.
//!
//! Adapted from FinaSU's FolkZygisk BootGuard (`zygisk.rs` + `startup_epoch.rs`),
//! re-wired onto Nova's NyaZygisk engine instead of the ReZygisk one. The engine
//! is untouched: the latch only decides whether a startup attempt may happen and
//! counts boots that never confirmed injection.
//!
//! One counted attempt per userspace cycle (a kernel boot, or a soft reboot once
//! its epoch is advanced): the attempt is recorded when `bootstrap()` starts the
//! monitor and cleared by `on_boot_completed()` when the zygote is confirmed
//! injected — or by a live switch that confirms injection the same way.
//! Three consecutive boots without a confirmed injection latch the
//! next boot's startup until the user runs `ksud zygisk recover` (or
//! `reset-failures`, which only clears the latch).
//!
//! A guard I/O error never blocks startup: an unreadable guard file resets to
//! empty with a warning, so a corrupt latch file cannot brick Zygisk. Only the
//! latched state itself refuses a start.

use anyhow::{Context, Result, ensure};
use log::{info, warn};
use serde::{Deserialize, Serialize};
use std::fs::{self, File, OpenOptions};
use std::io::Write;
use std::os::fd::AsRawFd;
use std::os::unix::fs::{OpenOptionsExt, PermissionsExt};
use std::path::Path;

/// Guard state, written after every startup attempt and boot receipt.
const BOOT_GUARD: &str = "/data/adb/ksu/zygisk-boot.json";
/// Serializes guard read-modify-write cycles (blocking: boot stages must not
/// spuriously skip the guard).
const GUARD_LOCK: &str = "/data/adb/ksu/zygisk-boot.lock";
/// Userspace-cycle identity, shared with nothing else (a minimal port of
/// FinaSU's `startup_epoch.rs`: only `current_or_create` and the soft-reboot
/// advance; no operation-lock machinery).
const EPOCH_STATE: &str = "/data/adb/ksu/startup_epoch.json";
const EPOCH_LOCK: &str = "/data/adb/ksu/startup_epoch.lock";
/// Failed boots before the latch engages.
const FAILURE_LIMIT: u32 = 3;

/// Why a startup failed, for the status report and the recovery UI. Only the
/// failure modes Nova's engine can actually observe: FinaSU's engine-install,
/// runtime-file and readiness-wait codes have no equivalent here.
#[derive(Clone, Copy, Debug, Deserialize, Eq, PartialEq, Serialize)]
#[serde(rename_all = "snake_case")]
pub enum StartupIssueCode {
    TracerMissing,
    MonitorSpawnFailed,
    InjectionUnconfirmed,
}

impl StartupIssueCode {
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::TracerMissing => "tracer_missing",
            Self::MonitorSpawnFailed => "monitor_spawn_failed",
            Self::InjectionUnconfirmed => "injection_unconfirmed",
        }
    }
}

#[derive(Clone, Debug, Deserialize, Serialize)]
#[serde(deny_unknown_fields)]
struct StartupIssue {
    code: StartupIssueCode,
    detail: String,
    boot_id: String,
    generation: u64,
    counted: bool,
}

/// Persisted latch state. Same JSON shape as FinaSU's, so a guard file written
/// by the other fork still parses.
#[derive(Default, Deserialize, Serialize)]
#[serde(deny_unknown_fields)]
struct BootGuard {
    #[serde(default)]
    boot_id: String,
    #[serde(default)]
    failures: u32,
    #[serde(default)]
    latched: bool,
    #[serde(default)]
    attempted_generation: u64,
    #[serde(default)]
    last_issue: Option<StartupIssue>,
}

impl BootGuard {
    fn prepare_cycle(&mut self, epoch: &StartupEpoch) {
        if self.attempted_generation == 0 && self.boot_id == epoch.boot_id {
            self.attempted_generation = epoch.generation;
        }
    }

    const fn blocked(&self) -> bool {
        self.latched || self.failures >= FAILURE_LIMIT
    }

    fn handled_this_cycle(&self, epoch: &StartupEpoch) -> bool {
        let same_boot = self.boot_id == epoch.boot_id;
        let same_generation = self.attempted_generation == epoch.generation;
        same_boot && same_generation
    }

    fn mark_attempt(
        &mut self,
        epoch: &StartupEpoch,
        code: StartupIssueCode,
        detail: impl Into<String>,
        counted: bool,
    ) {
        epoch.boot_id.clone_into(&mut self.boot_id);
        self.attempted_generation = epoch.generation;
        if counted {
            self.failures = self.failures.saturating_add(1);
            if self.failures >= FAILURE_LIMIT {
                self.latched = true;
            }
        }
        self.record_issue(epoch, code, detail, counted);
    }

    fn record_issue(
        &mut self,
        epoch: &StartupEpoch,
        code: StartupIssueCode,
        detail: impl Into<String>,
        counted: bool,
    ) {
        let detail = detail.into();
        self.last_issue = Some(StartupIssue {
            code,
            detail: diagnostic_detail(&detail),
            boot_id: epoch.boot_id.clone(),
            generation: epoch.generation,
            counted,
        });
    }

    fn mark_handled(&mut self, epoch: &StartupEpoch) {
        epoch.boot_id.clone_into(&mut self.boot_id);
        self.attempted_generation = epoch.generation;
    }

    fn clear_failures(&mut self, epoch: &StartupEpoch) {
        self.failures = 0;
        self.latched = false;
        self.last_issue = None;
        self.mark_handled(epoch);
    }
}

fn diagnostic_detail(detail: &str) -> String {
    detail
        .chars()
        .filter(|character| !character.is_control() || *character == '\t')
        .take(256)
        .collect()
}

/// One userspace cycle: a kernel boot, or a soft reboot once its epoch is
/// advanced. The generation is a global monotonic counter, so a new kernel
/// boot and a soft reboot never share one.
#[derive(Clone, Debug, Default, Deserialize, PartialEq, Eq, Serialize)]
#[serde(deny_unknown_fields)]
struct StartupEpoch {
    boot_id: String,
    generation: u64,
}

fn current_boot_id() -> Result<String> {
    let boot_id = fs::read_to_string("/proc/sys/kernel/random/boot_id")
        .context("Read kernel boot id")?
        .trim()
        .to_owned();
    ensure!(!boot_id.is_empty(), "Kernel boot id is empty");
    Ok(boot_id)
}

fn read_epoch(path: &Path) -> Result<Option<StartupEpoch>> {
    match fs::read(path) {
        Ok(bytes) => serde_json::from_slice(&bytes)
            .context("Invalid Android userspace startup epoch")
            .map(Some),
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => Ok(None),
        Err(error) => Err(error).context("Read Android userspace startup epoch"),
    }
}

fn next_generation(current: u64) -> Result<u64> {
    current
        .checked_add(1)
        .context("Android userspace startup generation overflow")
}

fn current_epoch() -> Result<StartupEpoch> {
    let boot_id = current_boot_id()?;
    let _lock = lock_file(Path::new(EPOCH_LOCK))?;
    let epoch = match read_epoch(Path::new(EPOCH_STATE))? {
        Some(epoch) if epoch.boot_id == boot_id && epoch.generation > 0 => epoch,
        Some(epoch) => StartupEpoch {
            boot_id,
            generation: next_generation(epoch.generation)?,
        },
        None => StartupEpoch {
            boot_id,
            generation: 1,
        },
    };
    if read_epoch(Path::new(EPOCH_STATE))?.as_ref() != Some(&epoch) {
        write_json(Path::new(EPOCH_STATE), &epoch)?;
    }
    Ok(epoch)
}

/// Advance the cycle counter for a soft reboot, so the replayed post-fs-data
/// gets a fresh startup attempt instead of sharing the stopped cycle's.
pub fn advance_epoch() -> Result<()> {
    let boot_id = current_boot_id()?;
    let _lock = lock_file(Path::new(EPOCH_LOCK))?;
    let previous = match read_epoch(Path::new(EPOCH_STATE))? {
        Some(epoch) => epoch,
        None => StartupEpoch {
            boot_id: boot_id.clone(),
            generation: 0,
        },
    };
    let epoch = StartupEpoch {
        boot_id,
        generation: next_generation(previous.generation)?,
    };
    write_json(Path::new(EPOCH_STATE), &epoch)?;
    Ok(())
}

fn lock_file(path: &Path) -> Result<File> {
    let parent = path.parent().context("Lock file has no parent")?;
    fs::create_dir_all(parent)?;
    let file = OpenOptions::new()
        .read(true)
        .write(true)
        .create(true)
        .truncate(false)
        .mode(0o600)
        .open(path)?;
    // SAFETY: file owns a valid descriptor for the duration of this call.
    if unsafe { libc::flock(file.as_raw_fd(), libc::LOCK_EX) } != 0 {
        return Err(std::io::Error::last_os_error()).context("Lock BootGuard state");
    }
    Ok(file)
}

fn write_json(path: &Path, value: &impl Serialize) -> Result<()> {
    let parent = path.parent().context("Missing state directory")?;
    fs::create_dir_all(parent)?;
    let mut file = tempfile::NamedTempFile::new_in(parent)?;
    file.as_file()
        .set_permissions(fs::Permissions::from_mode(0o600))?;
    serde_json::to_writer(&mut file, value)?;
    file.write_all(b"\n")?;
    file.as_file().sync_all()?;
    file.persist(path).context("Persist BootGuard state")?;
    File::open(parent)?.sync_all()?;
    Ok(())
}

fn load_guard() -> Result<BootGuard> {
    match fs::read(Path::new(BOOT_GUARD)) {
        Ok(bytes) => serde_json::from_slice(&bytes).context("Invalid built-in Zygisk boot guard"),
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => Ok(BootGuard::default()),
        Err(error) => Err(error).context("Read built-in Zygisk boot guard"),
    }
}

/// An unreadable guard file resets to empty with a warning instead of
/// blocking startup: a corrupt latch file must never brick Zygisk. The next
/// mutation persists a fresh file.
fn load_guard_or_default() -> BootGuard {
    match load_guard() {
        Ok(guard) => guard,
        Err(error) => {
            warn!("Built-in Zygisk boot guard is unreadable, starting fresh: {error:#}");
            BootGuard::default()
        }
    }
}

/// What `ksud zygisk status` and the Sui gate report about the latch.
#[derive(Clone, Debug, Default)]
pub struct GuardStatus {
    pub failures: u32,
    pub blocked: bool,
    pub failure_code: Option<String>,
    pub failure_detail: Option<String>,
    pub generation: u64,
}

pub fn status() -> Result<GuardStatus> {
    let epoch = current_epoch()?;
    let mut guard = load_guard()?;
    guard.prepare_cycle(&epoch);
    Ok(GuardStatus {
        failures: guard.failures,
        blocked: guard.blocked(),
        failure_code: guard
            .last_issue
            .as_ref()
            .map(|issue| issue.code.as_str().to_owned()),
        failure_detail: guard.last_issue.as_ref().map(|issue| issue.detail.clone()),
        generation: epoch.generation,
    })
}

/// Whether the latch currently refuses a start. Swallows I/O errors: a guard
/// that cannot be read never blocks startup.
pub fn is_blocked() -> bool {
    status().is_ok_and(|status| status.blocked)
}

/// Refuse a fresh monitor start while latched. The caller decides what a
/// refusal means: the boot path only logs it, a live switch reports it.
pub fn check_start_allowed() -> Result<()> {
    ensure!(
        !is_blocked(),
        "Built-in Zygisk startup is blocked by BootGuard after repeated startup failures; run `ksud zygisk recover` to try again"
    );
    Ok(())
}

/// Record this cycle's startup attempt, once: retries inside the same cycle
/// (services stage, late load, live switch) do not add another count.
pub fn record_startup_attempt() -> Result<()> {
    let _lock = lock_file(Path::new(GUARD_LOCK))?;
    let epoch = current_epoch()?;
    let mut guard = load_guard_or_default();
    if guard.handled_this_cycle(&epoch) {
        return Ok(());
    }
    guard.mark_attempt(
        &epoch,
        StartupIssueCode::InjectionUnconfirmed,
        "startup attempt recorded; complete zygote injection was not yet confirmed",
        true,
    );
    write_json(Path::new(BOOT_GUARD), &guard)?;
    Ok(())
}

/// Update the recorded failure detail without adding another count: the
/// attempt was already counted by [`record_startup_attempt`].
pub fn record_issue(code: StartupIssueCode, detail: impl Into<String>) -> Result<()> {
    let _lock = lock_file(Path::new(GUARD_LOCK))?;
    let epoch = current_epoch()?;
    let mut guard = load_guard_or_default();
    guard.record_issue(&epoch, code, detail, true);
    write_json(Path::new(BOOT_GUARD), &guard)?;
    Ok(())
}

/// A live switch that confirmed injection clears the latch the same way a
/// boot receipt does: the engine works, so earlier failed boots were transient.
pub fn record_confirmed_injection() -> Result<()> {
    let _lock = lock_file(Path::new(GUARD_LOCK))?;
    let epoch = current_epoch()?;
    let mut guard = load_guard_or_default();
    if guard.failures > 0 || guard.latched || guard.last_issue.is_some() {
        guard.clear_failures(&epoch);
        write_json(Path::new(BOOT_GUARD), &guard)?;
        info!("Built-in Zygisk injection confirmed by a live switch; BootGuard failures cleared");
    }
    Ok(())
}

/// Boot receipt: a boot that confirmed injection clears the latch; a boot
/// that never did keeps its counted attempt, with the failure detail updated.
/// Never fails the boot: only a successful receipt that cannot be persisted
/// is an error, matching FinaSU.
pub fn on_boot_completed() -> Result<()> {
    if !crate::builtin_zygisk::is_enabled() {
        return Ok(());
    }
    let _lock = lock_file(Path::new(GUARD_LOCK))?;
    let epoch = current_epoch()?;
    let mut guard = load_guard_or_default();
    guard.prepare_cycle(&epoch);
    let injected = crate::builtin_zygisk::is_fully_active();
    if injected {
        if guard.failures > 0 || guard.latched || guard.last_issue.is_some() {
            guard.clear_failures(&epoch);
            write_json(Path::new(BOOT_GUARD), &guard)
                .context("Clear built-in Zygisk startup protection")?;
            info!("Built-in Zygisk startup confirmed; BootGuard failures cleared");
        }
        return Ok(());
    }
    let pending_attempt = guard.last_issue.as_ref().is_some_and(|issue| {
        issue.generation == epoch.generation
            && issue.boot_id == epoch.boot_id
            && issue.counted
            && issue.code == StartupIssueCode::InjectionUnconfirmed
    });
    if !pending_attempt {
        return Ok(());
    }
    let detail = format!(
        "complete zygote injection was not confirmed (monitor={}, daemon={}, zygote={})",
        crate::builtin_zygisk::is_monitor_active(),
        crate::builtin_zygisk::is_daemon_active(),
        crate::builtin_zygisk::is_zygote_injected(),
    );
    guard.record_issue(&epoch, StartupIssueCode::InjectionUnconfirmed, detail, true);
    if let Err(error) = write_json(Path::new(BOOT_GUARD), &guard) {
        warn!("Could not persist built-in Zygisk boot-completion diagnostic: {error:#}");
    }
    Ok(())
}

/// Clear the latch without enabling or starting injection. Never permits
/// another attempt in this cycle: the next try waits for the next boot.
pub fn reset_failures() -> Result<()> {
    let _lock = lock_file(Path::new(GUARD_LOCK))?;
    let epoch = current_epoch()?;
    let mut guard = load_guard_or_default();
    guard.failures = 0;
    guard.latched = false;
    guard.last_issue = None;
    guard.mark_handled(&epoch);
    write_json(Path::new(BOOT_GUARD), &guard)?;
    Ok(())
}

/// Restore automatic startup for the next boot without starting now: reserve
/// the current cycle first, then enable, then clear the latch. A failure
/// between the steps leaves the latch in place, but the reserved cycle still
/// cannot attempt a start.
pub fn recover() -> Result<()> {
    let _lock = lock_file(Path::new(GUARD_LOCK))?;
    let epoch = current_epoch()?;
    let mut guard = load_guard_or_default();
    guard.prepare_cycle(&epoch);
    guard.mark_handled(&epoch);
    write_json(Path::new(BOOT_GUARD), &guard)
        .context("Built-in Zygisk recovery could not reserve the current userspace boot")?;
    crate::builtin_zygisk::enable().context(
        "Built-in Zygisk could not be enabled; the current userspace boot remains protected",
    )?;
    guard.clear_failures(&epoch);
    write_json(Path::new(BOOT_GUARD), &guard).context(
        "Built-in Zygisk was enabled and the current boot remains protected, but the startup latch could not be cleared",
    )?;
    Ok(())
}
