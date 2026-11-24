package com.sukisu.ultra.ui.screen.home

import androidx.compose.runtime.Immutable
import com.sukisu.ultra.KernelVersion
import com.sukisu.ultra.data.model.FolkMountProvider
import com.sukisu.ultra.data.model.FolkMountStatus
import com.sukisu.ultra.ui.util.HomeCardSticker
import com.sukisu.ultra.ui.util.HomeCardImageStyle
import com.sukisu.ultra.ui.util.HomeLayout
import com.sukisu.ultra.ui.util.HomeStatusAlignment
import com.sukisu.ultra.ui.util.module.LatestVersionInfo

@Immutable
data class HomeLoadState(
    val content: HomeUiState? = null,
    val isRefreshing: Boolean = true,
    val error: String? = null,
)

@Immutable
data class HomeUiState(
    val kernelVersion: KernelVersion,
    val ksuVersion: Int?,
    val managerUAPIVersion: Int,
    val kernelUAPIVersion: Int?,
    val lkmMode: Boolean?,
    val isLkmBundled: Boolean,
    val isManager: Boolean,
    val isManagerPrBuild: Boolean,
    val isKernelPrBuild: Boolean,
    val requiresNewKernel: Boolean,
    val requiresNewManager: Boolean,
    val isRootAvailable: Boolean,
    val isSafeMode: Boolean,
    val isLateLoadMode: Boolean,
    val checkUpdateEnabled: Boolean,
    val latestVersionInfo: LatestVersionInfo,
    val currentManagerVersionCode: Long,
    val systemInfo: SystemInfo,
    val checkCiUpdateEnabled: Boolean = false,
    val ciLatestVersionInfo: LatestVersionInfo = LatestVersionInfo(),
    val ciUpdateBusy: Boolean = false,
    val showFullStatus: Boolean = true,
    val hideHomeWarnings: Boolean = false,
    // Root without a ZephyrSU kernel: pages are browsable but kernel features are unavailable.
    val isPreviewMode: Boolean = false,
    val hideVersionMismatchWarning: Boolean = false,
    // Whether the kernel supports the version-spoof extension. When false the
    // home warning card is meaningless (the user cannot spoof to resolve the mismatch).
    val isVersionSpoofSupported: Boolean = true,
    // Whether the user has actually set a spoofed version value. The home warning is only
    // meaningful when this is true; without a spoof the version mismatch simply means an
    // update is needed, not that the spoof is broken.
    val isVersionSpoofed: Boolean = false,
    // Null until loaded through the root shell; kpmCount stays null when KPM is unavailable.
    val superuserCount: Int? = null,
    val moduleCount: Int? = null,
    val kpmCount: Int? = null,
    val showAiHomeCard: Boolean = false,
    val customHomeTitle: String = "",
    val homeCardImagePath: String = "",
    val hideHomeCardStatusText: Boolean = false,
    val hideHomeCardDecoration: Boolean = false,
    val homeCardStickers: List<HomeCardSticker> = emptyList(),
    val homeLayout: HomeLayout = HomeLayout.CLASSIC,
    val homeStatusAlignment: HomeStatusAlignment = HomeStatusAlignment.START,
    val homeStatusScale: Float = 1f,
    val homePillScale: Float = 1f,
    val homeStatusWidthFraction: Float = 1f,
    val homeCardImageStyle: HomeCardImageStyle = HomeCardImageStyle.BANNER,
    val showHomeCardImageWithoutBackground: Boolean = false,
    /** The provider that really mounted modules this boot; a null provider hides the row. */
    val mount: HomeMount = HomeMount(),
) {
    val isSELinuxPermissive: Boolean
        get() = systemInfo.selinuxStatus == "Permissive"

    val showGkiWarning: Boolean
        get() = ksuVersion != null && lkmMode == false

    val showLkmUpdate: Boolean
        get() = isManager &&
                lkmMode == true &&
                isLkmBundled &&
                ksuVersion?.toLong() != currentManagerVersionCode &&
                !requiresNewKernel &&
                !requiresNewManager

    // Jailbreak mode runs on locked bootloaders, so flashing a boot image would brick the device.
    val canInstallKernelUpdate: Boolean
        get() = lkmMode == true && !isLateLoadMode

    val showCustomLkmBadge: Boolean
        get() = lkmMode == true && !isLkmBundled

    /**
     * Working-mode tag on the home status card.
     *
     * LKM is a 5.10+ concept; a pre-5.10 kernel always runs the built-in
     * driver, so show "Built-in" there instead of leaving the tag blank.
     * On 5.10+ keep the original mapping: true = LKM, false = Built-in,
     * unknown (null) = no tag. With no active kernel (no root) there is
     * no mode to show either.
     */
    val workingModeLabel: String
        get() = when {
            ksuVersion == null -> ""
            !kernelVersion.isGKI() -> "Built-in"
            lkmMode == true -> "LKM"
            lkmMode == false -> "Built-in"
            else -> ""
        }

    val isVersionMismatch: Boolean
        get() = isManager && (kernelUAPIVersion != managerUAPIVersion || ksuVersion?.toUInt()?.toLong() != currentManagerVersionCode)

    val showVersionMismatchWarning: Boolean
        get() = isVersionMismatch && isVersionSpoofSupported && isVersionSpoofed &&
            !hideVersionMismatchWarning

    val showRootWarning: Boolean
        get() = ksuVersion != null && !isRootAvailable

    val showManagerPrBuildWarning: Boolean
        get() = isManager && isManagerPrBuild

    val showKernelPrBuildWarning: Boolean
        get() = isManager && !isManagerPrBuild && isKernelPrBuild

    val hasUpdate: Boolean
        get() = latestVersionInfo.versionCode > currentManagerVersionCode
}

/** The installed metamodule, named by its module entry when the module list has loaded. */
@Immutable
data class HomeMetamodule(
    val id: String,
    val name: String,
    val version: String,
)

/** The provider that actually mounted modules this boot; a null provider hides the row. */
@Immutable
data class HomeMount(
    val provider: FolkMountProvider? = null,
    val metamoduleId: String? = null,
    val metamodule: HomeMetamodule? = null,
)

internal fun resolveHomeMountProvider(status: FolkMountStatus?): FolkMountProvider? {
    status ?: return null
    if (status.bootResult == "skipped") return FolkMountProvider.NONE
    return status.bootProvider ?: status.nextProvider
}

@Immutable
data class HomeActions(
    val onInstallClick: () -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onJailbreakClick: () -> Unit = {},
    val onSuperuserClick: () -> Unit = {},
    val onModuleClick: () -> Unit = {},
    val onCiUpdateClick: () -> Unit = {},
    val onReleaseUpdateClick: () -> Unit = {},
    val onOpenVersionSpoof: () -> Unit = {},
    val onOpenAi: () -> Unit = {},
    val onOpenThemeStore: () -> Unit = {},
)
