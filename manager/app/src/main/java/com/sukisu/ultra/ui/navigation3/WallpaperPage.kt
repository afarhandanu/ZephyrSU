package com.sukisu.ultra.ui.navigation3

import top.yukonga.miuix.kmp.nav.core.NavKey
import com.sukisu.ultra.wallpaper.WallpaperConfig

/**
 * The wallpaper page owned by the current navigation destination.
 *
 * Routes without a section of their own inherit the nearest route below them, so editors and flash
 * flows keep their parent page's wallpaper.
 *
 * Adapted from FinaSU (M5.2): Nova's main pager has six pages (Home/Kpm/Plugin/SuperUser/
 * Module/Setting) — KPM paints its own slot (`PAGE_KPM`, added as a PR #31 follow-up so KPM
 * and Plugin no longer share one image) and the Plugin tab paints its natural slot
 * (`PAGE_PLUGIN`). Each page has its own "page background" row in the Wallpaper settings.
 * It is also kept as a constant for `.fpt` import/export compatibility (a theme's plugin
 * background round-trips through the archive). FinaSU-only routes
 * (PluginStore, MyThemes, Language, WebDavBrowser, SettingsCategory)
 * are dropped; Nova-only routes (SuSFS, Tool,
 * UmountManager, AppearanceOverlay, CustomTexts, FptThemeStore, FptMyThemes) map to
 * their sections.
 */
// shortcut: unmapped routes use the visible main page; add new section routes to the map below.
internal fun wallpaperPageFor(backStack: List<NavKey>, fallback: Int): Int =
    backStack.asReversed().firstNotNullOfOrNull { it.wallpaperPage } ?: fallback

private val NavKey.wallpaperPage: Int?
    get() = when (this) {
        is Route.SuSFS,
        is Route.Install,
        -> WallpaperConfig.PAGE_HOME

        Route.Sulog,
        is Route.AppProfile,
        -> WallpaperConfig.PAGE_SUPERUSER

        Route.ModuleRepo,
        is Route.ModuleRepoDetail,
        Route.KernelModules,
        is Route.ExecuteModuleAction,
        -> WallpaperConfig.PAGE_MODULE

        Route.About,
        Route.UtsView,
        Route.Nomount,
        Route.NomountExclusions,
        Route.NetIsolate,
        Route.Tool,
        Route.UmountManager,
        Route.SuiPermissions,
        is Route.ColorPalette,
        Route.Wallpaper,
        Route.WallpaperGallery,
        Route.Font,
        is Route.ThemeStore,
        Route.FptThemeStore,
        Route.FptMyThemes,
        Route.ApiMarketplace,
        Route.BackupList,
        Route.AppearanceOverlay,
        Route.CustomTexts,
        -> WallpaperConfig.PAGE_SETTINGS

        else -> null
    }

/**
 * Nova's main-pager slot to the wallpaper page it paints. Pager 1 is Kpm, which paints its
 * own slot (PAGE_KPM), and pager 2 is the Plugin tab, which paints PAGE_PLUGIN.
 */
internal fun mainPageToWallpaperPage(page: Int): Int = when (page) {
    1 -> WallpaperConfig.PAGE_KPM
    2 -> WallpaperConfig.PAGE_PLUGIN
    3 -> WallpaperConfig.PAGE_SUPERUSER
    4 -> WallpaperConfig.PAGE_MODULE
    5 -> WallpaperConfig.PAGE_SETTINGS
    else -> WallpaperConfig.PAGE_HOME
}
