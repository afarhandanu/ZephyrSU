package com.sukisu.ultra.ui.component.themeimport

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.sukisu.ultra.R
import com.sukisu.ultra.data.security.SecurityTarget
import com.sukisu.ultra.ui.component.security.LocalAppAuthenticator
import com.sukisu.ultra.wallpaper.PreparedTheme
import com.sukisu.ultra.wallpaper.ThemeImportService
import com.sukisu.ultra.wallpaper.ThemeMetadata
import com.sukisu.ultra.wallpaper.ThemePart
import com.sukisu.ultra.wallpaper.ThemeSelection

/**
 * Shared picker/store entry point for `.fpt` themes; prepared resources belong to this
 * UI lifetime.
 *
 * Adapted from FinaSU: the live Preview button is dropped — the full-app preview activity
 * needs the wallpaper system and theme-preview snapshot infra (M5.2). The dialog confirms
 * metadata, the parts to apply and the validation warnings, then applies.
 */
@Composable
fun rememberFptThemeImportRequest(onResult: (Result<Unit>) -> Unit): (Uri) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authenticator = LocalAppAuthenticator.current
    var pending by remember { mutableStateOf<PreparedTheme?>(null) }
    var busy by remember { mutableStateOf(false) }
    var disposed by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            disposed = true
            if (!busy) pending?.close()
        }
    }
    pending?.let { theme ->
        FptThemeImportDialog(
            theme = theme,
            busy = busy,
            onDismiss = { if (!busy) { pending = null; theme.close() } },
            onApply = { selection ->
                if (!busy) scope.launch {
                    busy = true
                    try {
                        if (!authenticator.require(SecurityTarget.THEME_IMPORT)) return@launch
                        val result = ThemeImportService.apply(context.applicationContext, theme, selection)
                        onResult(result)
                        if (result.isSuccess && Build.VERSION.SDK_INT < 33) (context as? Activity)?.recreate()
                    } finally {
                        pending = null
                        theme.close()
                        busy = false
                    }
                }
            },
        )
    }
    return { uri ->
        if (!busy) scope.launch {
            busy = true
            try {
                if (!authenticator.require(SecurityTarget.THEME_IMPORT)) return@launch
                ThemeImportService.prepare(context.applicationContext, uri).onSuccess {
                    pending?.close()
                    pending = it
                }.onFailure { onResult(Result.failure(it)) }
            } finally {
                busy = false
                if (disposed) {
                    pending?.close()
                    pending = null
                }
            }
        }
    }
}

@Composable
private fun FptThemeImportDialog(
    theme: PreparedTheme,
    busy: Boolean,
    onDismiss: () -> Unit,
    onApply: (ThemeSelection) -> Unit,
) {
    var selected by remember(theme) { mutableStateOf(theme.available) }
    val allSelected = selected.size == theme.available.size
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fpt_import_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                theme.previewFile?.let {
                    AsyncImage(model = it, contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(20.dp)))
                }
                ThemeMetadataSummary(theme.metadata)
                Text(stringResource(R.string.fpt_resources, theme.resourceCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (theme.warnings.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        theme.warnings.forEach { warning ->
                            Text(warning, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (theme.available.isNotEmpty()) {
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.fpt_parts),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = {
                                selected = if (allSelected) emptySet() else theme.available
                            }) {
                                Text(stringResource(if (allSelected) R.string.fpt_select_none else R.string.fpt_select_all))
                            }
                        }
                        ThemePartOrder.filter { it in theme.available }.forEach { part ->
                            ThemePartRow(part, part in selected) { checked ->
                                selected = if (checked) selected + part else selected - part
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(ThemeSelection(selected)) },
                enabled = !busy && selected.isNotEmpty()) {
                Text(stringResource(R.string.theme_store_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(stringResource(R.string.theme_store_cancel))
            }
        },
    )
}

/** Only the parts M5.1 can apply; page backgrounds, surfaces and home layout are M5.2/M5.3. */
private val ThemePartOrder = listOf(
    ThemePart.COLORS, ThemePart.INTERFACE, ThemePart.MAIN_BACKGROUND,
    ThemePart.FONT, ThemePart.NAV_ICONS, ThemePart.MUSIC,
    ThemePart.SOUND, ThemePart.LANGUAGE,
)

@Composable
private fun ThemePartRow(part: ThemePart, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = checked, role = Role.Checkbox) { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(part.labelRes), style = MaterialTheme.typography.bodyLarge)
            part.noteRes?.let {
                Text(stringResource(it), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Checkbox(checked = checked, onCheckedChange = null)
    }
}

private val ThemePart.labelRes: Int
    get() = when (this) {
        ThemePart.MAIN_BACKGROUND -> R.string.fpt_part_main_background
        ThemePart.PAGE_BACKGROUNDS -> R.string.fpt_part_page_backgrounds
        ThemePart.SURFACES -> R.string.fpt_part_surfaces
        ThemePart.FONT -> R.string.fpt_part_font
        ThemePart.HOME_LAYOUT -> R.string.fpt_part_home_layout
        ThemePart.NAV_ICONS -> R.string.fpt_part_nav_icons
        ThemePart.MUSIC -> R.string.fpt_part_music
        ThemePart.SOUND -> R.string.fpt_part_sound
        ThemePart.COLORS -> R.string.fpt_part_colors
        ThemePart.INTERFACE -> R.string.fpt_part_interface
        ThemePart.LANGUAGE -> R.string.fpt_part_language
    }

private val ThemePart.noteRes: Int?
    get() = when (this) {
        ThemePart.FONT, ThemePart.HOME_LAYOUT -> R.string.fpt_part_replace_note
        ThemePart.LANGUAGE -> R.string.fpt_part_language_note
        else -> null
    }

@Composable
fun ThemeMetadataSummary(metadata: ThemeMetadata, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (metadata.name.isNotBlank()) {
            Text(metadata.name, style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val typeLabel = when (metadata.type) {
                "phone" -> stringResource(R.string.theme_type_phone)
                "tablet" -> stringResource(R.string.theme_type_tablet)
                else -> metadata.type
            }
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (metadata.type == "phone" || metadata.type == "tablet") {
                        Icon(if (metadata.type == "tablet") Icons.Default.TabletAndroid else Icons.Default.PhoneAndroid,
                            contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Text(typeLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (metadata.version.isNotBlank()) {
                Text(stringResource(R.string.theme_store_version, metadata.version),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (metadata.author.isNotBlank()) {
            ThemeMetadataRow(stringResource(R.string.fpt_author), metadata.author)
        }
        if (metadata.description.isNotBlank()) {
            Text(metadata.description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ThemeMetadataRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ThemeMetadataEditor(initial: ThemeMetadata, onDismiss: () -> Unit, onExport: (ThemeMetadata) -> Unit) {
    var metadata by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.wallpaper_export)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(metadata.name, { metadata = metadata.copy(name = it) },
                    label = { Text(stringResource(R.string.fpt_name)) }, singleLine = true)
                ThemeDeviceTypeSelector(metadata.type) { metadata = metadata.copy(type = it) }
                OutlinedTextField(metadata.version, { metadata = metadata.copy(version = it) },
                    label = { Text(stringResource(R.string.fpt_version)) }, singleLine = true)
                OutlinedTextField(metadata.author, { metadata = metadata.copy(author = it) },
                    label = { Text(stringResource(R.string.fpt_author)) }, singleLine = true)
                OutlinedTextField(metadata.description, { metadata = metadata.copy(description = it) },
                    label = { Text(stringResource(R.string.fpt_description)) })
            }
        },
        confirmButton = { TextButton(onClick = { onExport(metadata) }, enabled = metadata.name.isNotBlank()) {
            Text(stringResource(R.string.wallpaper_export))
        } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.theme_store_cancel)) } },
    )
}

@Composable
private fun ThemeDeviceTypeSelector(type: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().selectableGroup()) {
        listOf(
            Triple("phone", Icons.Default.PhoneAndroid, R.string.theme_type_phone),
            Triple("tablet", Icons.Default.TabletAndroid, R.string.theme_type_tablet),
        ).forEachIndexed { index, (value, icon, label) ->
            val isSelected = type == value
            Surface(
                onClick = { onSelect(value) },
                modifier = Modifier.weight(1f).height(56.dp).semantics {
                    role = Role.RadioButton
                    selected = isSelected
                },
                shape = if (index == 0) RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    else RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
            ) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
                    if (isSelected) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
