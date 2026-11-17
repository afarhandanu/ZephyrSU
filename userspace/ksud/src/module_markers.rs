//! Marker handling shared by the module lifecycle commands.
//!
//! This lives outside `module.rs` on purpose: that module only compiles for Android, while the
//! marker dance is plain filesystem work that deserves host unit tests.

use std::path::Path;

use anyhow::{Context, Result};

/// Clear one marker file on both boot-promotion copies, rolling back on rc failure.
///
/// Boot promotion copies the pending module over the installed one, so a pending copy inherits
/// the installed module's markers. Clearing only the installed copy would let a bare
/// `enable`/`undo uninstall` silently revert on the next boot.
pub fn clear_activation_markers(
    installed: &Path,
    pending: &Path,
    marker: &str,
    mut refresh: impl FnMut() -> Result<()>,
) -> Result<()> {
    let mut previous = Vec::new();
    for directory in [installed, pending] {
        let path = directory.join(marker);
        match std::fs::read(&path) {
            Ok(bytes) => previous.push((path, bytes)),
            Err(error) if error.kind() == std::io::ErrorKind::NotFound => {}
            Err(error) => return Err(error.into()),
        }
    }

    let result = (|| {
        for (path, _) in &previous {
            std::fs::remove_file(path)?;
        }
        refresh()
    })();
    if let Err(error) = result {
        for (path, bytes) in previous {
            std::fs::write(path, bytes).context("Restore activation marker")?;
        }
        refresh().context("Restore activation rc")?;
        return Err(error);
    }
    Ok(())
}
