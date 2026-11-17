package com.sukisu.ultra.data.modulestore

import com.sukisu.ultra.data.model.Author
import com.sukisu.ultra.data.model.RepoModule
import com.sukisu.ultra.data.model.RepoModuleDetail
import com.sukisu.ultra.data.model.RepoModuleList
import com.sukisu.ultra.data.model.RepoRelease
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Adapts FinaSU's store model to Nova's module-repo model, so the Module Repo UI
 * ([ModuleRepoViewModel], detail screen) can list and install modules from any
 * [ModuleStoreSource] without a second UI.
 *
 * Notes on the mapping:
 * - Store sources carry no stars, tags or icons, so those fields stay empty.
 * - [StoreModule.parameter] marks kernel-module entries in the folk index; Nova's [RepoModule]
 *   has no such field, and FinaSU installs them through the normal module path as well, so the
 *   flag is not carried over.
 * - Releases without a recorded size/SHA-256 install unverified, exactly as in FinaSU
 *   ([ModuleDownloadButton] passes `expected = null` then). Only hash-pinned releases count as
 *   [RepoRelease.immutable].
 */
fun StoreModule.toRepoModule(): RepoModule {
    val latest = latestRelease
    val (_, epoch) = formatEpochSeconds(latest?.timestamp ?: 0.0)
    return RepoModule(
        moduleId = id,
        moduleName = name.ifBlank { id },
        authors = author,
        authorList = author.takeIf { it.isNotBlank() }?.let { listOf(Author(it, "")) } ?: emptyList(),
        summary = description,
        tags = emptyList(),
        metamodule = false,
        isScript = false,
        iconUrl = null,
        latestVersion = version,
        latestVersionCode = versionCode.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
        latestReleaseTime = formatEpochSeconds(latest?.timestamp ?: 0.0).first,
        latestReleaseEpoch = epoch,
        stars = null,
        starsRepository = null,
    )
}

fun StoreModule.toRepoModuleDetail(): RepoModuleDetail = RepoModuleDetail(
    module = toRepoModule(),
    license = license,
    homepage = homepage,
    support = support,
    sourceFullName = "",
    sourceUrl = source.orEmpty(),
    sourceCommit = "",
    // Store sources have no README; the description is the only text, shown in its place.
    readmeHtml = description.takeIf { it.isNotBlank() },
    readmeFailed = false,
    readmeBaseUrl = "",
    releases = releases.map { it.toRepoRelease(id) },
    healthStale = false,
    webUrl = homepage.orEmpty(),
    fromCache = false,
)

fun StoreRelease.toRepoRelease(moduleId: String): RepoRelease {
    val tag = version.ifBlank { versionCode.toString() }
    return RepoRelease(
        tag = tag,
        name = version.ifBlank { tag },
        version = version,
        versionCode = versionCode.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
        publishedAt = formatEpochSeconds(timestamp).first,
        changelogHtml = changelog.takeIf { it.isNotBlank() },
        zipUrl = downloadUrl,
        zipName = downloadUrl.substringAfterLast('/').ifBlank { "$moduleId-$versionCode.zip" },
        zipSize = expectedSize ?: 0L,
        sha256 = expectedSha256.orEmpty(),
        tagCommit = "",
        releaseUrl = "",
        attestationUrl = null,
        immutable = expectedSha256 != null,
    )
}

/** Wraps a store list as a [RepoModuleList]; store sources have no build id or cache. */
fun List<StoreModule>.toRepoModuleList(): RepoModuleList = RepoModuleList(
    modules = map { it.toRepoModule() },
    buildId = "",
    stale = false,
    error = null,
)

/** `true` when this kind is served by a [ModuleStoreSource] (i.e. anything but the KSU registry). */
fun StoreSourceKind.isStoreSource(): Boolean = this != StoreSourceKind.KERNELSU

/** Formats an epoch-seconds timestamp the way Nova formats registry dates. */
private fun formatEpochSeconds(timestampSeconds: Double): Pair<String, Long> {
    if (timestampSeconds <= 0.0) return "" to 0L
    val millis = (timestampSeconds * 1000).toLong().coerceAtLeast(0L)
    val text = runCatching {
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }.getOrDefault("")
    return text to millis
}
