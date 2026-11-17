package com.sukisu.ultra.data.backup.device

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sukisu.ultra.BuildConfig
import com.sukisu.ultra.Natives
import com.sukisu.ultra.data.backup.AllowlistDocument
import com.sukisu.ultra.data.backup.backupResult
import com.sukisu.ultra.data.backup.backupIsoTimestamp
import com.sukisu.ultra.data.backup.source.AllowlistAccess
import com.sukisu.ultra.data.backup.toDto
import com.sukisu.ultra.data.backup.toProfile
import com.sukisu.ultra.data.repository.SuperUserRepository
import com.sukisu.ultra.data.repository.SuperUserRepositoryImpl

/**
 * An [AllowlistAccess] that reads and writes the kernel allow-list through [Natives].
 *
 * The kernel has no API to enumerate the whole allow-list, so export reuses the Manager's app list
 * (each installed package is queried with `getAppProfile`) and keeps the entries that are actually
 * granted root or carry a custom profile. Applying an entry mirrors a normal grant via
 * `setAppProfile`.
 */
class NativesAllowlistAccess(
    private val repository: SuperUserRepository = SuperUserRepositoryImpl(),
) : AllowlistAccess {

    override suspend fun exportDocument(): Result<AllowlistDocument> = withContext(Dispatchers.IO) {
        backupResult {
            val (apps, _) = repository.getAppList().getOrThrow()
            val entries = apps
                .filter { it.allowSu || it.hasCustomProfile }
                .mapNotNull { app -> app.profile?.toDto(app.profileKey, app.packageName) }
            AllowlistDocument(
                exportedAt = backupIsoTimestamp(System.currentTimeMillis()),
                appVersion = BuildConfig.VERSION_NAME,
                kernelUapiVersion = Natives.kernelUAPIVersion,
                entries = entries,
            )
        }
    }

    override suspend fun applyDocument(document: AllowlistDocument): Result<Unit> =
        withContext(Dispatchers.IO) {
            backupResult {
                document.entries.forEach { entry ->
                    check(Natives.setAppProfile(entry.toProfile())) {
                        "failed to apply allow-list entry: ${entry.key}"
                    }
                }
            }
        }
}
