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
 * Module/Setting) — the Kpm tab occupies FinaSU's Plugin slot, so `PAGE_PLUGIN` paints
 * behind it, and the Plugin tab now paints its own natural slot. Both pages share the one
 * Plugin-slot image from the Wallpaper settings: there is a single "Plugin page background"
 * slot, and splitting KPM onto a sixth slot would need a new config slot, settings row and
 * .fpt round-trip, so sharing stays the least surprising option.
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
 * Nova's main-pager slot to the FinaSU wallpaper page it paints. Pager 1 is Kpm, which
 * occupies FinaSU's Plugin slot (PAGE_PLUGIN) — the KPM tab paints the Plugin wallpaper —
 * and pager 2 is the Plugin tab itself, which paints the same slot. Both pages share the
 * one Plugin-slot image; see the file header for why KPM was not moved to a new slot.
 */
internal fun mainPageToWallpaperPage(page: Int): Int = when (page) {
    1 -> WallpaperConfig.PAGE_PLUGIN
    2 -> WallpaperConfig.PAGE_PLUGIN
    3 -> WallpaperConfig.PAGE_SUPERUSER
    4 -> WallpaperConfig.PAGE_MODULE
    5 -> WallpaperConfig.PAGE_SETTINGS
    else -> WallpaperConfig.PAGE_HOME
}
