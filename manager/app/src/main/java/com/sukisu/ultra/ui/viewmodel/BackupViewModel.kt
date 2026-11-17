package com.sukisu.ultra.ui.viewmodel

import android.util.Log
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
import com.sukisu.ultra.data.backup.BackupPlan
import com.sukisu.ultra.data.backup.BackupSource
import com.sukisu.ultra.data.backup.BackupSourceRegistry
import com.sukisu.ultra.data.backup.BackupStorage
import com.sukisu.ultra.data.backup.device.NativesAllowlistAccess
import com.sukisu.ultra.data.backup.device.RootPartitionAccess
import com.sukisu.ultra.data.backup.local.LocalBackupStorage
import com.sukisu.ultra.data.backup.source.AllowlistAccess
import com.sukisu.ultra.data.backup.source.AllowlistBackupSource
import com.sukisu.ultra.data.backup.source.PartitionAccess
import com.sukisu.ultra.data.backup.source.PartitionBackupSource
import com.sukisu.ultra.data.backup.source.collectKnownStockSha1
import com.sukisu.ultra.data.repository.SettingsRepository
import com.sukisu.ultra.data.repository.SettingsRepositoryImpl
import com.sukisu.ultra.ksuApp

data class BackupOutcome(
    val written: Int,
    val skipped: Int,
    val failed: Int,
)

data class BackupUiState(
    val localEnabled: Boolean = true,
    val modulesEnabled: Boolean = true,
    val allowlistEnabled: Boolean = true,
    val selectedPartitions: Set<String> = emptySet(),
    val availablePartitions: List<String> = emptyList(),
    val partitionsLoaded: Boolean = false,
    val backupBeforeFlash: Boolean = false,
    val busy: Boolean = false,
    val outcome: BackupOutcome? = null,
    val error: String? = null,
) {
    val canBackup: Boolean get() = localEnabled &&
        (allowlistEnabled || selectedPartitions.isNotEmpty())
}

/** Manual runs export allowlists and partitions; module installers are archived only before flashing. */
class BackupViewModel(
    private val settings: SettingsRepository = SettingsRepositoryImpl(),
    private val partitionAccess: PartitionAccess = RootPartitionAccess(),
    private val allowlistAccess: AllowlistAccess = NativesAllowlistAccess(),
) : ViewModel() {

    private val mutable = MutableStateFlow(
        BackupUiState(
            localEnabled = settings.backupLocalEnabled,
            modulesEnabled = settings.backupModulesEnabled,
            allowlistEnabled = settings.backupAllowlistEnabled,
            selectedPartitions = settings.backupPartitions,
            backupBeforeFlash = settings.backupBeforeFlash,
        )
    )
    val state = mutable.asStateFlow()

    init {
        refreshPartitions()
    }

    private fun refreshPartitions() {
        viewModelScope.launch {
            val partitions = withContext(Dispatchers.IO) {
                partitionAccess.availablePartitions().getOrDefault(emptyList())
            }
            mutable.update { it.copy(availablePartitions = partitions, partitionsLoaded = true) }
        }
    }

    fun setLocalEnabled(value: Boolean) {
        settings.backupLocalEnabled = value
        mutable.update { it.copy(localEnabled = value) }
    }

    fun setModulesEnabled(value: Boolean) {
        settings.backupModulesEnabled = value
        mutable.update { it.copy(modulesEnabled = value) }
    }

    fun setAllowlistEnabled(value: Boolean) {
        settings.backupAllowlistEnabled = value
        mutable.update { it.copy(allowlistEnabled = value) }
    }

    fun setPartitionEnabled(partition: String, enabled: Boolean) {
        val updated = mutable.value.selectedPartitions.toMutableSet().apply {
            if (enabled) add(partition) else remove(partition)
        }
        settings.backupPartitions = updated
        mutable.update { it.copy(selectedPartitions = updated) }
    }

    fun setBackupBeforeFlash(value: Boolean) {
        settings.backupBeforeFlash = value
        mutable.update { it.copy(backupBeforeFlash = value) }
    }

    fun clearOutcome() {
        mutable.update { it.copy(outcome = null) }
    }

    fun clearError() {
        mutable.update { it.copy(error = null) }
    }

    fun backupNow() {
        val snapshot = mutable.value
        if (snapshot.busy || !snapshot.canBackup) return
        mutable.update { it.copy(busy = true, outcome = null, error = null) }
        viewModelScope.launch {
            try {
                val storages = buildStorages(snapshot)
                check(storages.isNotEmpty()) { "No backup destination configured" }
                val tempDir = File(ksuApp.cacheDir, "backup").apply { mkdirs() }
                val knownStockSha1 = withContext(Dispatchers.IO) { collectKnownStockSha1(storages) }
                val sources = buildSources(snapshot, tempDir, knownStockSha1)
                check(sources.kinds.isNotEmpty()) { "No backup source available" }
                val engine = BackupEngine(sources, storages)
                val outcome = withContext(Dispatchers.IO) {
                    val plan = BackupPlan(
                        localEnabled = snapshot.localEnabled,
                        modules = false,
                        allowlist = snapshot.allowlistEnabled,
                        partitions = snapshot.selectedPartitions,
                    )
                    var written = 0
                    var skipped = 0
                    var failed = 0
                    for (kind in plan.kinds) {
                        if (sources.sourceFor(kind) == null) continue
                        val result = engine.backup(kind)
                        result.failures.forEach { failure ->
                            Log.w(
                                "ZephyrSUBackup",
                                "backup $kind ${failure.operation} failed at ${failure.path} (${failure.storage})",
                                failure.cause,
                            )
                        }
                        written += result.written.size
                        skipped += result.skipped.size
                        failed += result.failures.size
                    }
                    BackupOutcome(written = written, skipped = skipped, failed = failed)
                }
                mutable.update { it.copy(busy = false, outcome = outcome) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("ZephyrSUBackup", "backup run failed", e)
                mutable.update { it.copy(busy = false, error = e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    private fun buildStorages(state: BackupUiState): List<BackupStorage> = buildList {
        if (state.localEnabled) add(LocalBackupStorage(ksuApp))
    }

    private fun buildSources(
        state: BackupUiState,
        tempDir: File,
        knownStockSha1: Set<String>,
    ): BackupSourceRegistry {
        val sources = mutableListOf<BackupSource>()
        if (state.allowlistEnabled) sources += AllowlistBackupSource(allowlistAccess)
        val partitions = state.availablePartitions.filter { it in state.selectedPartitions }
        if (partitions.isNotEmpty()) {
            sources += PartitionBackupSource(
                access = partitionAccess,
                partitions = partitions,
                tempDir = tempDir,
                knownStockSha1 = knownStockSha1,
            )
        }
        return BackupSourceRegistry(sources)
    }
}
