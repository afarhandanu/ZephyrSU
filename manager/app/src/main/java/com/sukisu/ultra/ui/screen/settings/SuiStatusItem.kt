package com.sukisu.ultra.ui.screen.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.data.model.SuiStatus
import com.sukisu.ultra.ui.component.dialog.rememberConfirmDialog
import com.sukisu.ultra.ui.component.material.IconTile
import com.sukisu.ultra.ui.component.material.SegmentedColumnScope
import com.sukisu.ultra.ui.component.material.SegmentedListItem
import com.sukisu.ultra.ui.component.material.SegmentedSwitchItem
import com.sukisu.ultra.ui.component.miuix.AppCard
import com.sukisu.ultra.ui.theme.backgroundAwareCardColors
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme as miuixColorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles as miuixTextStyles

/**
 * Sui is a Zygisk module, so its switch stays disabled until ksud reports a usable provider.
 * The permission UI itself stays Sui's own; this section only owns install, enable and diagnosis.
 */
internal fun SegmentedColumnScope.suiStatusItems(
    state: SettingsUiState,
    actions: SettingsScreenActions,
) {
    item(key = "sui_toggle") {
        val status = state.suiStatus
        val confirm = rememberConfirmDialog(onConfirm = { actions.onSetSuiEnabled(true) })
        val notInstalled = stringResource(R.string.sui_not_installed)
        val zygiskText = status?.let { stringResource(zygiskStateRes(it)) }.orEmpty()
        val confirmTitle = stringResource(R.string.sui_confirm_title)
        val confirmAccept = stringResource(R.string.sui_confirm_confirm)
        val confirmCancel = stringResource(R.string.sui_confirm_cancel)
        val confirmBody = status?.let {
            stringResource(
                R.string.sui_confirm_body,
                zygiskText,
                it.payloadVersion.ifBlank { notInstalled },
            )
        }
        val summary = suiSummaryText(state)
        val actionable = status != null && !state.suiReadError && !state.isSuiLoading && !state.isSuiWriting

        SegmentedSwitchItem(
            icon = Icons.Filled.WaterDrop,
            title = stringResource(R.string.sui_title),
            summary = summary,
            checked = status?.selected == true,
            enabled = actionable && status.conflicts.isEmpty(),
            onCheckedChange = { wanted ->
                if (!wanted) {
                    actions.onSetSuiEnabled(false)
                } else {
                    confirm.showConfirm(
                        title = confirmTitle,
                        content = confirmBody,
                        confirm = confirmAccept,
                        dismiss = confirmCancel,
                    )
                }
            },
        )
    }

    val status = state.suiStatus
    if (status != null && status.installed) {
        item(key = "sui_manage") {
            val manageTitle = stringResource(R.string.sui_manage)
            SegmentedListItem(
                headlineContent = { Text(manageTitle) },
                supportingContent = { Text(stringResource(R.string.sui_manage_summary)) },
                leadingContent = { IconTile(Icons.AutoMirrored.Filled.OpenInNew, manageTitle) },
                enabled = !state.isSuiWriting,
                onClick = actions.onManageSuiPermissions,
            )
        }
    }

    if (status != null && status.needsDiagnostics) {
        item(key = "sui_diag_injection") {
            SegmentedListItem(
                headlineContent = { Text(stringResource(R.string.sui_diag_injection)) },
                supportingContent = { Text(stringResource(zygiskStateRes(status))) },
            )
        }
        item(key = "sui_diag_payload") {
            val payload = payloadLabel(status)
            SegmentedListItem(
                headlineContent = { Text(stringResource(R.string.sui_diag_payload)) },
                supportingContent = { Text("$payload · ${status.payloadVersion.ifBlank { DEFAULT_VERSION }}") },
            )
        }
        item(key = "sui_diag_daemons") {
            SegmentedListItem(
                headlineContent = { Text(stringResource(R.string.sui_diag_daemons)) },
                supportingContent = {
                    Text(
                        stringResource(
                            if (status.daemonHealthy) R.string.sui_diag_daemons_ok
                            else R.string.sui_diag_daemons_stopped,
                        ),
                    )
                },
            )
        }
        if (status.conflicts.isNotEmpty()) {
            item(key = "sui_diag_conflicts") {
                SegmentedListItem(
                    headlineContent = { Text(stringResource(R.string.sui_diag_conflicts)) },
                    supportingContent = { Text(status.conflicts.joinToString()) },
                )
            }
        }
        state.suiManagementAvailable?.let { available ->
            item(key = "sui_diag_management") {
                SegmentedListItem(
                    headlineContent = { Text(stringResource(R.string.sui_diag_management)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                if (available) R.string.sui_management_ok
                                else R.string.sui_management_unavailable,
                            ),
                        )
                    },
                )
            }
        }
        if (!status.installed || !status.payloadCurrent) {
            item(key = "sui_repair") {
                val repairTitle = stringResource(R.string.sui_repair)
                SegmentedListItem(
                    headlineContent = { Text(repairTitle) },
                    supportingContent = { Text(stringResource(R.string.sui_repair_summary)) },
                    leadingContent = { IconTile(Icons.Filled.Build, repairTitle) },
                    enabled = !state.isSuiWriting,
                    onClick = actions.onRepairSui,
                )
            }
        }
    }
}

@Composable
internal fun SuiStatusSectionMiuix(
    state: SettingsUiState,
    actions: SettingsScreenActions,
    modifier: Modifier = Modifier,
) {
    val status = state.suiStatus
    val confirm = rememberConfirmDialog(onConfirm = { actions.onSetSuiEnabled(true) })
    val notInstalled = stringResource(R.string.sui_not_installed)
    val zygiskText = status?.let { stringResource(zygiskStateRes(it)) }.orEmpty()
    val confirmTitle = stringResource(R.string.sui_confirm_title)
    val confirmAccept = stringResource(R.string.sui_confirm_confirm)
    val confirmCancel = stringResource(R.string.sui_confirm_cancel)
    val confirmBody = status?.let {
        stringResource(
            R.string.sui_confirm_body,
            zygiskText,
            it.payloadVersion.ifBlank { notInstalled },
        )
    }
    val summary = suiSummaryText(state)
    val actionable = status != null && !state.suiReadError && !state.isSuiLoading && !state.isSuiWriting

    AppCard(
        modifier = modifier
            .padding(top = 12.dp)
            .fillMaxWidth(),
        colors = backgroundAwareCardColors(),
    ) {
        SwitchPreference(
            title = stringResource(R.string.sui_title),
            summary = summary,
            startAction = {
                MiuixIcon(
                    Icons.Rounded.WaterDrop,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(R.string.sui_title),
                    tint = miuixColorScheme.onBackground,
                )
            },
            enabled = actionable && status.conflicts.isEmpty(),
            checked = status?.selected == true,
            onCheckedChange = { wanted ->
                if (!wanted) {
                    actions.onSetSuiEnabled(false)
                } else {
                    confirm.showConfirm(
                        title = confirmTitle,
                        content = confirmBody,
                        confirm = confirmAccept,
                        dismiss = confirmCancel,
                    )
                }
            },
        )

        if (status != null && status.installed) {
            ArrowPreference(
                title = stringResource(R.string.sui_manage),
                summary = stringResource(R.string.sui_manage_summary),
                startAction = {
                    MiuixIcon(
                        Icons.AutoMirrored.Rounded.OpenInNew,
                        modifier = Modifier.padding(end = 6.dp),
                        contentDescription = stringResource(R.string.sui_manage),
                        tint = miuixColorScheme.onBackground,
                    )
                },
                enabled = !state.isSuiWriting,
                onClick = actions.onManageSuiPermissions,
            )
        }

        if (status != null && status.needsDiagnostics) {
            MiuixDiagRow(
                title = stringResource(R.string.sui_diag_injection),
                summary = stringResource(zygiskStateRes(status)),
            )
            MiuixDiagRow(
                title = stringResource(R.string.sui_diag_payload),
                summary = "${payloadLabel(status)} · ${status.payloadVersion.ifBlank { DEFAULT_VERSION }}",
            )
            MiuixDiagRow(
                title = stringResource(R.string.sui_diag_daemons),
                summary = stringResource(
                    if (status.daemonHealthy) R.string.sui_diag_daemons_ok
                    else R.string.sui_diag_daemons_stopped,
                ),
            )
            if (status.conflicts.isNotEmpty()) {
                MiuixDiagRow(
                    title = stringResource(R.string.sui_diag_conflicts),
                    summary = status.conflicts.joinToString(),
                )
            }
            state.suiManagementAvailable?.let { available ->
                MiuixDiagRow(
                    title = stringResource(R.string.sui_diag_management),
                    summary = stringResource(
                        if (available) R.string.sui_management_ok
                        else R.string.sui_management_unavailable,
                    ),
                )
            }
            if (!status.installed || !status.payloadCurrent) {
                ArrowPreference(
                    title = stringResource(R.string.sui_repair),
                    summary = stringResource(R.string.sui_repair_summary),
                    startAction = {
                        MiuixIcon(
                            Icons.Rounded.Build,
                            modifier = Modifier.padding(end = 6.dp),
                            contentDescription = stringResource(R.string.sui_repair),
                            tint = miuixColorScheme.onBackground,
                        )
                    },
                    enabled = !state.isSuiWriting,
                    onClick = actions.onRepairSui,
                )
            }
        }
    }
}

@Composable
private fun MiuixDiagRow(title: String, summary: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        MiuixText(
            text = title,
            style = miuixTextStyles.headline1,
            color = miuixColorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        MiuixText(
            text = summary,
            style = miuixTextStyles.body2,
            color = miuixColorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun suiSummaryText(state: SettingsUiState): String {
    val status = state.suiStatus
    val stateText = status?.let { stringResource(suiStateRes(it)) }
    val reasonText = status?.let { suiReasonRes(it)?.let { res -> stringResource(res, it.detail) } }
    val zygiskText = status?.let { stringResource(zygiskStateRes(it)) }.orEmpty()
    return when {
        state.isSuiLoading -> stringResource(R.string.sui_loading)
        state.suiReadError || status == null -> stringResource(R.string.sui_read_error)
        else -> listOfNotNull(
            stateText,
            reasonText,
            stringResource(R.string.sui_dependency, zygiskText),
        ).joinToString("\n")
    }
}

@Composable
private fun payloadLabel(status: SuiStatus): String = when {
    !status.installed -> stringResource(R.string.sui_diag_payload_absent)
    status.payloadCurrent -> stringResource(R.string.sui_diag_payload_ok)
    else -> stringResource(R.string.sui_diag_payload_stale)
}

private const val DEFAULT_VERSION = "-"

@StringRes
private fun suiStateRes(status: SuiStatus): Int = when (status.state) {
    SuiStatus.STATE_ENABLED -> R.string.sui_state_enabled
    SuiStatus.STATE_PENDING_REBOOT -> R.string.sui_state_pending
    SuiStatus.STATE_EXTERNAL_UNVERIFIED -> R.string.sui_state_external
    SuiStatus.STATE_UNAVAILABLE -> R.string.sui_state_unavailable
    SuiStatus.STATE_ERROR -> R.string.sui_state_error
    else -> R.string.sui_state_disabled
}

@StringRes
private fun suiReasonRes(status: SuiStatus): Int? = when (status.reason) {
    "zygisk_disabled" -> R.string.sui_reason_zygisk_disabled
    "zygisk_engine_missing" -> R.string.sui_reason_zygisk_engine_missing
    "zygisk_startup_protection" -> R.string.sui_reason_zygisk_startup
    "zygisk_injection_failed" -> R.string.sui_reason_injection_failed
    "conflict_existing_sui" -> R.string.sui_reason_conflict
    "payload_missing" -> R.string.sui_reason_payload_missing
    "module_disabled" -> R.string.sui_reason_module_disabled
    "enabled_externally" -> R.string.sui_reason_enabled_externally
    "not_installed" -> null
    else -> R.string.sui_reason_unknown.takeIf { status.state == SuiStatus.STATE_ERROR }
}

@StringRes
private fun zygiskStateRes(status: SuiStatus): Int = when {
    status.externalProviders.isNotEmpty() -> R.string.sui_zygisk_external
    !status.zygiskEnabled || !status.engineAvailable -> R.string.sui_zygisk_disabled
    status.injectionState == "injected" -> R.string.sui_zygisk_injected
    else -> R.string.sui_zygisk_pending
}
