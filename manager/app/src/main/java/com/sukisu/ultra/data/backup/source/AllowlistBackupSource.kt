package com.sukisu.ultra.data.backup.source

import com.sukisu.ultra.data.backup.AllowlistDocument
import com.sukisu.ultra.data.backup.backupResult
import com.sukisu.ultra.data.backup.BackupArtifact
import com.sukisu.ultra.data.backup.BackupContent
import com.sukisu.ultra.data.backup.BackupEntry
import com.sukisu.ultra.data.backup.BackupKind
import com.sukisu.ultra.data.backup.BackupOrigin
import com.sukisu.ultra.data.backup.BackupSource
import com.sukisu.ultra.data.backup.allowlistFileName
import com.sukisu.ultra.data.backup.sha256

/**
 * Reads and applies the allow-list on behalf of an [AllowlistBackupSource].
 *
 * The real implementation talks to the kernel through [com.sukisu.ultra.Natives]; keeping it
 * behind this interface lets the source be tested without a rooted device.
 */
interface AllowlistAccess {

    /** Captures the current allow-list as a portable document. */
    suspend fun exportDocument(): Result<AllowlistDocument>

    /** Applies the saved profiles without removing entries absent from [document]. */
    suspend fun applyDocument(document: AllowlistDocument): Result<Unit>
}

/**
 * Exports the allow-list and per-app profiles as a single JSON document.
 *
 * The document is self-validating (see [AllowlistDocument]) and carries its own schema/version, so
 * restoring one is a pure re-application through [AllowlistAccess.applyDocument].
 */
class AllowlistBackupSource(
    private val access: AllowlistAccess,
    private val clock: () -> Long = System::currentTimeMillis,
) : BackupSource {

    override val kind: BackupKind = BackupKind.ALLOWLIST

    override suspend fun list(): Result<List<BackupEntry>> = access.exportDocument().map { document ->
        val bytes = document.toJson().toByteArray(Charsets.UTF_8)
        listOf(
            BackupEntry(
                id = "allowlist",
                kind = kind,
                displayName = "allowlist",
                sizeBytes = bytes.size.toLong(),
                source = BackupOrigin.EXPORTED,
            ),
        )
    }

    override suspend fun export(): Result<List<BackupArtifact>> = backupResult {
        val document = access.exportDocument().getOrThrow()
        val bytes = document.toJson().toByteArray(Charsets.UTF_8)
        val fileName = allowlistFileName(clock())
        listOf(
            BackupArtifact(
                kind = kind,
                moduleId = null,
                fileName = fileName,
                metaFileName = null,
                sha256 = bytes.sha256(),
                metaJson = null,
                bytes = bytes,
            ),
        )
    }

    override suspend fun restore(entry: BackupEntry, content: BackupContent): Result<Unit> = backupResult {
        val text = content.open().use { it.readBytes() }.toString(Charsets.UTF_8)
        val document = AllowlistDocument.fromJson(text).getOrThrow()
        access.applyDocument(document).getOrThrow()
    }

    override suspend fun delete(entry: BackupEntry): Result<Unit> = Result.success(Unit)
}
