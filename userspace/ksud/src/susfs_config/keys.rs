//! Public config-key constants.
//!
//! Names mirror the Kotlin `KEY_*` constants (minus the prefix) used by
//! the on-device WebUI; renaming one here without updating the WebUI
//! will silently drop that setting on every read.
//!
//! Used internally by [`super::store`] (for `reset_to_defaults`) and
//! [`super::module`] (for `load_module_config`); not re-exported at the
//! `susfs_config` root because the CLI never needs to spell a key.

pub const KEY_UNAME_VALUE: &str = "uname_value";
pub const KEY_BUILD_TIME_VALUE: &str = "build_time_value";
pub const KEY_AUTO_START_ENABLED: &str = "auto_start_enabled";
pub const KEY_SUS_PATHS: &str = "sus_paths";
pub const KEY_SUS_LOOP_PATHS: &str = "sus_loop_paths";
pub const KEY_PRIORITY_UID_FILTER: &str = "priority_uid_filter";
pub const KEY_SUS_PATH_UID_FILTER: &str = "sus_path_uid_filter";
pub const KEY_SUS_MAPS: &str = "sus_maps";
pub const KEY_ENABLE_LOG: &str = "enable_log";
pub const KEY_EXECUTE_IN_POST_FS_DATA: &str = "execute_in_post_fs_data";
pub const KEY_KSTAT_CONFIGS: &str = "kstat_configs";
pub const KEY_ADD_KSTAT_PATHS: &str = "add_kstat_paths";
pub const KEY_HIDE_SUS_MOUNTS_FOR_ALL_PROCS: &str = "hide_sus_mounts_for_all_procs";
pub const KEY_ENABLE_CLEANUP_RESIDUE: &str = "enable_cleanup_residue";
pub const KEY_ENABLE_HIDE_BL: &str = "enable_hide_bl";
pub const KEY_ENABLE_AVC_LOG_SPOOFING: &str = "enable_avc_log_spoofing";
pub const KEY_CMDLINE_OR_BOOTCONFIG_PATH: &str = "cmdline_or_bootconfig_path";
/// Open-redirect rules, `;;`-joined `target|destination|uid_scheme` records
/// (same record style as `KEY_KSTAT_CONFIGS`). Ported from FinaSU's SUSFS
/// redirect rules; Nova previously had only the one-shot
/// `susfs add-open-redirect` command with no persistence.
pub const KEY_REDIRECT_RULES: &str = "redirect_rules";
/// LKM hidden module names (`/proc/susfs_hide_modules`), `;`-joined.
/// Ported from FinaSU's `lkm.hidden_modules`; Nova only detected the proc
/// node before, with no way to set entries.
pub const KEY_HIDDEN_MODULES: &str = "hidden_modules";
/// LKM hidden mount prefixes (`/proc/susfs_hide_mounts`), `;`-joined.
/// Ported from FinaSU's `lkm.mount_prefixes`.
pub const KEY_MOUNT_PREFIXES: &str = "mount_prefixes";

/// All known config keys, in load order.
///
/// `#[allow(dead_code)]` because this list is meant for tooling
/// (enumeration / diff against the on-disk file) rather than for
/// driving control flow in `ksud` itself.
#[allow(dead_code)]
pub const ALL_KEYS: &[&str] = &[
    KEY_UNAME_VALUE,
    KEY_BUILD_TIME_VALUE,
    KEY_AUTO_START_ENABLED,
    KEY_SUS_PATHS,
    KEY_SUS_LOOP_PATHS,
    KEY_SUS_MAPS,
    KEY_PRIORITY_UID_FILTER,
    KEY_SUS_PATH_UID_FILTER,
    KEY_ENABLE_LOG,
    KEY_EXECUTE_IN_POST_FS_DATA,
    KEY_KSTAT_CONFIGS,
    KEY_ADD_KSTAT_PATHS,
    KEY_HIDE_SUS_MOUNTS_FOR_ALL_PROCS,
    KEY_ENABLE_CLEANUP_RESIDUE,
    KEY_ENABLE_HIDE_BL,
    KEY_ENABLE_AVC_LOG_SPOOFING,
    KEY_CMDLINE_OR_BOOTCONFIG_PATH,
    KEY_REDIRECT_RULES,
    KEY_HIDDEN_MODULES,
    KEY_MOUNT_PREFIXES,
];
