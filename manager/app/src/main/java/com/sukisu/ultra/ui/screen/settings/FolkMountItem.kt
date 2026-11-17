package com.sukisu.ultra.ui.screen.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.data.model.FolkMountMode
import com.sukisu.ultra.data.model.FolkMountProvider
import com.sukisu.ultra.ui.component.folkMountWarningText
import com.sukisu.ultra.ui.component.material.SegmentedDropdownItem
import com.sukisu.ultra.ui.component.miuix.AppCard
import com.sukisu.ultra.ui.theme.backgroundAwareCardColors
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * Folk Mount mode picker, ported from FinaSU (M6.3).
 *
 * The mode is written by ksud (`ksud mount set-mode <token>`) and only takes
 * effect after a reboot. The NoMount switch is confirmed in SettingsScreen
 * before it reaches the ViewModel, because it changes who can see the
 * injected files.
 */
@Composable
private fun folkMountSummary(uiState: SettingsUiState): String {
    val folkMountStatus = uiState.folkMountStatus
    val available = folkMountStatus != null && !uiState.folkMountReadError
    val mode = folkMountStatus?.configuredMode
    return when {
        uiState.isFolkMountLoading ->
            stringResource(id = R.string.settings_folk_mount_summary)

        !available ->
            stringResource(id = R.string.settings_folk_mount_unavailable_summary)

        else -> {
            val base = stringResource(
                id = when (mode) {
                    FolkMountMode.AUTO -> R.string.settings_folk_mount_mode_auto_summary
                    FolkMountMode.BUILTIN -> R.string.settings_folk_mount_mode_builtin_summary
                    FolkMountMode.METAMODULE -> R.string.settings_folk_mount_mode_metamodule_summary
                    FolkMountMode.NOMOUNT -> R.string.settings_folk_mount_mode_nomount_summary
                    FolkMountMode.OFF -> R.string.settings_folk_mount_mode_off_summary
                    null -> R.string.settings_folk_mount_summary
                }
            )
            val bootLine = when (folkMountStatus.bootProvider) {
                FolkMountProvider.BUILTIN -> stringResource(
                    id = R.string.settings_folk_mount_boot_provider,
                    stringResource(id = R.string.settings_folk_mount_provider_builtin)
                )

                FolkMountProvider.METAMODULE -> stringResource(
                    id = R.string.settings_folk_mount_boot_provider,
                    stringResource(id = R.string.settings_folk_mount_provider_metamodule)
                )

                FolkMountProvider.NOMOUNT -> stringResource(
                    id = R.string.settings_folk_mount_boot_provider,
                    stringResource(id = R.string.settings_folk_mount_provider_nomount)
                )

                FolkMountProvider.NONE -> stringResource(
                    id = R.string.settings_folk_mount_provider_none
                )

                null -> stringResource(id = R.string.settings_folk_mount_boot_provider_unknown)
            }
            val compatHint = when {
                mode == FolkMountMode.BUILTIN && folkMountStatus.metamoduleEnabled ->
                    stringResource(id = R.string.settings_folk_mount_metamodule_compat_hint)

                mode == FolkMountMode.NOMOUNT &&
                    folkMountStatus.bootProvider != null &&
                    folkMountStatus.bootProvider != FolkMountProvider.NOMOUNT ->
                    folkMountWarningText(folkMountStatus)
                        ?: stringResource(
                            id = R.string.settings_folk_mount_nomount_unavailable_hint
                        )

                else -> null
            }
            listOfNotNull(
                base,
                bootLine,
                stringResource(id = R.string.settings_folk_mount_reboot_hint),
                compatHint,
            ).joinToString("\n")
        }
    }
}

@Composable
private fun folkMountModeItems(): List<String> = listOf(
    stringResource(id = R.string.settings_folk_mount_mode_auto),
    stringResource(id = R.string.settings_folk_mount_mode_builtin),
    stringResource(id = R.string.settings_folk_mount_mode_metamodule),
    stringResource(id = R.string.settings_folk_mount_mode_nomount),
    stringResource(id = R.string.settings_folk_mount_mode_off),
)

@Composable
internal fun folkMountItem(uiState: SettingsUiState, actions: SettingsScreenActions) {
    val folkMountStatus = uiState.folkMountStatus
    val available = folkMountStatus != null && !uiState.folkMountReadError
    val mode = folkMountStatus?.configuredMode
    SegmentedDropdownItem(
        icon = Icons.Filled.Layers,
        title = stringResource(id = R.string.settings_folk_mount),
        summary = folkMountSummary(uiState),
        items = if (available) folkMountModeItems() else emptyList(),
        enabled = available && !uiState.isFolkMountLoading && !uiState.isFolkMountWriting,
        selectedIndex = mode?.ordinal ?: 0,
        onItemSelected = { index ->
            FolkMountMode.entries.getOrNull(index)?.let(actions.onSetFolkMountMode)
        }
    )
}

@Composable
internal fun FolkMountSectionMiuix(
    state: SettingsUiState,
    actions: SettingsScreenActions,
    modifier: Modifier = Modifier,
) {
    val folkMountStatus = state.folkMountStatus
    val available = folkMountStatus != null && !state.folkMountReadError
    val mode = folkMountStatus?.configuredMode
    AppCard(
        modifier = modifier
            .padding(top = 12.dp)
            .fillMaxWidth(),
        colors = backgroundAwareCardColors(),
    ) {
        OverlayDropdownPreference(
            title = stringResource(id = R.string.settings_folk_mount),
            summary = folkMountSummary(state),
            items = if (available) folkMountModeItems() else emptyList(),
            startAction = {
                Icon(
                    Icons.Filled.Layers,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(id = R.string.settings_folk_mount),
                    tint = colorScheme.onBackground
                )
            },
            enabled = available && !state.isFolkMountLoading && !state.isFolkMountWriting,
            selectedIndex = mode?.ordinal ?: 0,
            onSelectedIndexChange = { index ->
                FolkMountMode.entries.getOrNull(index)?.let(actions.onSetFolkMountMode)
            }
        )
    }
}
