package com.sukisu.ultra.ui.screen.settings

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
import com.sukisu.ultra.ui.component.material.SegmentedCheckboxItem
import com.sukisu.ultra.ui.component.material.SegmentedColumn
import com.sukisu.ultra.ui.component.material.SegmentedListItem
import com.sukisu.ultra.ui.component.material.SegmentedSwitchItem
import com.sukisu.ultra.ui.navigation3.LocalNavigator
import com.sukisu.ultra.ui.navigation3.Route
import com.sukisu.ultra.ui.viewmodel.BackupViewModel

private val BackupGroupModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)

/**
 * Settings content for the on-demand backup feature: where to write (local folder) and what to
 * include (modules, allowlist, partitions).
 */
@Composable
fun BackupSettingsContent() {
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

    SegmentedColumn(modifier = BackupGroupModifier, title = stringResource(R.string.backup_section_targets)) {
        item {
            SegmentedSwitchItem(
                title = stringResource(R.string.backup_local_enabled),
                summary = stringResource(R.string.backup_local_summary),
                checked = state.localEnabled,
                onCheckedChange = viewModel::setLocalEnabled,
            )
        }
        item {
            SegmentedListItem(
                onClick = { openDownloads(context) },
                headlineContent = { Text(stringResource(R.string.backup_open_folder)) },
            )
        }
        item {
            SegmentedSwitchItem(
                title = stringResource(R.string.backup_before_flash),
                summary = stringResource(R.string.backup_before_flash_summary),
                checked = state.backupBeforeFlash,
                onCheckedChange = viewModel::setBackupBeforeFlash,
            )
        }
    }

    SegmentedColumn(modifier = BackupGroupModifier, title = stringResource(R.string.backup_section_content)) {
        item {
            SegmentedSwitchItem(
                title = stringResource(R.string.backup_modules),
                summary = stringResource(R.string.backup_modules_summary),
                checked = state.modulesEnabled,
                onCheckedChange = viewModel::setModulesEnabled,
            )
        }
        item {
            SegmentedSwitchItem(
                title = stringResource(R.string.backup_allowlist),
                checked = state.allowlistEnabled,
                onCheckedChange = viewModel::setAllowlistEnabled,
            )
        }
    }

    if (state.partitionsLoaded) {
        SegmentedColumn(
            modifier = BackupGroupModifier,
            title = stringResource(R.string.backup_partitions),
        ) {
            if (state.availablePartitions.isEmpty()) {
                item {
                    SegmentedListItem(
                        headlineContent = { Text(stringResource(R.string.backup_no_partitions)) },
                    )
                }
            }
            state.availablePartitions.forEach { partition ->
                item(key = partition) {
                    val (title, summary) = backupPartitionLabels(partition)
                    SegmentedCheckboxItem(
                        title = title,
                        summary = summary,
                        checked = partition in state.selectedPartitions,
                        onCheckedChange = { viewModel.setPartitionEnabled(partition, it) },
                    )
                }
            }
        }
    }

    SegmentedColumn(modifier = BackupGroupModifier) {
        item {
            SegmentedListItem(
                onClick = viewModel::backupNow,
                enabled = state.canBackup && !state.busy,
                headlineContent = {
                    Text(
                        stringResource(
                            if (state.busy) R.string.backup_running else R.string.backup_now
                        )
                    )
                },
            )
        }
        item {
            SegmentedListItem(
                onClick = { navigator.push(Route.BackupList) },
                headlineContent = { Text(stringResource(R.string.backup_open_list)) },
            )
        }
    }

    val personalization = rememberPersonalizationBackupActions()
    SegmentedColumn(modifier = BackupGroupModifier, title = stringResource(R.string.personalization_section)) {
        item {
            SegmentedListItem(
                onClick = personalization.export,
                headlineContent = { Text(stringResource(R.string.personalization_backup)) },
                supportingContent = { Text(stringResource(R.string.personalization_backup_summary)) },
            )
        }
        item {
            SegmentedListItem(
                onClick = personalization.restore,
                headlineContent = { Text(stringResource(R.string.personalization_restore)) },
                supportingContent = { Text(stringResource(R.string.personalization_restore_summary)) },
            )
        }
        item {
            SegmentedListItem(
                onClick = personalization.reset,
                headlineContent = { Text(stringResource(R.string.personalization_reset)) },
                supportingContent = { Text(stringResource(R.string.personalization_reset_summary)) },
            )
        }
    }
}

@Composable
internal fun backupPartitionLabels(partition: String): Pair<String, String?> = when (partition) {
    "boot" -> stringResource(R.string.backup_partition_boot) to
        stringResource(R.string.backup_partition_boot_summary)
    "init_boot" -> stringResource(R.string.backup_partition_init_boot) to
        stringResource(R.string.backup_partition_init_boot_summary)
    "vendor_boot" -> stringResource(R.string.backup_partition_vendor_boot) to
        stringResource(R.string.backup_partition_vendor_boot_summary)
    else -> partition to null
}

internal fun openDownloads(context: Context) {
    val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
