//! LKM-only SuSFS proc extensions: hidden module names and hidden mount
//! prefixes.
//!
//! The LKM build of SuSFS exposes `/proc/susfs_hide_modules` and
//! `/proc/susfs_hide_mounts`; each accepts one `add <value>` line per write.
//! These commands mirror FinaSU's `proc_write` helper: they refuse to run
//! when the proc node is absent instead of failing a boot script.

use std::fs::OpenOptions;
use std::io::Write;
use std::path::Path;

/// Write one `add <value>` line to an LKM proc node.
///
/// # Errors
///
/// Returns an error when the proc node does not exist (no LKM SuSFS) or
/// when the write is short.
fn proc_write(node: &str, value: &str) -> anyhow::Result<()> {
    let path = format!("/proc/{node}");
    if !Path::new(&path).exists() {
        anyhow::bail!("LKM proc extension unavailable: {node}");
    }
    let command = format!("add {value}\n");
    let mut file = OpenOptions::new().write(true).open(&path)?;
    if file.write(command.as_bytes())? != command.len() {
        anyhow::bail!("Short SUSFS proc write");
    }
    Ok(())
}

/// Whether `value` is a valid SUSFS path: absolute, below the 256-byte
/// limit, with no empty segments, whitespace, or control characters.
/// Mirrors FinaSU's `SusfsInputs::validPath`.
fn valid_path(value: &str) -> bool {
    value.starts_with('/')
        && value != "/"
        && value.len() < 256
        && !value.contains("//")
        && !value.chars().any(|c| c.is_whitespace() || c.is_control())
        && value.split('/').all(|part| part != "." && part != "..")
}

/// Hide a kernel module name from module listings (LKM SuSFS only).
///
/// # Errors
///
/// Returns an error when the module name is invalid or the
/// `/proc/susfs_hide_modules` node is absent.
pub fn add_hidden_module(name: &str) -> anyhow::Result<()> {
    if name.is_empty()
        || name.len() > 63
        || !name.bytes().all(|c| c.is_ascii_alphanumeric() || c == b'_')
    {
        anyhow::bail!("Invalid module name");
    }
    proc_write("susfs_hide_modules", name)
}

/// Hide a mount prefix from mount listings (LKM SuSFS only).
///
/// # Errors
///
/// Returns an error when the prefix is not a valid path or the
/// `/proc/susfs_hide_mounts` node is absent.
pub fn add_mount_prefix(path: &str) -> anyhow::Result<()> {
    if !valid_path(path) {
        anyhow::bail!("Invalid path: {path}");
    }
    proc_write("susfs_hide_mounts", path)
}
