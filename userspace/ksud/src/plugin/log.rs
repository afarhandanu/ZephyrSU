//! Per-plugin output log.
//!
//! A plugin writes its captured output to `last_output.log` next to the
//! package. Reading never returns more than the newest bytes, so an oversized
//! file left behind by another manager cannot flood the caller.

use std::fs::{self, File};
use std::io::{ErrorKind, Read, Seek, SeekFrom};
use std::path::{Path, PathBuf};

use anyhow::{Context, Result, bail};

use super::{lock, manifest};
use crate::defs;

fn log_path(plugin_dir: &Path, id: &str) -> PathBuf {
    plugin_dir.join(id).join(defs::PLUGIN_LOG_FILE)
}

fn lock_path(lock_dir: &Path, id: &str) -> PathBuf {
    lock_dir.join(format!("{id}.log.lock"))
}

/// Read the newest bytes of `id`'s log.
///
/// A missing log yields an empty string. Only the newest
/// [`defs::PLUGIN_MAX_LOG_SIZE`] bytes are returned, so a large file cannot
/// flood the caller.
///
/// # Errors
///
/// Returns an error when the log exists but cannot be read or is not a regular
/// file.
pub fn read(plugin_dir: &Path, id: &str) -> Result<String> {
    manifest::validate_plugin_id(id)?;
    let path = log_path(plugin_dir, id);
    let metadata = match fs::metadata(&path) {
        Ok(metadata) => metadata,
        Err(error) if error.kind() == ErrorKind::NotFound => return Ok(String::new()),
        Err(error) => {
            return Err(error).with_context(|| format!("cannot read {}", path.display()));
        }
    };
    if !metadata.is_file() {
        bail!("{} is not a regular file", path.display());
    }
    let mut file = File::open(&path).with_context(|| format!("cannot open {}", path.display()))?;
    let len = metadata.len();
    let cap = defs::PLUGIN_MAX_LOG_SIZE;
    if len > cap {
        file.seek(SeekFrom::Start(len - cap))
            .with_context(|| format!("cannot seek {}", path.display()))?;
    }
    let mut bytes = Vec::new();
    file.take(cap)
        .read_to_end(&mut bytes)
        .with_context(|| format!("cannot read {}", path.display()))?;
    Ok(String::from_utf8_lossy(&bytes).into_owned())
}

/// Remove `id`'s log. A missing log is not an error.
///
/// # Errors
///
/// Returns an error when the log exists but cannot be removed.
pub fn clear(plugin_dir: &Path, lock_dir: &Path, id: &str) -> Result<()> {
    manifest::validate_plugin_id(id)?;
    let _guard = lock::exclusive(&lock_path(lock_dir, id))?;
    let path = log_path(plugin_dir, id);
    match fs::remove_file(&path) {
        Ok(()) => Ok(()),
        Err(error) if error.kind() == ErrorKind::NotFound => Ok(()),
        Err(error) => Err(error).with_context(|| format!("cannot remove {}", path.display())),
    }
}
