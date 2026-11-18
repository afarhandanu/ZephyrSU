//! Plugin discovery and listing.
//!
//! Listing only reads the package metadata. It never loads or runs plugin
//! code, so a broken or hostile entry cannot affect the listing itself.

use std::collections::{BTreeMap, BTreeSet};
use std::fs;
use std::path::Path;

use anyhow::{Context, Result};
use serde::Serialize;

use super::manifest::{self, ConfigField, QuickAction};
use crate::defs;

/// One discovered plugin. Invalid packages are reported instead of skipped so
/// the user can see why a plugin does not work.
#[derive(Debug, Clone, Serialize)]
pub struct PluginInfo {
    pub id: String,
    pub name: String,
    pub author: String,
    pub version: String,
    pub description: String,
    pub descriptions: BTreeMap<String, String>,
    pub license: String,
    pub enabled: bool,
    pub has_manifest: bool,
    pub has_action: bool,
    pub entry: String,
    pub quick_action: Option<QuickAction>,
    pub config: Vec<ConfigField>,
    pub depends: Vec<String>,
    pub callbacks: Vec<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

/// Lists the plugins installed under `dir`, sorted by id.
///
/// A missing directory is not an error: it simply means no plugin is
/// installed yet.
///
/// # Errors
/// Returns an error when `dir` exists but its contents cannot be read.
pub fn discover(dir: &Path) -> Result<Vec<PluginInfo>> {
    let entries = match fs::read_dir(dir) {
        Ok(entries) => entries,
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => return Ok(Vec::new()),
        Err(error) => return Err(error).with_context(|| format!("cannot read {}", dir.display())),
    };

    let mut plugins = Vec::new();
    for entry in entries {
        let entry = entry.with_context(|| format!("cannot read {}", dir.display()))?;
        let Some(id) = entry.file_name().to_str().map(str::to_owned) else {
            continue;
        };
        let path = entry.path();
        if !path.is_dir() {
            continue;
        }
        plugins.push(inspect(&path, &id));
    }
    plugins.sort_by(|left, right| left.id.cmp(&right.id));
    Ok(plugins)
}

/// Prints the plugin list as a JSON array on stdout.
///
/// # Errors
/// Returns an error when the plugin directory or the manifest of no plugin can
/// be read; an unreadable manifest is reported per item instead.
pub fn print_list() -> Result<()> {
    let plugins = discover(Path::new(defs::PLUGIN_DIR))?;
    println!("{}", serde_json::to_string(&plugins)?);
    Ok(())
}

/// The order in which the enabled plugins should run.
#[derive(Debug, Default, Serialize)]
pub struct Plan {
    /// Plugin ids that can run, dependencies first.
    pub runnable: Vec<String>,
    /// Plugins that are left out, with the reason.
    pub skipped: Vec<(String, String)>,
}

/// Orders the enabled plugins so that every dependency runs before the plugins
/// that need it. Ties are broken by id, so the order only depends on the set of
/// installed plugins. A plugin that `is_admitted` rejects, or whose dependency
/// is missing, disabled or invalid, is left out with the reason, and so is every
/// plugin that needs it; a dependency cycle leaves all of its members out rather
/// than picking one.
pub fn execution_order(plugins: &[PluginInfo], is_admitted: impl Fn(&str) -> bool) -> Plan {
    let enabled: BTreeMap<&str, &PluginInfo> = plugins
        .iter()
        .filter(|plugin| plugin.enabled && plugin.error.is_none())
        .map(|plugin| (plugin.id.as_str(), plugin))
        .collect();

    let mut skipped: BTreeMap<String, String> = BTreeMap::new();
    loop {
        let mut changed = false;
        for (id, plugin) in &enabled {
            if skipped.contains_key(*id) {
                continue;
            }
            if !is_admitted(id) {
                skipped.insert(
                    (*id).to_owned(),
                    "the plugin has not been admitted".to_owned(),
                );
                changed = true;
                continue;
            }
            for dependency in &plugin.depends {
                let reason = if enabled.contains_key(dependency.as_str()) {
                    skipped.get(dependency).map(|reason| {
                        format!("requires '{dependency}', which is skipped ({reason})")
                    })
                } else {
                    Some(describe_absent(plugins, dependency))
                };
                if let Some(reason) = reason {
                    skipped.insert((*id).to_owned(), reason);
                    changed = true;
                    break;
                }
            }
        }
        if !changed {
            break;
        }
    }

    let mut runnable = Vec::new();
    let mut done: BTreeSet<&str> = BTreeSet::new();
    loop {
        let mut next = None;
        for (id, plugin) in &enabled {
            if done.contains(*id) || skipped.contains_key(*id) {
                continue;
            }
            if plugin
                .depends
                .iter()
                .all(|dependency| done.contains(dependency.as_str()))
            {
                next = Some(*id);
                break;
            }
        }
        match next {
            Some(id) => {
                done.insert(id);
                runnable.push(id.to_owned());
            }
            None => break,
        }
    }

    for id in enabled.keys() {
        if !done.contains(*id) {
            skipped
                .entry((*id).to_owned())
                .or_insert_with(|| "dependency cycle".to_owned());
        }
    }

    Plan {
        runnable,
        skipped: skipped.into_iter().collect(),
    }
}

fn describe_absent(plugins: &[PluginInfo], dependency: &str) -> String {
    match plugins.iter().find(|plugin| plugin.id == dependency) {
        Some(plugin) if plugin.error.is_some() => {
            format!("requires '{dependency}', whose manifest is invalid")
        }
        Some(_) => format!("requires '{dependency}', which is disabled"),
        None => format!("requires '{dependency}', which is not installed"),
    }
}

fn inspect(path: &Path, id: &str) -> PluginInfo {
    let enabled = !path.join(defs::PLUGIN_DISABLE_FILE).exists();
    let has_manifest = path.join(defs::PLUGIN_MANIFEST).exists();
    match manifest::read_manifest(path, id) {
        Ok(manifest) => PluginInfo {
            id: manifest.id,
            name: manifest.name,
            author: manifest.author,
            version: manifest.version,
            description: manifest.description,
            descriptions: manifest.descriptions,
            license: manifest.license,
            enabled,
            has_manifest: true,
            has_action: manifest.has_action,
            entry: manifest.entry,
            quick_action: manifest.quick_action,
            config: manifest.config,
            depends: manifest.depends,
            callbacks: manifest.callbacks,
            error: None,
        },
        Err(error) => PluginInfo {
            id: id.to_owned(),
            name: String::new(),
            author: String::new(),
            version: String::new(),
            description: String::new(),
            descriptions: BTreeMap::new(),
            license: String::new(),
            enabled,
            has_manifest,
            has_action: false,
            entry: String::new(),
            quick_action: None,
            config: Vec::new(),
            depends: Vec::new(),
            callbacks: Vec::new(),
            error: Some(error.to_string()),
        },
    }
}
