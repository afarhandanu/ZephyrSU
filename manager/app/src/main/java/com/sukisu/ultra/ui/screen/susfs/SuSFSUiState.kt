package com.sukisu.ultra.ui.screen.susfs

import androidx.compose.runtime.Immutable
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.screen.susfs.util.AppInfo
import com.sukisu.ultra.ui.screen.susfs.util.EnabledFeature
import com.sukisu.ultra.ui.screen.susfs.util.SlotInfo

@Immutable
data class SuSFSUiState(
    val operationResults: List<com.sukisu.ultra.ui.component.ConfigResult> = emptyList(),
    val priorityUidFilterResults: List<com.sukisu.ultra.ui.component.ConfigResult> = emptyList(),
    val susPathUidFilterResults: List<com.sukisu.ultra.ui.component.ConfigResult> = emptyList(),
    val isLoading: Boolean = false,
    val runtimeInfo: SuSFSRuntimeInfo? = null,
    val isNavigatingBack: Boolean = false,

    val selectedTab: SuSFSTab = SuSFSTab.BASIC_SETTINGS,

    // 基础配置
    val unameValue: String = "",
    val buildTimeValue: String = "",
    val autoStartEnabled: Boolean = false,
    val canEnableAutoStart: Boolean = false,
    val executeInPostFsData: Boolean = false,
    val enableHideBl: Boolean = false,
    val enableLog: Boolean = false,
    val enableCleanupResidue: Boolean = false,
    val enableAvcLogSpoofing: Boolean = false,
    val hideSusMountsForAllProcs: Boolean = false,
    val cmdlineOrBootconfigPath: String = "",
    val cmdlineOrBootconfigError: String? = null,

    // 槽位信息
    val slotInfoList: List<SlotInfo> = emptyList(),
    val currentActiveSlot: String = "",
    val isLoadingSlotInfo: Boolean = false,
    val slotInfoError: String? = null,
    val showSlotInfoDialog: Boolean = false,

    // 路径配置
    val susPaths: Set<String> = emptySet(),
    val susLoopPaths: Set<String> = emptySet(),
    val susMaps: Set<String> = emptySet(),
    val priorityUidFilter: Set<String> = emptySet(),
    val priorityApps: List<AppInfo> = emptyList(),
    val isLoadingPriorityApps: Boolean = false,
    val showPriorityAppsDialog: Boolean = false,
    val susPathUidFilter: Set<String> = emptySet(),
    val susPathUidFilterApps: List<AppInfo> = emptyList(),
    val isLoadingSusPathUidFilterApps: Boolean = false,
    val showSusPathUidFilterDialog: Boolean = false,

    // Kstat 配置
    val kstatConfigs: Set<String> = emptySet(),
    val addKstatPaths: Set<String> = emptySet(),

    // 重定向规则 (M4.4, ported from FinaSU)
    val redirectRules: Set<String> = emptySet(),

    // LKM 扩展 (M4.4, ported from FinaSU)
    val hiddenModules: Set<String> = emptySet(),
    val mountPrefixes: Set<String> = emptySet(),

    // 已启用功能
    val enabledFeatures: List<EnabledFeature> = emptyList(),
    val isLoadingFeatures: Boolean = false,

    // 应用列表（用于添加路径）
    val installedApps: List<AppInfo> = emptyList(),
    val isLoadingApps: Boolean = false,

    // 对话框状态
    val showConfirmReset: Boolean = false,
    val showAddPathDialog: Boolean = false,
    val showAddLoopPathDialog: Boolean = false,
    val showAddSusMapDialog: Boolean = false,
    val showAddAppPathDialog: Boolean = false,
    val showAddKstatStaticallyDialog: Boolean = false,
    val showAddKstatDialog: Boolean = false,
    val showAddRedirectDialog: Boolean = false,
    val showAddHiddenModuleDialog: Boolean = false,
    val showAddMountPrefixDialog: Boolean = false,

    val showResetPathsDialog: Boolean = false,
    val showResetLoopPathsDialog: Boolean = false,
    val showResetSusMapsDialog: Boolean = false,
    val showResetKstatDialog: Boolean = false,
    val showResetRedirectRulesDialog: Boolean = false,
    val showResetHiddenModulesDialog: Boolean = false,
    val showResetMountPrefixesDialog: Boolean = false,

    // 编辑中项
    val editingPath: String? = null,
    val editingLoopPath: String? = null,
    val editingSusMap: String? = null,
    val editingKstatConfig: String? = null,
    val editingKstatPath: String? = null,
    val editingRedirectRule: String? = null,
    val editingHiddenModule: String? = null,
    val editingMountPrefix: String? = null,

    val error: Throwable? = null,
)

enum class SuSFSTab(val displayNameRes: Int) {
    BASIC_SETTINGS(R.string.susfs_tab_basic_settings),
    FEATURE_TOGGLES(R.string.susfs_tab_feature_toggles),
    SUS_PATHS(R.string.susfs_tab_sus_paths),
    SUS_PATH_UID_FILTER(R.string.susfs_sus_path_uid_filter),
    SUS_LOOP_PATHS(R.string.susfs_tab_sus_loop_paths),
    SUS_MAPS(R.string.susfs_tab_sus_maps),
    KSTAT_CONFIG(R.string.susfs_tab_kstat_config),
    REDIRECT_RULES(R.string.susfs_tab_redirect_rules),
    PRIORITY_UID_FILTER(R.string.susfs_priority_uid_filter),
    LKM_EXTENSIONS(R.string.susfs_tab_lkm_extensions),
    ENABLED_FEATURES(R.string.susfs_tab_enabled_features);

    companion object {
        fun fromRouteId(id: String): SuSFSTab? = when (id.trim().lowercase().replace('-', '_')) {
            "basic", "basic_settings" -> BASIC_SETTINGS
            "features", "feature_toggles" -> FEATURE_TOGGLES
            "sus_paths" -> SUS_PATHS
            "sus_path_uid", "sus_path_uid_filter" -> SUS_PATH_UID_FILTER
            "sus_loop_paths" -> SUS_LOOP_PATHS
            "sus_maps" -> SUS_MAPS
            "kstat", "kstat_config" -> KSTAT_CONFIG
            "redirect", "redirect_rules" -> REDIRECT_RULES
            "priority_uid", "priority_uid_filter" -> PRIORITY_UID_FILTER
            "lkm", "lkm_extensions" -> LKM_EXTENSIONS
            "enabled_features" -> ENABLED_FEATURES
            else -> null
        }
    }
}

data class SuSFSRuntimeInfo(
    val version: String?,
    val variant: String?,
    val implementation: String?,
)
