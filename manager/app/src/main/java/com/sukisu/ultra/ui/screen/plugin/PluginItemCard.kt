package com.sukisu.ultra.ui.screen.plugin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.data.model.Plugin
import com.sukisu.ultra.data.model.pluginLocalizedOrNull
import com.sukisu.ultra.data.modulebanner.CustomModuleInfo
import com.sukisu.ultra.data.modulebanner.ModuleCardConfig
import com.sukisu.ultra.ui.component.material.ExpressiveSwitch
import com.sukisu.ultra.ui.component.material.TonalCard
import com.sukisu.ultra.ui.component.module.AdaptiveModuleButtonRow
import com.sukisu.ultra.ui.component.module.ModuleButtonConfig
import com.sukisu.ultra.ui.component.module.ModuleButtonStyle
import com.sukisu.ultra.ui.screen.module.ModuleCardVisual
import com.sukisu.ultra.ui.screen.module.bannerAlpha
import com.sukisu.ultra.ui.screen.module.moduleBanner
import com.sukisu.ultra.ui.theme.LocalModuleDescriptionMaxLines
import com.sukisu.ultra.wallpaper.LocalFolkWallpaperTokens
import com.sukisu.ultra.wallpaper.WallpaperConfig

/**
 * One plugin, drawn with the module card design: same frame, same banner, same folding and the
 * same adaptive action row.
 */
@Composable
fun PluginItemCard(
    plugin: Plugin,
    visual: ModuleCardVisual,
    pending: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    actions: PluginActions,
    onLongPress: () -> Unit,
    closeSearch: () -> Unit,
) {
    val foldActions = ModuleCardConfig.foldSystemModule

    // ZephyrSU's module-card frame (TonalCard + banner modifier), not FinaSU's
    // FolkCardFrame: the frame pulls in FinaSU's wallpaper-token and nav-origin
    // machinery, which has no ZephyrSU counterpart.
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (foldActions) onExpandToggle() },
        onLongClick = onLongPress,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .moduleBanner(visual.banner, MaterialTheme.colorScheme.surfaceBright, alpha = bannerAlpha()),
        ) {
            PluginCardContent(
                plugin = plugin,
                customInfo = visual.customInfo,
                pending = pending,
                expanded = expanded,
                foldActions = foldActions,
                actions = actions,
                closeSearch = closeSearch,
            )
        }
    }
}

@Composable
private fun PluginCardContent(
    plugin: Plugin,
    customInfo: CustomModuleInfo?,
    pending: Boolean,
    expanded: Boolean,
    foldActions: Boolean,
    actions: PluginActions,
    closeSearch: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val showMoreInfo = ModuleCardConfig.showMoreInfo
    val iconOnly = ModuleButtonStyle.fromKey(ModuleCardConfig.buttonStyle) == ModuleButtonStyle.IconOnly
    val actionable = plugin.error == null && !pending
    val maxLinesLimit = LocalModuleDescriptionMaxLines.current

    val displayName = customInfo?.name?.takeIf { it.isNotBlank() } ?: plugin.name.ifBlank { plugin.id }
    val displayVersion = customInfo?.version?.takeIf { it.isNotBlank() } ?: plugin.version
    val displayAuthor = customInfo?.author?.takeIf { it.isNotBlank() } ?: plugin.author
    val displayDescription = customInfo?.description?.takeIf { it.isNotBlank() }
        ?: plugin.descriptions.pluginLocalizedOrNull()
        ?: plugin.description

    val quickLabel = plugin.quickAction?.labels?.pluginLocalizedOrNull() ?: plugin.quickAction?.label?.takeIf { it.isNotBlank() }
    val showRun = actionable && (plugin.quickAction != null || plugin.callbacks.contains("action"))
    val showConfig = actionable && plugin.config.isNotEmpty()

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                if (showMoreInfo) {
                    PluginLabel(text = plugin.id)
                }
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                if (showMoreInfo) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${stringResource(R.string.module_version)}: $displayVersion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${stringResource(R.string.module_author)}: $displayAuthor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ExpressiveSwitch(
                enabled = actionable,
                checked = plugin.enabled,
                onCheckedChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    actions.onToggleEnabled(plugin, it)
                },
            )
        }

        if (showMoreInfo && displayDescription.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = displayDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = maxLinesLimit,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (showMoreInfo && plugin.depends.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${stringResource(R.string.plugin_dependencies)}: ${plugin.depends.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        plugin.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(
            visible = !foldActions || expanded,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        ) {
            Column {
                AdaptiveModuleButtonRow(
                    buttons = buildList {
                        if (showConfig) {
                            add(
                                ModuleButtonConfig(
                                    icon = Icons.Outlined.Tune,
                                    text = stringResource(R.string.plugin_config),
                                    contentDescription = stringResource(R.string.plugin_config),
                                    onClick = {
                                        actions.onOpenConfig(plugin)
                                        closeSearch()
                                    },
                                ),
                            )
                        }
                        if (showRun) {
                            add(
                                ModuleButtonConfig(
                                    icon = Icons.Outlined.PlayArrow,
                                    text = quickLabel ?: stringResource(R.string.plugin_run),
                                    contentDescription = quickLabel ?: stringResource(R.string.plugin_run),
                                    onClick = {
                                        actions.onRun(plugin)
                                        closeSearch()
                                    },
                                ),
                            )
                        }
                        add(
                            ModuleButtonConfig(
                                icon = Icons.Outlined.Description,
                                text = stringResource(R.string.plugin_log),
                                contentDescription = stringResource(R.string.plugin_log),
                                onClick = {
                                    actions.onOpenLog(plugin)
                                    closeSearch()
                                },
                            ),
                        )
                    },
                    trailingButton = ModuleButtonConfig(
                        icon = Icons.Outlined.Delete,
                        text = stringResource(R.string.uninstall),
                        contentDescription = stringResource(R.string.uninstall),
                        enabled = actionable,
                        onClick = {
                            actions.onRequestUninstall(plugin)
                            closeSearch()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ),
                    iconOnly = iconOnly,
                    spacing = if (iconOnly) 12 else 8,
                    opacity = cardSurfaceOpacity(),
                )
            }
        }
    }
}

@Composable
private fun PluginLabel(text: String) {
    com.sukisu.ultra.ui.component.module.ModuleLabel(
        text = text,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    Spacer(Modifier.height(8.dp))
}

/**
 * Opacity of the card surface over a wallpaper, mirroring FinaSU's card frame.
 * Opaque when no wallpaper is active.
 */
@Composable
private fun cardSurfaceOpacity(): Float {
    if (LocalFolkWallpaperTokens.current == null) return 1f
    return WallpaperConfig.opacity.coerceAtLeast(0.35f)
}
