//! Per-plugin configuration values.
//!
//! Values live in `config.json` next to the plugin package and are stored as
//! JSON strings. Reading tolerates values written by other managers by
//! rendering non-string scalars as text, and a damaged file is reported to the
//! caller instead of being silently replaced.

use std::collections::BTreeMap;
use std::fs::{self, File};
use std::io::Write;
use std::os::unix::fs::PermissionsExt;
use std::path::{Path, PathBuf};

use anyhow::{Context, Result, bail};
use serde_json::{Map, Value};

use super::{lock, manifest};
use crate::defs;

fn config_path(plugin_dir: &Path, id: &str) -> PathBuf {
    plugin_dir.join(id).join(defs::PLUGIN_CONFIG_FILE)
}

fn lock_path(lock_dir: &Path, id: &str) -> PathBuf {
    lock_dir.join(format!("{id}.config.lock"))
}

/// Read every value of `id`, rendering legacy non-string values as text.
///
/// A missing file yields an empty set; a file that is not a JSON object is an
/// error and is left untouched.
///
/// # Errors
///
/// Returns an error for an invalid id, an oversized or unreadable file, or
/// content that is not a JSON object.
pub fn load(plugin_dir: &Path, id: &str) -> Result<BTreeMap<String, String>> {
    manifest::validate_plugin_id(id)?;
    let path = config_path(plugin_dir, id);
    let metadata = match fs::metadata(&path) {
        Ok(metadata) => metadata,
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => return Ok(BTreeMap::new()),
        Err(error) => {
            return Err(error).with_context(|| format!("cannot read {}", path.display()));
        }
    };
    if !metadata.is_file() {
        bail!("{} is not a regular file", path.display());
    }
    if metadata.len() > defs::PLUGIN_MAX_CONFIG_SIZE {
        bail!(
            "{} exceeds {} bytes",
            path.display(),
            defs::PLUGIN_MAX_CONFIG_SIZE
        );
    }
    let raw = fs::read(&path).with_context(|| format!("cannot read {}", path.display()))?;
    if raw.is_empty() {
        return Ok(BTreeMap::new());
    }
    let value: Value = serde_json::from_slice(&raw)
        .with_context(|| format!("{} is not valid JSON", path.display()))?;
    let object = value
        .as_object()
        .ok_or_else(|| anyhow::anyhow!("{} must hold a JSON object", path.display()))?;
    Ok(object
        .iter()
        .map(|(key, value)| {
            let text = match value {
                Value::String(text) => text.clone(),
                other => other.to_string(),
            };
            (key.clone(), text)
        })
        .collect())
}

/// Return one value, or an empty string when the key is unset.
///
/// # Errors
///
/// Returns an error for an invalid id or key, or when the file cannot be read.
pub fn get(plugin_dir: &Path, id: &str, key: &str) -> Result<String> {
    manifest::validate_config_key(key)?;
    Ok(load(plugin_dir, id)?.get(key).cloned().unwrap_or_default())
}

/// Store `value` for `key`, replacing any previous value.
///
/// # Errors
///
/// Returns an error when the plugin is not installed, the lock is held, or the
/// file cannot be written.
pub fn set(plugin_dir: &Path, lock_dir: &Path, id: &str, key: &str, value: &str) -> Result<()> {
    manifest::validate_config_key(key)?;
    let _guard = lock::exclusive(&lock_path(lock_dir, id))?;
    let mut values = load(plugin_dir, id)?;
    values.insert(key.to_owned(), value.to_owned());
    write(plugin_dir, id, &values)
}

/// Remove `key`, leaving the file alone when the key is not present.
///
/// # Errors
///
/// Returns an error when the plugin is not installed, the lock is held, or the
/// file cannot be written.
pub fn delete(plugin_dir: &Path, lock_dir: &Path, id: &str, key: &str) -> Result<()> {
    manifest::validate_config_key(key)?;
    let _guard = lock::exclusive(&lock_path(lock_dir, id))?;
    let mut values = load(plugin_dir, id)?;
    if values.remove(key).is_none() {
        return Ok(());
    }
    write(plugin_dir, id, &values)
}

fn write(plugin_dir: &Path, id: &str, values: &BTreeMap<String, String>) -> Result<()> {
    let dir = plugin_dir.join(id);
    if !dir.is_dir() {
        bail!("plugin '{id}' is not installed");
    }
    let mut object = Map::new();
    for (key, value) in values {
        object.insert(key.clone(), Value::String(value.clone()));
    }
    let bytes = serde_json::to_vec_pretty(&Value::Object(object))?;
    if bytes.len() as u64 > defs::PLUGIN_MAX_CONFIG_SIZE {
        bail!(
            "configuration for '{id}' exceeds {} bytes",
            defs::PLUGIN_MAX_CONFIG_SIZE
        );
    }
    let path = dir.join(defs::PLUGIN_CONFIG_FILE);
    let mut temp = tempfile::NamedTempFile::new_in(&dir)
        .with_context(|| format!("cannot create a file in {}", dir.display()))?;
    temp.as_file()
        .set_permissions(fs::Permissions::from_mode(0o600))?;
    temp.write_all(&bytes)?;
    temp.as_file().sync_all()?;
    temp.persist(&path)
        .with_context(|| format!("cannot write {}", path.display()))?;
    File::open(&dir)?.sync_all()?;
    Ok(())
}
