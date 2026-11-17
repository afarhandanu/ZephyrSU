package com.sukisu.ultra.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sukisu.ultra.data.backup.BackupEngine
import com.sukisu.ultra.data.backup.BackupEntry
import com.sukisu.ultra.data.backup.BackupPaths
import com.sukisu.ultra.data.backup.BackupSourceRegistry
import com.sukisu.ultra.data.backup.BackupStorage
import com.sukisu.ultra.data.backup.RemoteEntry
import com.sukisu.ultra.data.backup.device.NativesAllowlistAccess
import com.sukisu.ultra.data.backup.device.RootModuleAccess
import com.sukisu.ultra.data.backup.device.RootPartitionAccess
import com.sukisu.ultra.data.backup.local.LocalBackupStorage
import com.sukisu.ultra.data.backup.source.AllowlistAccess
import com.sukisu.ultra.data.backup.source.AllowlistBackupSource
import com.sukisu.ultra.data.backup.source.ModuleAccess
import com.sukisu.ultra.data.backup.source.ModuleBackupSource
import com.sukisu.ultra.data.backup.source.PartitionAccess
import com.sukisu.ultra.data.backup.source.PartitionBackupSource
import com.sukisu.ultra.data.repository.SettingsRepository
import com.sukisu.ultra.data.repository.SettingsRepositoryImpl
import com.sukisu.ultra.ksuApp

data class BackupListItem(
    val entry: BackupEntry,
    val sizeBytes: Long,
)

sealed interface BackupNotice {
    data class Restored(val name: String) : BackupNotice
    data class Deleted(val name: String) : BackupNotice
    data class Failed(val message: String) : BackupNotice
}

data class BackupListUiState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val items: List<BackupListItem> = emptyList(),
    val notice: BackupNotice? = null,
)

/**
 * Lists local backups, and restores or deletes them.
 *
 * Restore re-reads the payload from the local folder, so an entry is streamed back through the
 * same source that produced it.
 */
class BackupListViewModel(
    private val settings: SettingsRepository = SettingsRepositoryImpl(),
    private val partitionAccess: PartitionAccess = RootPartitionAccess(),
    private val moduleAccess: ModuleAccess = RootModuleAccess(),
    private val allowlistAccess: AllowlistAccess = NativesAllowlistAccess(),
    private val local: BackupStorage = LocalBackupStorage(ksuApp),
) : ViewModel() {

    private val mutable = MutableStateFlow(BackupListUiState())
    val state = mutable.asStateFlow()

    /** Reloads the local folder; a failed list simply contributes no entries. */
    fun refresh() {
        viewModelScope.launch {
            mutable.update { it.copy(loading = true) }
            val items = withContext(Dispatchers.IO) {
                local.list("").getOrDefault(emptyList()).toItems()
            }
            mutable.update { it.copy(loading = false, items = items) }
        }
    }

    /** Restores [item] through the source registered for its kind. */
    fun restore(item: BackupListItem) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try {
                val engine = BackupEngine(sources(), listOf(local))
                withContext(Dispatchers.IO) {
                    engine.restore(item.entry.kind, item.entry, local).getOrThrow()
                }
                mutable.update { it.copy(busy = false, notice = BackupNotice.Restored(item.entry.displayName)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutable.update { it.copy(busy = false, notice = BackupNotice.Failed(e.message ?: e.javaClass.simpleName)) }
            }
        }
    }

    /** Deletes [item] and its metadata sidecar from the local folder, then reloads the list. */
    fun delete(item: BackupListItem) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    local.delete(item.entry.relativePath).getOrThrow()
                    BackupPaths.metaPathOf(item.entry.relativePath)?.let { local.delete(it).getOrThrow() }
                }
                mutable.update { it.copy(busy = false, notice = BackupNotice.Deleted(item.entry.displayName)) }
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutable.update { it.copy(busy = false, notice = BackupNotice.Failed(e.message ?: e.javaClass.simpleName)) }
            }
        }
    }

    fun clearNotice() {
        mutable.update { it.copy(notice = null) }
    }

    private fun sources(): BackupSourceRegistry {
        val tempDir = File(ksuApp.cacheDir, "backup-restore").apply { mkdirs() }
        return BackupSourceRegistry(
            listOf(
                ModuleBackupSource(moduleAccess, tempDir),
                AllowlistBackupSource(allowlistAccess),
                PartitionBackupSource(partitionAccess, settings.backupPartitions.toList(), tempDir),
            ),
        )
    }

    private fun List<RemoteEntry>.toItems(): List<BackupListItem> =
        mapNotNull { remote ->
            val entry = BackupPaths.parse(
                relativePath = remote.relativePath,
                sizeBytes = remote.sizeBytes,
                sha256 = remote.sha256.orEmpty(),
                createdAtEpochMs = remote.lastModifiedEpochMs ?: 0L,
            ) ?: return@mapNotNull null
            BackupListItem(
                entry = entry,
                sizeBytes = remote.sizeBytes,
            )
        }
}
