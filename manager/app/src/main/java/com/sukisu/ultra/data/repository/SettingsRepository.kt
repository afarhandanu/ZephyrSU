@file:Suppress("SpellCheckingInspection")

package com.sukisu.ultra.data.repository

import com.sukisu.ultra.data.model.SuiApp
import com.sukisu.ultra.data.model.SuiStatus
import com.sukisu.ultra.data.model.NomountStatus
import com.sukisu.ultra.data.modulestore.StoreSourceKind

import com.sukisu.ultra.ui.util.HomeCardSticker

interface SettingsRepository {
    var customBackgroundUri: String
    var customBackgroundUpdatedAt: Long
    
    var useSystemWallpaper: Boolean
    /**
     * Derive the theme key color from the active background image. This and [keyColor] are
     * mutually exclusive: a non-zero [keyColor] wins, so derive the effective flag instead of
     * reading this field directly.
     */
    var extractColorFromBackground: Boolean
    var cardBackgroundAlphaPercent: Int
    var customBackgroundBlurPercent: Int
    var customBackgroundScrimPercent: Int
    var backgroundAppearanceExpanded: Boolean
    /** Top stats layout on the home screen: "list" or "grid" (FinaSU wallpaper system, M5.2). */
    var statsTopLayout: String
    /** Hide the working-card mode switch on the home screen (M5.2). */
    var listWorkingCardModeHidden: Boolean
    /** Home layout style token: "circle" (Nova home) or "kernelsu" (GridUI), see HomeLayoutStyle (M5.3). */
    var homeLayoutStyle: String
    
    var backgroundSectionExpanded: Boolean
    var themePackageSectionExpanded: Boolean
    var appearanceSectionExpanded: Boolean
    var homeCardSectionExpanded: Boolean
    var colorsSectionExpanded: Boolean
    var bottomBarSectionExpanded: Boolean
    var interactionSectionExpanded: Boolean
    var detailsSectionExpanded: Boolean
    var customHomeTitle: String
    var customTexts: Map<String, String>
    /** Style token of the home screen bottom card; see [com.sukisu.ultra.data.model.SupportLinksStyle]. */
    var supportLinksStyle: String
    /** Title of the `SupportLinksStyle.CUSTOM` home card. Empty values hide the card. */
    var supportLinksCustomTitle: String
    /** Subtitle of the `SupportLinksStyle.CUSTOM` home card. Empty values hide the card. */
    var supportLinksCustomSubtitle: String
    /** URL of the `SupportLinksStyle.CUSTOM` home card. Empty values hide the card. */
    var supportLinksCustomUrl: String
    
    var themeStoreBaseUrl: String

    /** FinaSU (`.fpt`) theme store mode: "", "builtin" or "compat" — see ThemeStoreMode (M5.1). */
    var themeStoreMode: String
    
    var themeStoreToken: String
    
    var themeStoreAuthorId: String
    
    var themeStoreAuthorName: String
    
    var themeStoreAdminUrl: String
    
    var themeStoreExtraSources: List<com.sukisu.ultra.data.model.ThemeStoreSourceConfig>
    
    var themeStoreUploadSource: String
    var themeStoreGridLayout: Boolean
    var homeCardImagePath: String
    
    var hideHomeCardStatusText: Boolean
    
    var hideHomeCardDecoration: Boolean
    var homeCardStickers: List<HomeCardSticker>
    /** Custom bottom bar icon images keyed by [com.sukisu.ultra.ui.util.BottomBarIconSlot.key]. */
    var bottomBarIcons: Map<String, String>
    /** "classic" or "immersive"; see [com.sukisu.ultra.ui.util.HomeLayout]. */
    var homeLayout: String
    /** "start" or "end"; see [com.sukisu.ultra.ui.util.HomeStatusAlignment]. */
    var homeStatusAlignment: String
    /** Uniform scale for the immersive status area, coerced to the supported range. */
    var homeStatusScale: Float
    /** Independent scale for the working-status pill. */
    var homePillScale: Float
    /** Width of the immersive status cluster relative to the page content width. */
    var homeStatusWidthFraction: Float
    /** "window", "banner" or "off"; see [com.sukisu.ultra.ui.util.HomeCardImageStyle]. */
    var homeCardImageStyle: String
    /** Show the immersive card image even when no custom background is active. */
    var showHomeCardImageWithoutBackground: Boolean
    
    var adaptiveThemeJson: String
    
    var adaptiveAppliedMode: Int
    
    var adaptiveDraftJson: String
    /** Canonical JSON for the static colour/image-effect overlay; empty when disabled. */
    var customAppearanceJson: String
    var uiMode: String
    var appLanguage: String
    var checkUpdate: Boolean
    var checkCiUpdate: Boolean
    var checkModuleUpdate: Boolean
    var autoDeleteModuleZip: Boolean
    /** When enabled, a manager self-update installs without keeping the APK in Downloads. */
    var autoDeleteUpdateApk: Boolean
    
    var appIconId: String
    var appIconDynamicColor: Boolean
    var themeMode: Int
    var miuixMonet: Boolean
    var keyColor: Int
    var colorStyle: String
    var colorSpec: String
    var enablePredictiveBack: Boolean
    var enableSwipeDismiss: Boolean
    var pagerInterceptionMode: Int
    var enableBlur: Boolean
    /** Progressive (edge gradient) blur behind the top and bottom bars. */
    var enableProgressiveBlur: Boolean
    var enableFloatingBottomBar: Boolean
    var enableFloatingBottomBarBlur: Boolean
    var enableNavigationBadge: Boolean
    /** M5.4: show the SuperUser tab in the bottom bar / rail (Home and Setting always stay). */
    var showNavSuperuser: Boolean
    /** M5.4: show the Module tab in the bottom bar / rail (Home and Setting always stay). */
    var showNavModule: Boolean
    /** M5.4: hide the floating bar automatically after 3 s idle. */
    var floatingAutoHide: Boolean
    /** M5.4: hide the floating bar while scrolling down, show it while scrolling up. */
    var floatingSwipeHide: Boolean
    var navigationRailExpanded: Boolean
    var pageScale: Float
    var moduleDescriptionMaxLines: Int
    var enableWebDebugging: Boolean
    var moduleSortEnabledFirst: Boolean
    var moduleSortActionFirst: Boolean
    var moduleSortWebUiFirst: Boolean
    var moduleSortZygiskFirst: Boolean
    var moduleSortMetamoduleFirst: Boolean
    var moduleSortNameOrder: String
    var moduleSortManual: Boolean
    var moduleManualOrder: String
    
    var moduleRepoSortOrder: String
    // Module store source selection (M3.4, FinaSU multi-source): which backend feeds the
    // Module Repo screen. KERNELSU is Nova's own registry; the other kinds are served by
    // ModuleStoreSource implementations.
    var repoSourceKind: StoreSourceKind
    var repoCustomUrl: String
    var repoSelectedRepositoryUrl: String
    var superuserShowSystemApps: Boolean
    var superuserShowOnlyPrimaryUserApps: Boolean
    var superuserSortOption: Int
    var suLogFilters: Set<String>?
    var showFullStatus: Boolean
    var hideHomeWarnings: Boolean
    
    var showAiHomeCard: Boolean
    var autoJailbreak: Boolean
    var builtinZygisk: Boolean
    var useSoftReboot: Boolean
    val intentToken: String

    suspend fun getSuCompatStatus(): String
    suspend fun getSuCompatPersistValue(): Long?
    fun isSuEnabled(): Boolean
    fun setSuEnabled(enabled: Boolean): Boolean
    fun setSuCompatModePref(mode: Int)
    fun getSuCompatModePref(): Int

    suspend fun getKernelUmountStatus(): String
    fun isKernelUmountEnabled(): Boolean
    fun setKernelUmountEnabled(enabled: Boolean): Boolean

    suspend fun getSelinuxHideStatus(): String
    fun isSelinuxHideEnabled(): Boolean
    fun setSelinuxHideEnabled(enabled: Boolean): Int

    suspend fun getAvcSpoofStatus(): String
    fun isAvcSpoofEnabled(): Boolean
    fun setAvcSpoofEnabled(enabled: Boolean): Boolean

    suspend fun getMountHideStatus(): String
    fun isMountHideEnabled(): Boolean
    fun setMountHideEnabled(enabled: Boolean): Int

    suspend fun getSulogStatus(): String
    suspend fun getSulogPersistValue(): Long?
    suspend fun getSulogValue(): Long?
    fun setSulogEnabled(enabled: Boolean): Boolean

    suspend fun getAdbRootStatus(): String
    suspend fun getAdbRootPersistValue(): Long?
    suspend fun getAdbRootValue(): Long?
    fun setAdbRootEnabled(enabled: Boolean): Boolean
    suspend fun getShellSuAdbOnlyStatus(): String
    suspend fun getShellSuAdbOnlyValue(): Long?
    suspend fun getShellSuAdbOnlyPersistValue(): Long?
    fun setShellSuAdbOnlyEnabled(enabled: Boolean): Boolean

    fun isDefaultUmountModules(): Boolean
    fun setDefaultUmountModules(enabled: Boolean): Boolean

    fun isLkmMode(): Boolean

    fun execKsudFeatureSave()

    suspend fun getNomountStatus(): Result<NomountStatus>
    suspend fun setNomountIsolated(enabled: Boolean): Result<Unit>
    suspend fun injectNomountRules(): Result<Unit>
    suspend fun clearNomountRules(): Result<Unit>
    suspend fun clearNomountEverything(): Result<Unit>
    suspend fun setNomountUids(uids: List<Int>): Result<Unit>
    suspend fun hotLoadNomountModule(id: String): Result<Unit>
    suspend fun hotUnloadNomountModule(id: String): Result<Unit>

    suspend fun getSuiStatus(): Result<SuiStatus>
    suspend fun setSuiEnabled(enabled: Boolean): Result<Unit>
    suspend fun repairSui(): Result<Unit>
    suspend fun suiProbeManagement(): Result<String>
    suspend fun suiAppPermissions(): Result<List<SuiApp>>
    suspend fun suiLaunchableAppCount(): Result<Int>
    suspend fun suiDefaultMode(): Result<Int>
    suspend fun setSuiDefaultMode(mode: Int): Result<Unit>
    suspend fun suiSettingsFlags(): Result<Int>
    suspend fun setSuiSettingsFlags(flags: Int): Result<Unit>
    suspend fun suiHiddenUids(): Result<List<Int>>
    suspend fun setSuiAppMode(uid: Int, mode: Int): Result<Unit>

    suspend fun getBootHideStatus(): String
    fun setBootHideEnabled(enabled: Boolean): Boolean
    suspend fun getSELinuxMode(): String
    suspend fun setSELinuxMode(enforcing: Boolean): Boolean

    // Local backup (M4.5, ported from FinaSU). WebDAV prefs are intentionally absent —
    // WebDAV is out of scope for M4.5.
    var backupLocalEnabled: Boolean
    var backupModulesEnabled: Boolean
    var backupAllowlistEnabled: Boolean
    var backupPartitions: Set<String>
    var backupBeforeFlash: Boolean
}
