package com.sukisu.ultra.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sukisu.ultra.R

/**
 * Human-readable label for a shared kernel feature id
 * (M6.7 external feature-change detection). Shared by the Material and Miuix
 * settings screens.
 */
@Composable
internal fun featureChangeLabel(name: String): String {
    val labelRes = when (name) {
        "su_compat" -> R.string.settings_sucompat
        "kernel_umount" -> R.string.settings_kernel_umount
        "sulog" -> R.string.settings_sulog
        "adb_root" -> R.string.settings_adb_root
        "selinux_hide" -> R.string.settings_selinux_hide
        "avc_spoof" -> R.string.settings_avc_spoof
        "shell_su_adb_only" -> R.string.settings_shell_su_adb_only
        "mount_hide" -> R.string.settings_mount_hide
        else -> null
    }
    return if (labelRes != null) stringResource(labelRes) else name
}

@Composable
internal fun featureChangeValue(value: Long): String {
    return stringResource(
        if (value != 0L) R.string.feature_external_value_on else R.string.feature_external_value_off,
    )
}
