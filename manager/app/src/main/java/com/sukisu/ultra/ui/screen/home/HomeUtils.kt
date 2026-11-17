package com.sukisu.ultra.ui.screen.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.core.content.pm.PackageInfoCompat
import com.sukisu.ultra.Natives
import com.sukisu.ultra.Natives.isManager
import com.sukisu.ultra.R
import com.sukisu.ultra.data.model.FolkMountProvider
import com.sukisu.ultra.ui.util.ZygiskImplementationInfo
import com.sukisu.ultra.ui.util.getSuSFSStatus
import com.sukisu.ultra.ui.util.getSuSFSVersion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

data class ManagerVersion(
    val versionName: String,
    val versionCode: Long
)

data class SystemInfo(
    val kernelVersion: String,
    val managerVersion: String,
    val deviceModel: String,
    val kernelFullVersion: String?,
    val fingerprint: String,
    val selinuxStatus: String,
    val seccompStatus: Int,
    val zygiskImplementation: String? = null,
)

fun formatZygiskImplementation(
    info: ZygiskImplementationInfo,
    notInstalledText: String,
    disabledText: String,
    rebootRequiredText: String,
): String {
    val label = listOf(info.name, info.version)
        .filter { it.isNotBlank() }
        .joinToString(" ")
    return when {
        info.state == "running" -> label.ifBlank { notInstalledText }
        label.isBlank() -> notInstalledText
        info.state == "reboot" -> "$label ($rebootRequiredText)"
        info.state == "disabled" -> "$label ($disabledText)"
        else -> notInstalledText
    }
}

fun getHomeModuleCount(modulesJson: String): Int =
    runCatching { Json.parseToJsonElement(modulesJson).jsonArray.size }.getOrDefault(0)


fun getManagerVersion(context: Context): ManagerVersion {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)!!
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    return ManagerVersion(
        versionName = packageInfo.versionName!!,
        versionCode = versionCode
    )
}

enum class SusfsStatus {
    Idle, Supported, Unsupported, Error
}

data class SusfsInfoState(
    val status: SusfsStatus = SusfsStatus.Idle,
    val detail: String = "",
)

@Composable
fun rememberSusfsInfo(
    manualHookLabel: String,
    inlineHookLabel: String,
): SusfsInfoState {
    return remember(manualHookLabel, inlineHookLabel) {
        runCatching {
            val supported = getSuSFSStatus().equals("true", ignoreCase = true)
            if (supported) {
                val version = getSuSFSVersion().trim()
                val hookLabel = when (val type = Natives.getHookType()) {
                    "Manual" -> manualHookLabel
                    "Inline" -> inlineHookLabel
                    else -> type
                }.takeIf { it.isNotBlank() }?.let { "($it)" }.orEmpty()
                SusfsInfoState(
                    status = SusfsStatus.Supported,
                    detail = listOf(version, hookLabel)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                )
            } else {
                SusfsInfoState(
                    status = SusfsStatus.Unsupported,
                    detail = ""
                )
            }
        }.getOrElse {
            SusfsInfoState(status = SusfsStatus.Error)
        }
    }
}

@Composable
fun rememberHookTypeLabel(
    manualHookText: String,
    inlineHookText: String,
    tracepointHookText: String,
    unknownHookText: String,
): String? {
    return remember(manualHookText, inlineHookText, tracepointHookText, unknownHookText) {
        if (!isManager) return@remember null
        val rawType = runCatching { Natives.getHookType() }.getOrNull() ?: return@remember null
        when (rawType) {
            "Manual" -> manualHookText
            "Tracepoint" -> tracepointHookText
            else -> rawType.ifBlank { unknownHookText }
        }
    }
}

/** The provider that really mounted modules this boot, or null when it cannot be named. */
@Composable
internal fun homeMountValue(mount: HomeMount): String? = when (mount.provider) {
    FolkMountProvider.METAMODULE -> listOfNotNull(
        stringResource(R.string.settings_folk_mount_provider_metamodule),
        mount.metamodule?.name?.takeIf { it.isNotBlank() } ?: mount.metamoduleId,
        mount.metamodule?.version?.takeIf { it.isNotBlank() && it != "Unknown" },
    ).joinToString(" · ")

    FolkMountProvider.BUILTIN -> stringResource(R.string.home_mount_builtin_magic)
    FolkMountProvider.NOMOUNT -> stringResource(R.string.home_mount_builtin_vfs)
    FolkMountProvider.NONE -> stringResource(R.string.settings_folk_mount_mode_off)
    null -> null
}
