package com.sukisu.ultra.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.data.modulebanner.BannerApiClient
import com.sukisu.ultra.data.modulebanner.ModuleBannerConfig
import com.sukisu.ultra.ui.component.material.SegmentedColumn
import com.sukisu.ultra.ui.component.material.SegmentedListItem
import com.sukisu.ultra.ui.component.material.SegmentedSliderItem
import com.sukisu.ultra.ui.component.material.SegmentedSwitchItem
import com.sukisu.ultra.ui.component.material.snapToPercent
import com.sukisu.ultra.ui.component.miuix.AppCard
import com.sukisu.ultra.ui.navigation3.LocalNavigator
import com.sukisu.ultra.ui.navigation3.Route
import com.sukisu.ultra.ui.theme.backgroundAwareCardColors
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import kotlin.math.roundToInt

private val SectionModifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp)

/**
 * Module banner settings (Material 3 Expressive). Ported from FinaSU's
 * `ModuleBannerSettingsSection` (Milestone 3.5); the `ModuleCardSettingsSection` that shares
 * FinaSU's file is NOT ported — card layout config belongs to M3.6, not banners.
 * Adaptations: ZephyrSU's Segmented* components (no `requireStationaryTap` here).
 */
@Composable
fun ModuleBannerSettingsSection() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    var showSourceDialog by remember { mutableStateOf(false) }
    val policy = ModuleBannerConfig.policy
    // API controls follow the same parent gates used by card rendering.
    val apiRowsVisible = policy.apiActive
    val apiConfigured = ModuleBannerConfig.effectiveApiSource.isNotEmpty()
    // Local drag state for the opacity slider (same pattern as the Miuix
    // section, PR #42): ModuleBannerConfig is only written on release, never
    // on every drag tick — see the slider item below.
    var sliderValue by remember(ModuleBannerConfig.customOpacity) {
        mutableFloatStateOf(ModuleBannerConfig.customOpacity)
    }

    SegmentedColumn(
        modifier = SectionModifier,
        title = stringResource(R.string.settings_module_banner),
    ) {
        item {
            SegmentedSwitchItem(
                icon = Icons.Filled.Wallpaper,
                title = stringResource(R.string.module_banner_enabled),
                summary = stringResource(R.string.module_banner_enabled_summary),
                checked = ModuleBannerConfig.enabled,
                onCheckedChange = ModuleBannerConfig::updateEnabled,
            )
        }
        item(visible = ModuleBannerConfig.enabled) {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Cloud,
                title = stringResource(R.string.module_banner_folk),
                summary = stringResource(R.string.module_banner_folk_summary),
                checked = ModuleBannerConfig.folkEnabled,
                onCheckedChange = ModuleBannerConfig::updateFolkEnabled,
            )
        }
        item(visible = policy.active) {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Cloud,
                title = stringResource(R.string.module_banner_api_mode),
                summary = stringResource(R.string.module_banner_api_mode_summary),
                checked = ModuleBannerConfig.apiModeEnabled,
                onCheckedChange = ModuleBannerConfig::updateApiModeEnabled,
            )
        }
        item(visible = apiRowsVisible) {
            SegmentedListItem(
                onClick = { showSourceDialog = true },
                leadingContent = { Icon(Icons.Outlined.Cloud, null) },
                headlineContent = { Text(stringResource(R.string.module_banner_api_source)) },
                supportingContent = {
                    Text(
                        if (apiConfigured) ModuleBannerConfig.effectiveApiSource
                        else stringResource(R.string.module_banner_api_source_not_configured)
                    )
                },
            )
        }
        item(visible = apiRowsVisible) {
            SegmentedListItem(
                onClick = { navigator.push(Route.ApiMarketplace) },
                leadingContent = { Icon(Icons.Outlined.Cloud, null) },
                headlineContent = { Text(stringResource(R.string.module_banner_marketplace)) },
                supportingContent = { Text(stringResource(R.string.module_banner_marketplace_summary)) },
            )
        }
        item(visible = apiRowsVisible) {
            SegmentedListItem(
                onClick = {
                    BannerApiClient.clearAll(context)
                    ModuleBannerConfig.requestReload()
                    Toast.makeText(context, R.string.module_banner_cache_cleared, Toast.LENGTH_SHORT).show()
                },
                leadingContent = { Icon(Icons.Outlined.Cloud, null) },
                headlineContent = { Text(stringResource(R.string.module_banner_clear_cache)) },
            )
        }
        item(visible = ModuleBannerConfig.enabled) {
            SegmentedSwitchItem(
                icon = Icons.Filled.Wallpaper,
                title = stringResource(R.string.module_banner_custom_opacity),
                summary = stringResource(R.string.module_banner_custom_opacity_summary),
                checked = ModuleBannerConfig.customOpacityEnabled,
                onCheckedChange = ModuleBannerConfig::updateCustomOpacityEnabled,
            )
        }
        item(visible = ModuleBannerConfig.enabled && ModuleBannerConfig.customOpacityEnabled) {
            // Commit on release, like the Miuix section (PR #42). The Material
            // Slider drives its drag from its own SliderState, and M3's
            // contract is that onValueChange writes the value back into that
            // state — SegmentedSliderItem only re-syncs it from the hoisted
            // value on recomposition. Writing the snapped value into the
            // state-backed ModuleBannerConfig.customOpacity on every tick made
            // the recomposition-time re-sync fight the in-flight drag (the
            // 0.1..1 range with 19 steps lands on 0.045-spaced ticks that
            // never equal a whole percent, so the re-sync never settled), and
            // the thumb stayed pinned at 100%. During the drag only the local
            // sliderValue moves — the % label still follows live — and the
            // config is written once on release.
            SegmentedSliderItem(
                icon = Icons.Filled.Wallpaper,
                title = stringResource(R.string.module_banner_opacity),
                value = sliderValue,
                valueRange = 0.1f..1f,
                valueText = { "${(it * 100f).roundToInt()}%" },
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    ModuleBannerConfig.editCustomOpacity(snapToPercent(sliderValue))
                    ModuleBannerConfig.persist()
                },
            )
        }
    }

    if (showSourceDialog) {
        BannerApiSourceDialog(onDismiss = { showSourceDialog = false })
    }
}

@Composable
private fun BannerApiSourceDialog(onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(ModuleBannerConfig.apiSource) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.module_banner_api_source_dialog_title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(stringResource(R.string.module_banner_api_source_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    ModuleBannerConfig.updateApiSource(value.trim())
                    onDismiss()
                }
            ) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

/**
 * Module banner settings (Miuix). Same switches and rows as the Material section, rendered
 * with Miuix preferences; the opacity slider follows the ArrowPreference + bottomAction Slider
 * pattern used by the page-scale setting.
 */
@Composable
fun ModuleBannerSettingsSectionMiuix() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    var showSourceDialog by remember { mutableStateOf(false) }
    val policy = ModuleBannerConfig.policy
    val apiRowsVisible = policy.apiActive
    val apiConfigured = ModuleBannerConfig.effectiveApiSource.isNotEmpty()
    var sliderValue by remember(ModuleBannerConfig.customOpacity) {
        mutableFloatStateOf(ModuleBannerConfig.customOpacity)
    }

    AppCard(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth(),
        colors = backgroundAwareCardColors(),
    ) {
        SwitchPreference(
            title = stringResource(R.string.module_banner_enabled),
            summary = stringResource(R.string.module_banner_enabled_summary),
            startAction = {
                MiuixIcon(
                    Icons.Rounded.Wallpaper,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(R.string.module_banner_enabled),
                    tint = colorScheme.onBackground
                )
            },
            checked = ModuleBannerConfig.enabled,
            onCheckedChange = ModuleBannerConfig::updateEnabled,
        )
        if (ModuleBannerConfig.enabled) {
            SwitchPreference(
                title = stringResource(R.string.module_banner_folk),
                summary = stringResource(R.string.module_banner_folk_summary),
                startAction = {
                    MiuixIcon(
                        Icons.Rounded.Cloud,
                        modifier = Modifier.padding(end = 6.dp),
                        contentDescription = stringResource(R.string.module_banner_folk),
                        tint = colorScheme.onBackground
                    )
                },
                checked = ModuleBannerConfig.folkEnabled,
                onCheckedChange = ModuleBannerConfig::updateFolkEnabled,
            )
        }
        if (policy.active) {
            SwitchPreference(
                title = stringResource(R.string.module_banner_api_mode),
                summary = stringResource(R.string.module_banner_api_mode_summary),
                startAction = {
                    MiuixIcon(
                        Icons.Rounded.Cloud,
                        modifier = Modifier.padding(end = 6.dp),
                        contentDescription = stringResource(R.string.module_banner_api_mode),
                        tint = colorScheme.onBackground
                    )
                },
                checked = ModuleBannerConfig.apiModeEnabled,
                onCheckedChange = ModuleBannerConfig::updateApiModeEnabled,
            )
        }
        if (apiRowsVisible) {
            ArrowPreference(
                title = stringResource(R.string.module_banner_api_source),
                summary = if (apiConfigured) ModuleBannerConfig.effectiveApiSource
                else stringResource(R.string.module_banner_api_source_not_configured),
                startAction = {
                    MiuixIcon(
                        Icons.Rounded.Cloud,
                        modifier = Modifier.padding(end = 6.dp),
                        contentDescription = stringResource(R.string.module_banner_api_source),
                        tint = colorScheme.onBackground
                    )
                },
                onClick = { showSourceDialog = true },
            )
            ArrowPreference(
                title = stringResource(R.string.module_banner_marketplace),
                summary = stringResource(R.string.module_banner_marketplace_summary),
                startAction = {
                    MiuixIcon(
                        Icons.Rounded.Cloud,
                        modifier = Modifier.padding(end = 6.dp),
                        contentDescription = stringResource(R.string.module_banner_marketplace),
                        tint = colorScheme.onBackground
                    )
                },
                onClick = { navigator.push(Route.ApiMarketplace) },
            )
            ArrowPreference(
                title = stringResource(R.string.module_banner_clear_cache),
                startAction = {
                    MiuixIcon(
                        Icons.Rounded.Cloud,
                        modifier = Modifier.padding(end = 6.dp),
                        contentDescription = stringResource(R.string.module_banner_clear_cache),
                        tint = colorScheme.onBackground
                    )
                },
                onClick = {
                    BannerApiClient.clearAll(context)
                    ModuleBannerConfig.requestReload()
                    Toast.makeText(context, R.string.module_banner_cache_cleared, Toast.LENGTH_SHORT).show()
                },
            )
        }
        if (ModuleBannerConfig.enabled) {
            SwitchPreference(
                title = stringResource(R.string.module_banner_custom_opacity),
                summary = stringResource(R.string.module_banner_custom_opacity_summary),
                startAction = {
                    MiuixIcon(
                        Icons.Rounded.Wallpaper,
                        modifier = Modifier.padding(end = 6.dp),
                        contentDescription = stringResource(R.string.module_banner_custom_opacity),
                        tint = colorScheme.onBackground
                    )
                },
                checked = ModuleBannerConfig.customOpacityEnabled,
                onCheckedChange = ModuleBannerConfig::updateCustomOpacityEnabled,
            )
        }
        if (ModuleBannerConfig.enabled && ModuleBannerConfig.customOpacityEnabled) {
            ArrowPreference(
                title = stringResource(R.string.module_banner_opacity),
                startAction = {
                    MiuixIcon(
                        Icons.Rounded.Wallpaper,
                        modifier = Modifier.padding(end = 6.dp),
                        contentDescription = stringResource(R.string.module_banner_opacity),
                        tint = colorScheme.onBackground
                    )
                },
                endActions = {
                    MiuixText(
                        text = "${(sliderValue * 100f).roundToInt()}%",
                        color = colorScheme.onSurfaceVariantActions,
                    )
                },
                onClick = {},
                bottomAction = {
                    // Commit on release, like the page-scale slider in
                    // ColorPaletteScreenMiuix: updating ModuleBannerConfig
                    // on every tick mutates ModuleBannerConfig.customOpacity,
                    // which is the remember() key of sliderValue above, so
                    // the local state was re-initialized mid-drag and the
                    // thumb could not move.
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            ModuleBannerConfig.editCustomOpacity(snapToPercent(sliderValue))
                            ModuleBannerConfig.persist()
                        },
                        valueRange = 0.1f..1f,
                        showKeyPoints = true,
                        keyPoints = listOf(0.1f, 0.25f, 0.5f, 0.75f, 1f),
                        magnetThreshold = 0.01f,
                        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                    )
                },
            )
        }
    }

    BannerApiSourceDialogMiuix(
        show = showSourceDialog,
        onDismissRequest = { showSourceDialog = false },
    )
}

@Composable
private fun BannerApiSourceDialogMiuix(
    show: Boolean,
    onDismissRequest: () -> Unit,
) {
    OverlayDialog(
        show = show,
        title = stringResource(R.string.module_banner_api_source_dialog_title),
        onDismissRequest = onDismissRequest,
        content = {
            var text by remember(show) { mutableStateOf(ModuleBannerConfig.apiSource) }
            MiuixTextField(
                modifier = Modifier.padding(bottom = 16.dp),
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.module_banner_api_source_hint),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                MiuixTextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                MiuixTextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = {
                        ModuleBannerConfig.updateApiSource(text.trim())
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    )
}
