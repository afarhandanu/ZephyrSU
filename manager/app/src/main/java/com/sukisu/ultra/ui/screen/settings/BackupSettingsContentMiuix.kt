package com.sukisu.ultra.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.component.miuix.AppCard
import com.sukisu.ultra.ui.navigation3.LocalNavigator
import com.sukisu.ultra.ui.navigation3.Route
import com.sukisu.ultra.ui.theme.backgroundAwareCardColors
import com.sukisu.ultra.ui.viewmodel.BackupViewModel
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * Backup settings (Miuix). Same switches and rows as the Material [BackupSettingsContent],
 * rendered with Miuix preferences; partitions use switches instead of checkboxes (Miuix has no
 * checkbox preference in this codebase).
 *
 * [modifier] lets the settings screen attach its anchor requester for search scrolling.
 */
@Composable
fun BackupSettingsContentMiuix(modifier: Modifier = Modifier) {
    val viewModel = viewModel<BackupViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val navigator = LocalNavigator.current

    LaunchedEffect(state.outcome) {
        val outcome = state.outcome ?: return@LaunchedEffect
        val text = resources.getString(
            R.string.backup_result,
            outcome.written,
            outcome.skipped,
            outcome.failed,
        )
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        viewModel.clearOutcome()
    }
    LaunchedEffect(state.error) {
        val error = state.error ?: return@LaunchedEffect
        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        viewModel.clearError()
    }

    AppCard(
        modifier = modifier
            .padding(top = 12.dp)
            .fillMaxWidth(),
        colors = backgroundAwareCardColors(),
    ) {
        SwitchPreference(
            title = stringResource(R.string.backup_local_enabled),
            summary = stringResource(R.string.backup_local_summary),
            startAction = {
                MiuixIcon(
                    Icons.Rounded.Backup,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(R.string.backup_local_enabled),
                    tint = colorScheme.onBackground
                )
            },
            checked = state.localEnabled,
            onCheckedChange = viewModel::setLocalEnabled,
        )
        ArrowPreference(
            title = stringResource(R.string.backup_open_folder),
            startAction = {
                MiuixIcon(
                    Icons.Rounded.FolderOpen,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(R.string.backup_open_folder),
                    tint = colorScheme.onBackground
                )
            },
            onClick = { openDownloads(context) },
        )
        SwitchPreference(
            title = stringResource(R.string.backup_before_flash),
            summary = stringResource(R.string.backup_before_flash_summary),
            startAction = {
                MiuixIcon(
                    Icons.Rounded.FlashOn,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(R.string.backup_before_flash),
                    tint = colorScheme.onBackground
                )
            },
            checked = state.backupBeforeFlash,
            onCheckedChange = viewModel::setBackupBeforeFlash,
        )
        SwitchPreference(
            title = stringResource(R.string.backup_modules),
            summary = stringResource(R.string.backup_modules_summary),
            startAction = {
                MiuixIcon(
                    Icons.Rounded.Extension,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(R.string.backup_modules),
                    tint = colorScheme.onBackground
                )
            },
            checked = state.modulesEnabled,
            onCheckedChange = viewModel::setModulesEnabled,
        )
        SwitchPreference(
            title = stringResource(R.string.backup_allowlist),
            startAction = {
                MiuixIcon(
                    Icons.Rounded.Shield,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(R.string.backup_allowlist),
                    tint = colorScheme.onBackground
                )
            },
            checked = state.allowlistEnabled,
            onCheckedChange = viewModel::setAllowlistEnabled,
        )
        if (state.partitionsLoaded) {
            state.availablePartitions.forEach { partition ->
                val (title, summary) = backupPartitionLabels(partition)
                SwitchPreference(
                    title = title,
                    summary = summary,
                    checked = partition in state.selectedPartitions,
                    onCheckedChange = { viewModel.setPartitionEnabled(partition, it) },
                )
            }
        }
        ArrowPreference(
            title = stringResource(if (state.busy) R.string.backup_running else R.string.backup_now),
            startAction = {
                MiuixIcon(
                    Icons.Rounded.Save,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(R.string.backup_now),
                    tint = colorScheme.onBackground
                )
            },
            onClick = { if (state.canBackup && !state.busy) viewModel.backupNow() },
        )
        ArrowPreference(
            title = stringResource(R.string.backup_open_list),
            startAction = {
                MiuixIcon(
                    Icons.Rounded.List,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(R.string.backup_open_list),
                    tint = colorScheme.onBackground
                )
            },
            onClick = { navigator.push(Route.BackupList) },
        )
        val personalization = rememberPersonalizationBackupActions()
        ArrowPreference(
            title = stringResource(R.string.personalization_backup),
            summary = stringResource(R.string.personalization_backup_summary),
            startAction = {
                MiuixIcon(
                    Icons.Rounded.Save,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(R.string.personalization_backup),
                    tint = colorScheme.onBackground
                )
            },
            onClick = personalization.export,
        )
        ArrowPreference(
            title = stringResource(R.string.personalization_restore),
            summary = stringResource(R.string.personalization_restore_summary),
            startAction = {
                MiuixIcon(
                    Icons.Rounded.FolderOpen,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(R.string.personalization_restore),
                    tint = colorScheme.onBackground
                )
            },
            onClick = personalization.restore,
        )
        ArrowPreference(
            title = stringResource(R.string.personalization_reset),
            summary = stringResource(R.string.personalization_reset_summary),
            startAction = {
                MiuixIcon(
                    Icons.Rounded.Shield,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(R.string.personalization_reset),
                    tint = colorScheme.onBackground
                )
            },
            onClick = personalization.reset,
        )
    }
}
