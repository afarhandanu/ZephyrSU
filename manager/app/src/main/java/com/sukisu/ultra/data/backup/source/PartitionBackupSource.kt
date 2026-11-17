package com.sukisu.ultra.data.backup.source

import java.io.File
import com.sukisu.ultra.data.backup.backupResult
import com.sukisu.ultra.data.backup.BackupArtifact
import com.sukisu.ultra.data.backup.BackupContent
import com.sukisu.ultra.data.backup.BackupEntry
import com.sukisu.ultra.data.backup.BackupKind
import com.sukisu.ultra.data.backup.BackupOrigin
import com.sukisu.ultra.data.backup.BackupSource
import com.sukisu.ultra.data.backup.BackupStorage
import com.sukisu.ultra.data.backup.BackupTrigger
import com.sukisu.ultra.data.backup.BackupPaths
import com.sukisu.ultra.data.backup.BOOT_MARKER_CURRENT
import com.sukisu.ultra.data.backup.BOOT_MARKER_PATCHED
import com.sukisu.ultra.data.backup.BOOT_MARKER_STOCK
import com.sukisu.ultra.data.backup.BootBackupMeta
import com.sukisu.ultra.data.backup.backupIsoTimestamp
import com.sukisu.ultra.data.backup.bootBackupName
import com.sukisu.ultra.data.backup.bootMetaFileName
import com.sukisu.ultra.data.backup.digests
import com.sukisu.ultra.data.backup.sanitizeSegment

/** Best-effort identity of the device a partition image was captured from. */
data class DeviceInfo(val device: String, val fingerprint: String) {
    companion object {
        /** Placeholder used when the device identity cannot be read. */
        val UNKNOWN = DeviceInfo(device = "", fingerprint = "")
    }
}

/**
 * Reads and writes raw block partitions on behalf of a [PartitionBackupSource].
 *
 * The real implementation runs privileged shell commands; keeping it behind this interface lets the
 * source logic (naming, hashing, sidecars) be exercised without a rooted device.
 */
interface PartitionAccess {

    /** Partitions present on this device, in a stable order. */
    suspend fun availablePartitions(): Result<List<String>>

    /** Copies the raw contents of [partition] into [target]. */
    suspend fun readPartition(partition: String, target: File): Result<Unit>

    /** Overwrites [partition] with the raw contents of [source]. */
    suspend fun writePartition(partition: String, source: File): Result<Unit>

    /** Reads the device identity written into the boot sidecar. */
    suspend fun deviceInfo(): Result<DeviceInfo>
}

/** Exports available selected partitions as raw images with [BootBackupMeta] sidecars. */
class PartitionBackupSource(
    private val access: PartitionAccess,
    private val partitions: List<String>,
    private val tempDir: File,
    private val slotSuffix: String? = null,
    private val trigger: BackupTrigger = BackupTrigger.MANUAL,
    private val knownStockSha1: Set<String> = emptySet(),
    private val clock: () -> Long = System::currentTimeMillis,
) : BackupSource {

    override val kind: BackupKind = BackupKind.BOOT

    override suspend fun list(): Result<List<BackupEntry>> = access.availablePartitions().map { available ->
        available.filter { it in partitions }.map { partition ->
            val target = targetName(partition)
            BackupEntry(
                id = target,
                kind = kind,
                displayName = target,
                source = BackupOrigin.EXPORTED,
            )
        }
    }

    override suspend fun export(): Result<List<BackupArtifact>> = backupResult {
        val available = access.availablePartitions().getOrThrow()
        val device = access.deviceInfo().getOrElse { DeviceInfo.UNKNOWN }
        val createdAt = backupIsoTimestamp(clock())
        val created = mutableListOf<File>()
        try {
            available.filter { it in partitions }.map { partition ->
                val target = targetName(partition)
                val file = File(tempDir, "partition-${sanitizeSegment(target)}.img")
                created += file
                access.readPartition(target, file).getOrThrow()
                val digests = file.digests()
                val marker = resolveMarker(digests.sha1, trigger, knownStockSha1)
                val fileName = bootBackupName(target, digests.sha1, marker)
                val meta = BootBackupMeta(
                    partition = target,
                    sha1 = digests.sha1,
                    sizeBytes = file.length(),
                    device = device.device,
                    fingerprint = device.fingerprint,
                    createdAt = createdAt,
                    trigger = trigger,
                )
                BackupArtifact(
                    kind = kind,
                    moduleId = null,
                    fileName = fileName,
                    metaFileName = bootMetaFileName(fileName),
                    sha256 = digests.sha256,
                    metaJson = meta.toJson(),
                    file = file,
                )
            }
        } catch (e: Throwable) {
            // A failed export never hands its artifacts to the engine, so images already produced
            // would leak; drop them before surfacing the error.
            created.forEach { it.delete() }
            throw e
        }
    }

    override suspend fun restore(entry: BackupEntry, content: BackupContent): Result<Unit> = backupResult {
        val temp = File.createTempFile("restore-", ".img", tempDir)
        try {
            content.open().use { input -> temp.outputStream().use { input.copyTo(it) } }
            access.writePartition(entry.id, temp).getOrThrow()
        } finally {
            temp.delete()
        }
    }

    override suspend fun delete(entry: BackupEntry): Result<Unit> = Result.success(Unit)

    private fun targetName(partition: String): String =
        if (slotSuffix.isNullOrEmpty() ||
            partition.endsWith("_a") ||
            partition.endsWith("_b")
        ) {
            partition
        } else {
            partition + slotSuffix
        }

    companion object {
        /** Pre-flash captures retain the stock marker; manual captures compare saved references. */
        fun resolveMarker(
            sha1: String,
            trigger: BackupTrigger,
            knownStockSha1: Set<String>,
        ): String = when {
            sha1 in knownStockSha1 -> BOOT_MARKER_STOCK
            trigger == BackupTrigger.PRE_FLASH -> BOOT_MARKER_STOCK
            knownStockSha1.isEmpty() -> BOOT_MARKER_CURRENT
            else -> BOOT_MARKER_PATCHED
        }
    }
}

/**
 * Collects the SHA-1 of every stock boot image already present in [storages].
 *
 * Used to label a fresh capture `stock` when it matches an image saved earlier, so the comparison
 * spans every enabled destination. Best-effort: a backend that cannot be listed, or a name that is
 * not a stock capture, simply contributes nothing.
 */
suspend fun collectKnownStockSha1(storages: Collection<BackupStorage>): Set<String> {
    val sha1s = mutableSetOf<String>()
    for (storage in storages) {
        val entries = storage.list(BackupPaths.BOOT_DIR).getOrNull() ?: continue
        for (entry in entries) {
            val fileName = entry.relativePath.substringAfterLast('/')
            if (BackupPaths.markerOf(fileName) != BOOT_MARKER_STOCK) continue
            BackupPaths.bootSha1Of(fileName)?.let { sha1s += it }
        }
    }
    return sha1s
}
