package com.sukisu.ultra.ui.screen.settings.search

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.LabelOff
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import com.sukisu.ultra.R
import com.sukisu.ultra.data.model.SupportLinksStyle
import com.sukisu.ultra.data.modulebanner.ModuleBannerConfig
import com.sukisu.ultra.data.security.SecurityTarget
import com.sukisu.ultra.ui.navigation3.Route
import com.sukisu.ultra.ui.screen.settings.SettingsSection
import com.sukisu.ultra.ui.screen.settings.SettingsUiState

/**
 * Settings search index for Nova, written for ZephyrSU (Milestone 3.1).
 *
 * FinaSU's index (`ui/screen/settings/search/SettingsSearchIndex.kt`) could NOT be copied:
 * it targets FinaSU's hub categories, and the hub was not ported (duplicate — see
 * MERGE_PLAN.md §3.9). This index is built from Nova's actual settings page instead:
 * the single scrollable page with sections (GENERAL, PROFILE_TEMPLATES, EXTENSIONS,
 * TOOLS, TEMPORARY_ROOT, SECURITY, MODULE_BEHAVIOUR, UNINSTALL, DIAGNOSTICS) in both
 * Material and Miuix variants, plus the dedicated sub-screens. It covers every row on
 * the settings page, the key rows of the ColorPalette / Multimedia / Authentication
 * sub-screens, and every feature added by earlier ports (App Lock, module store
 * sources live in Module Repo, module banner settings, Geometry icon, custom support
 * links, FirstRunGuide replay in About).
 *
 * The matcher and results UI are FinaSU's (`SettingsSearchMatch.kt`,
 * `SettingsSearchResults.kt`); only the index and the navigation targets are Nova's.
 *
 * Notes on the titles: rows whose title is a custom text (`customText(...)`) are
 * indexed by their default string (`CustomTextKey.defaultRes`) — the search matches
 * the default name even if the user renamed the row. The matcher itself is
 * locale-aware (folding), so Indonesian aliases are included where the English
 * word differs a lot from everyday Indonesian use.
 */

/** Where a search result lands when its row is tapped. */
sealed interface SettingsSearchTarget {
    /** A row on the settings page: scroll the page to this anchor's card. */
    data class Section(val anchor: String) : SettingsSearchTarget

    /** A page of its own, opened directly. */
    data class Page(val route: Route) : SettingsSearchTarget

    /** An action the settings page itself owns: a dialog or a sheet. */
    data class Hub(val action: SettingsHubAction) : SettingsSearchTarget
}

/** The rows the settings page itself owns, which have no page of their own to push. */
enum class SettingsHubAction { SEND_LOG, UNINSTALL }

/**
 * One searchable settings row.
 *
 * The index is the single source of truth for what the search can find, so a row added to the
 * settings page has to be registered here too. [titleRes] and [summaryRes] point at the very
 * strings the settings screen draws, [icon] is the same icon, and [available] mirrors the
 * visibility condition the screen applies, so the search never offers a row the device cannot show.
 */
data class SettingsSearchEntry(
    val id: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val target: SettingsSearchTarget,
    val groupKey: String,
    @StringRes val groupTitleRes: Int,
    @StringRes val summaryRes: Int = 0,
    val aliases: List<String> = emptyList(),
    val available: (SettingsUiState) -> Boolean = { true },
)

/** Group keys for rows that live on a sub-screen rather than the settings page itself. */
private const val GROUP_THEME = "theme"
private const val GROUP_AUTHENTICATION = "authentication"
private const val GROUP_MULTIMEDIA = "multimedia"

/** The order result groups are drawn in: the settings page's own order, then sub-screens. */
val settingsSearchGroupOrder: List<String> = listOf(
    SettingsSection.TOP,
    SettingsSection.GENERAL,
    SettingsSection.NAVIGATION,
    SettingsSection.PROFILE_TEMPLATES,
    SettingsSection.EXTENSIONS,
    SettingsSection.TOOLS,
    SettingsSection.TEMPORARY_ROOT,
    SettingsSection.SECURITY,
    SettingsSection.MODULE_BEHAVIOUR,
    SettingsSection.BACKUP,
    SettingsSection.UNINSTALL,
    SettingsSection.DIAGNOSTICS,
    GROUP_THEME,
    GROUP_AUTHENTICATION,
    GROUP_MULTIMEDIA,
)

/** One section of the settings page, for the shortcuts shown before anything is typed. */
data class SettingsSearchSectionMeta(
    val key: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
)

/** The settings page's sections, in page order, for the search shortcuts. */
val settingsSearchSections: List<SettingsSearchSectionMeta> = listOf(
    SettingsSearchSectionMeta(
        SettingsSection.GENERAL,
        R.string.settings_search_section_general,
        Icons.Filled.Settings,
    ),
    SettingsSearchSectionMeta(
        SettingsSection.NAVIGATION,
        R.string.settings_search_section_navigation,
        Icons.Filled.Navigation,
    ),
    SettingsSearchSectionMeta(
        SettingsSection.PROFILE_TEMPLATES,
        R.string.settings_search_section_profile_templates,
        Icons.Filled.Description,
    ),
    SettingsSearchSectionMeta(
        SettingsSection.EXTENSIONS,
        R.string.settings_search_section_extensions,
        Icons.Filled.Memory,
    ),
    SettingsSearchSectionMeta(
        SettingsSection.TOOLS,
        R.string.settings_search_section_tools,
        Icons.Filled.Fence,
    ),
    SettingsSearchSectionMeta(
        SettingsSection.TEMPORARY_ROOT,
        R.string.settings_search_section_temporary_root,
        Icons.Filled.FlashOn,
    ),
    SettingsSearchSectionMeta(
        SettingsSection.SECURITY,
        R.string.settings_search_section_security,
        Icons.Filled.Security,
    ),
    SettingsSearchSectionMeta(
        SettingsSection.MODULE_BEHAVIOUR,
        R.string.settings_search_section_module_behaviour,
        Icons.Filled.Extension,
    ),
    SettingsSearchSectionMeta(
        SettingsSection.BACKUP,
        R.string.settings_search_section_backup,
        Icons.Filled.Backup,
    ),
    SettingsSearchSectionMeta(
        SettingsSection.UNINSTALL,
        R.string.settings_search_section_uninstall,
        Icons.Filled.Delete,
    ),
    SettingsSearchSectionMeta(
        SettingsSection.DIAGNOSTICS,
        R.string.settings_search_section_diagnostics,
        Icons.Filled.BugReport,
    ),
)

/** A row on the settings page: tapping it scrolls the page to [anchor]'s card. */
private fun sectionRow(
    groupKey: String,
    @StringRes groupTitleRes: Int,
    id: String,
    @StringRes titleRes: Int,
    icon: ImageVector,
    anchor: String,
    @StringRes summaryRes: Int = 0,
    aliases: List<String> = emptyList(),
    available: (SettingsUiState) -> Boolean = { true },
) = SettingsSearchEntry(
    id = id,
    titleRes = titleRes,
    icon = icon,
    target = SettingsSearchTarget.Section(anchor),
    groupKey = groupKey,
    groupTitleRes = groupTitleRes,
    summaryRes = summaryRes,
    aliases = aliases,
    available = available,
)

/** A row that opens a page of its own, filed under the group that links to it. */
private fun pageRow(
    groupKey: String,
    @StringRes groupTitleRes: Int,
    id: String,
    @StringRes titleRes: Int,
    icon: ImageVector,
    route: Route,
    @StringRes summaryRes: Int = 0,
    aliases: List<String> = emptyList(),
    available: (SettingsUiState) -> Boolean = { true },
) = SettingsSearchEntry(
    id = id,
    titleRes = titleRes,
    icon = icon,
    target = SettingsSearchTarget.Page(route),
    groupKey = groupKey,
    groupTitleRes = groupTitleRes,
    summaryRes = summaryRes,
    aliases = aliases,
    available = available,
)

/** A row the settings page draws itself, so it is filed under its own section. */
private fun hubRow(
    groupKey: String,
    @StringRes groupTitleRes: Int,
    id: String,
    @StringRes titleRes: Int,
    icon: ImageVector,
    action: SettingsHubAction,
    @StringRes summaryRes: Int = 0,
    aliases: List<String> = emptyList(),
    available: (SettingsUiState) -> Boolean = { true },
) = SettingsSearchEntry(
    id = id,
    titleRes = titleRes,
    icon = icon,
    target = SettingsSearchTarget.Hub(action),
    groupKey = groupKey,
    groupTitleRes = groupTitleRes,
    summaryRes = summaryRes,
    aliases = aliases,
    available = available,
)

/**
 * Every row the settings search can find, in the order the pages present them.
 *
 * Ordered by section so the result groups read the same way as the settings page, and within a
 * section so equally scored rows keep the page's own order.
 */
val settingsSearchIndex: List<SettingsSearchEntry> = buildList {
    // Top card (Font / Multimedia / BootHide / SELinux mode) — ZephyrSU extras, no section
    // of their own on the page, so they group under the app name and scroll to the top card.
    add(
        pageRow(
            groupKey = SettingsSection.TOP,
            groupTitleRes = R.string.app_name,
            id = "top.font",
            titleRes = R.string.font_title,
            icon = Icons.Filled.FontDownload,
            route = Route.Font,
            aliases = listOf("font", "typeface", "typography", "huruf"),
        )
    )
    add(
        pageRow(
            groupKey = SettingsSection.TOP,
            groupTitleRes = R.string.app_name,
            id = "top.multimedia",
            titleRes = R.string.media_music,
            icon = Icons.Filled.MusicNote,
            route = Route.Multimedia,
            aliases = listOf("multimedia", "music", "audio", "sound", "musik", "suara"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.TOP,
            groupTitleRes = R.string.app_name,
            id = "top.boothide",
            titleRes = R.string.settings_zephyrsu_hide,
            summaryRes = R.string.settings_zephyrsu_hide_summary,
            icon = Icons.Filled.PhonelinkLock,
            anchor = SettingsSection.TOP,
            aliases = listOf("boothide", "hide", "bootloader", "unlock", "vbmeta", "sembunyi"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.TOP,
            groupTitleRes = R.string.app_name,
            id = "top.selinux_mode",
            titleRes = R.string.settings_selinux_mode,
            icon = Icons.Filled.Shield,
            anchor = SettingsSection.TOP,
            aliases = listOf("selinux", "enforcing", "permissive", "mode"),
        )
    )

    // General
    add(
        sectionRow(
            groupKey = SettingsSection.GENERAL,
            groupTitleRes = R.string.settings_search_section_general,
            id = "general.ui_mode",
            titleRes = R.string.settings_ui_mode,
            summaryRes = R.string.settings_ui_mode_summary,
            icon = Icons.Filled.DisplaySettings,
            anchor = SettingsSection.UI_MODE,
            aliases = listOf("ui", "mode", "miuix", "material", "tampilan"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.GENERAL,
            groupTitleRes = R.string.settings_search_section_general,
            id = "general.language",
            titleRes = R.string.settings_language,
            summaryRes = R.string.settings_language_summary,
            icon = Icons.Rounded.Language,
            anchor = SettingsSection.LANGUAGE,
            aliases = listOf("language", "locale", "english", "bahasa"),
        )
    )
    add(
        pageRow(
            groupKey = SettingsSection.GENERAL,
            groupTitleRes = R.string.settings_search_section_general,
            id = "general.theme",
            titleRes = R.string.settings_theme,
            summaryRes = R.string.settings_theme_summary,
            icon = Icons.Filled.Palette,
            route = Route.ColorPalette(),
            aliases = listOf("theme", "color", "colour", "palette", "monet", "dynamic color", "amoled", "dark", "tema", "warna"),
        )
    )
    add(
        pageRow(
            groupKey = SettingsSection.GENERAL,
            groupTitleRes = R.string.settings_search_section_general,
            id = "general.theme_store",
            titleRes = R.string.theme_store_title,
            summaryRes = R.string.theme_store_summary,
            icon = Icons.Filled.Storefront,
            route = Route.ThemeStore(),
            aliases = listOf("theme", "store", "fpt", "import", "toko"),
        )
    )
    add(
        pageRow(
            groupKey = SettingsSection.GENERAL,
            groupTitleRes = R.string.settings_search_section_general,
            id = "general.fpt_theme_store",
            titleRes = R.string.fpt_theme_store_title,
            summaryRes = R.string.fpt_theme_store_summary,
            icon = Icons.Filled.Storefront,
            route = Route.FptThemeStore,
            aliases = listOf("fpt", "theme", "store", "toko", "finasu", "fpt theme"),
        )
    )

    add(
        pageRow(
            groupKey = SettingsSection.GENERAL,
            groupTitleRes = R.string.settings_search_section_general,
            id = "general.wallpaper",
            titleRes = R.string.wallpaper_title,
            summaryRes = R.string.settings_wallpaper_summary,
            icon = Icons.Filled.Wallpaper,
            route = Route.Wallpaper,
            aliases = listOf("wallpaper", "background", "video", "wallpaper latar"),
        )
    )
    add(
        pageRow(
            groupKey = SettingsSection.GENERAL,
            groupTitleRes = R.string.settings_search_section_general,
            id = "general.wallpaper_gallery",
            titleRes = R.string.settings_wallpaper_gallery,
            summaryRes = R.string.settings_wallpaper_gallery_summary,
            icon = Icons.Filled.Collections,
            route = Route.WallpaperGallery,
            aliases = listOf("wallpaper", "gallery", "photo", "galeri"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.GENERAL,
            groupTitleRes = R.string.settings_search_section_general,
            id = "general.home_layout",
            titleRes = R.string.settings_home_layout,
            summaryRes = R.string.settings_home_layout_summary,
            icon = Icons.Filled.GridView,
            anchor = SettingsSection.GENERAL,
            aliases = listOf("home", "layout", "grid", "gridui", "circle", "tampilan", "beranda"),
        )
    )

    // Navigation (M5.4): bottom-bar destination visibility + floating-bar hide behaviour.
    add(
        sectionRow(
            groupKey = SettingsSection.NAVIGATION,
            groupTitleRes = R.string.settings_search_section_navigation,
            id = "navigation.show_superuser",
            titleRes = R.string.settings_nav_show_superuser,
            summaryRes = R.string.settings_nav_show_superuser_summary,
            icon = Icons.Filled.Shield,
            anchor = SettingsSection.NAVIGATION,
            aliases = listOf("superuser", "tab", "show", "hide", "sembunyi", "tampilkan"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.NAVIGATION,
            groupTitleRes = R.string.settings_search_section_navigation,
            id = "navigation.show_module",
            titleRes = R.string.settings_nav_show_module,
            summaryRes = R.string.settings_nav_show_module_summary,
            icon = Icons.Filled.Extension,
            anchor = SettingsSection.NAVIGATION,
            aliases = listOf("module", "tab", "show", "hide", "sembunyi", "tampilkan"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.NAVIGATION,
            groupTitleRes = R.string.settings_search_section_navigation,
            id = "navigation.floating_auto_hide",
            titleRes = R.string.settings_floating_auto_hide,
            summaryRes = R.string.settings_floating_auto_hide_summary,
            icon = Icons.Filled.Timer,
            anchor = SettingsSection.NAVIGATION,
            aliases = listOf("floating", "auto", "hide", "bar", "sembunyi", "otomatis"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.NAVIGATION,
            groupTitleRes = R.string.settings_search_section_navigation,
            id = "navigation.floating_swipe_hide",
            titleRes = R.string.settings_floating_swipe_hide,
            summaryRes = R.string.settings_floating_swipe_hide_summary,
            icon = Icons.Filled.SwapVert,
            anchor = SettingsSection.NAVIGATION,
            aliases = listOf("floating", "scroll", "hide", "bar", "sembunyi", "gulir"),
        )
    )

    // Profile templates
    add(
        pageRow(
            groupKey = SettingsSection.PROFILE_TEMPLATES,
            groupTitleRes = R.string.settings_search_section_profile_templates,
            id = "profile_templates.template",
            titleRes = R.string.settings_profile_template,
            summaryRes = R.string.settings_profile_template_summary,
            icon = Icons.Filled.Description,
            route = Route.AppProfileTemplate,
            aliases = listOf("profile", "template", "per app", "profil"),
        )
    )

    // Extensions
    add(
        pageRow(
            groupKey = SettingsSection.EXTENSIONS,
            groupTitleRes = R.string.settings_search_section_extensions,
            id = "extensions.extensions",
            titleRes = R.string.extension_section_title,
            summaryRes = R.string.extension_section_summary,
            icon = Icons.Filled.Memory,
            route = Route.Extensions,
            aliases = listOf("extensions", "extension", "ekstensi"),
        )
    )

    // Tools
    add(
        pageRow(
            groupKey = SettingsSection.TOOLS,
            groupTitleRes = R.string.settings_search_section_tools,
            id = "tools.tools",
            titleRes = R.string.tools,
            summaryRes = R.string.settings_tools_summary,
            icon = Icons.Filled.Fence,
            route = Route.Tool,
            aliases = listOf("tools", "tool", "perkakas", "alat"),
        )
    )

    // UTS View (kernel identity spoofing), reached from the Tools page
    add(
        pageRow(
            groupKey = SettingsSection.TOOLS,
            groupTitleRes = R.string.settings_search_section_tools,
            id = "tools.uts_view",
            titleRes = R.string.settings_uts_view,
            summaryRes = R.string.settings_uts_view_summary,
            icon = Icons.Rounded.TextFields,
            route = Route.UtsView,
            aliases = listOf("uts", "uts view", "kernel identity", "uname", "spoof", "identitas kernel"),
        )
    )

    // NoMount (VFS path redirection), reached from the Tools page
    add(
        pageRow(
            groupKey = SettingsSection.TOOLS,
            groupTitleRes = R.string.settings_search_section_tools,
            id = "tools.nomount",
            titleRes = R.string.settings_nomount,
            summaryRes = R.string.settings_nomount_summary,
            icon = Icons.Filled.Layers,
            route = Route.Nomount,
            aliases = listOf("nomount", "no mount", "exclusion", "injection", "redirection", "pengalihan"),
        )
    )

    // Plugin system (Lua), reached from the Tools page
    add(
        pageRow(
            groupKey = SettingsSection.TOOLS,
            groupTitleRes = R.string.settings_search_section_tools,
            id = "tools.plugin",
            titleRes = R.string.plugin,
            summaryRes = R.string.settings_plugin_summary,
            icon = Icons.Outlined.Widgets,
            route = Route.Plugin,
            aliases = listOf("plugin", "lua", "plugins", "skrip"),
        )
    )

    // Kernel module registry (.ko install/autoload), reached from the Tools page
    add(
        pageRow(
            groupKey = SettingsSection.TOOLS,
            groupTitleRes = R.string.settings_search_section_tools,
            id = "tools.kmod_registry",
            titleRes = R.string.kernel_modules,
            summaryRes = R.string.kernel_modules_entry_hint,
            icon = Icons.Outlined.Memory,
            route = Route.KmodRegistry,
            aliases = listOf("kmod", "kernel module", "ko", "lkm", "autoload", "modul kernel"),
        )
    )

    // Network isolation (per-app), reached from the Tools page
    add(
        pageRow(
            groupKey = SettingsSection.TOOLS,
            groupTitleRes = R.string.settings_search_section_tools,
            id = "tools.netisolate",
            titleRes = R.string.settings_netisolate,
            summaryRes = R.string.settings_netisolate_summary,
            icon = Icons.Outlined.WifiOff,
            route = Route.NetIsolate,
            aliases = listOf("netisolate", "network", "isolate", "offline", "wifi", "jaringan", "internet"),
        )
    )

    // Temporary root
    add(
        sectionRow(
            groupKey = SettingsSection.TEMPORARY_ROOT,
            groupTitleRes = R.string.settings_search_section_temporary_root,
            id = "temporary_root.duration",
            titleRes = R.string.temporary_root_duration_title,
            icon = Icons.Filled.FlashOn,
            anchor = SettingsSection.TEMPORARY_ROOT,
            aliases = listOf("temporary root", "duration", "minutes", "timeout", "root sementara", "durasi"),
        )
    )

    // Security
    add(
        sectionRow(
            groupKey = SettingsSection.SECURITY,
            groupTitleRes = R.string.settings_search_section_security,
            id = "security.sucompat",
            titleRes = R.string.settings_sucompat,
            summaryRes = R.string.settings_sucompat_summary,
            icon = Icons.Filled.AdminPanelSettings,
            anchor = SettingsSection.SU_COMPAT,
            aliases = listOf("su", "compat", "compatibility", "legacy"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.SECURITY,
            groupTitleRes = R.string.settings_search_section_security,
            id = "security.kernel_umount",
            titleRes = R.string.settings_kernel_umount,
            summaryRes = R.string.settings_kernel_umount_summary,
            icon = Icons.Filled.LayersClear,
            anchor = SettingsSection.KERNEL_UMOUNT,
            aliases = listOf("umount", "unmount", "module", "hide", "master switch"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.SECURITY,
            groupTitleRes = R.string.settings_search_section_security,
            id = "security.selinux_hide",
            titleRes = R.string.settings_selinux_hide,
            summaryRes = R.string.settings_selinux_hide_summary,
            icon = Icons.Filled.Security,
            anchor = SettingsSection.SELINUX_HIDE,
            aliases = listOf("selinux", "hide", "spoof", "sembunyi"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.SECURITY,
            groupTitleRes = R.string.settings_search_section_security,
            id = "security.avc_spoof",
            titleRes = R.string.settings_avc_spoof,
            summaryRes = R.string.settings_avc_spoof_summary,
            icon = Icons.Filled.Security,
            anchor = SettingsSection.AVC_SPOOF,
            aliases = listOf("avc", "selinux", "spoof", "denial"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.SECURITY,
            groupTitleRes = R.string.settings_search_section_security,
            id = "security.mount_hide",
            titleRes = R.string.settings_mount_hide,
            summaryRes = R.string.settings_mount_hide_summary,
            icon = Icons.Filled.Policy,
            anchor = SettingsSection.MOUNT_HIDE,
            aliases = listOf("mount", "hide", "sembunyi"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.SECURITY,
            groupTitleRes = R.string.settings_search_section_security,
            id = "security.sulog",
            titleRes = R.string.settings_sulog,
            summaryRes = R.string.settings_sulog_summary,
            icon = Icons.AutoMirrored.Filled.Article,
            anchor = SettingsSection.SULOG,
            aliases = listOf("su", "log", "logging", "history", "riwayat"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.SECURITY,
            groupTitleRes = R.string.settings_search_section_security,
            id = "security.adb_root",
            titleRes = R.string.settings_adb_root,
            summaryRes = R.string.settings_adb_root_summary,
            icon = Icons.Filled.Adb,
            anchor = SettingsSection.ADB_ROOT,
            aliases = listOf("adb", "root", "debug"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.SECURITY,
            groupTitleRes = R.string.settings_search_section_security,
            id = "security.shell_su_adb_only",
            titleRes = R.string.settings_shell_su_adb_only,
            summaryRes = R.string.settings_shell_su_adb_only_summary,
            icon = Icons.Filled.VerifiedUser,
            anchor = SettingsSection.SHELL_SU_ADB_ONLY,
            aliases = listOf("shell", "su", "adb", "terminal"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.SECURITY,
            groupTitleRes = R.string.settings_search_section_security,
            id = "security.soft_reboot",
            titleRes = R.string.settings_soft_reboot,
            summaryRes = R.string.settings_soft_reboot_summary,
            icon = Icons.Filled.RestartAlt,
            anchor = SettingsSection.SOFT_REBOOT,
            aliases = listOf("reboot", "soft", "restart", "mulai ulang"),
        )
    )
    add(
        pageRow(
            groupKey = SettingsSection.SECURITY,
            groupTitleRes = R.string.settings_search_section_security,
            id = "security.authentication",
            titleRes = R.string.settings_category_authentication,
            summaryRes = R.string.security_default_method_summary,
            icon = Icons.Filled.Security,
            route = Route.Authentication,
            aliases = listOf("authentication", "app lock", "lock", "kunci", "pin", "pattern", "fingerprint", "biometric"),
        )
    )

    // Module behaviour
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.default_umount_modules",
            titleRes = R.string.settings_umount_modules_default,
            summaryRes = R.string.settings_umount_modules_default_summary,
            icon = Icons.AutoMirrored.Filled.Rule,
            anchor = SettingsSection.DEFAULT_UMOUNT_MODULES,
            aliases = listOf("umount", "unmount", "default", "app"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.auto_delete_module_zip",
            titleRes = R.string.settings_auto_delete_module_zip,
            summaryRes = R.string.settings_auto_delete_module_zip_summary,
            icon = Icons.Filled.Delete,
            anchor = SettingsSection.AUTO_DELETE_MODULE_ZIP,
            aliases = listOf("delete", "zip", "module", "auto", "hapus"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.auto_delete_update_apk",
            titleRes = R.string.settings_auto_delete_update_apk,
            summaryRes = R.string.settings_auto_delete_update_apk_summary,
            icon = Icons.Filled.Delete,
            anchor = SettingsSection.AUTO_DELETE_UPDATE_APK,
            aliases = listOf("delete", "apk", "update", "auto", "hapus"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.web_debugging",
            titleRes = R.string.enable_web_debugging,
            summaryRes = R.string.enable_web_debugging_summary,
            icon = Icons.Filled.AdminPanelSettings,
            anchor = SettingsSection.WEB_DEBUGGING,
            aliases = listOf("webui", "debug", "webview", "devtools"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.auto_jailbreak",
            titleRes = R.string.settings_auto_jailbreak,
            summaryRes = R.string.settings_auto_jailbreak_summary,
            icon = Icons.Filled.FlashOn,
            anchor = SettingsSection.AUTO_JAILBREAK,
            aliases = listOf("jailbreak", "auto", "late load"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.builtin_zygisk",
            titleRes = R.string.settings_builtin_zygisk,
            summaryRes = R.string.settings_builtin_zygisk_summary,
            icon = Icons.Filled.Extension,
            anchor = SettingsSection.MODULE_BEHAVIOUR,
            aliases = listOf("zygisk", "builtin", "injection"),
        )
    )
    add(
        pageRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.builtin_zygisk_dashboard",
            titleRes = R.string.settings_builtin_zygisk_dashboard,
            summaryRes = R.string.settings_builtin_zygisk_dashboard_summary,
            icon = Icons.Filled.Settings,
            route = Route.BuiltinZygisk,
            aliases = listOf("zygisk", "dashboard", "status"),
            available = { it.builtinZygisk },
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.sui",
            titleRes = R.string.sui_title,
            icon = Icons.Filled.WaterDrop,
            anchor = SettingsSection.MODULE_BEHAVIOUR,
            aliases = listOf("sui", "shizuku", "adbd"),
        )
    )
    // Folk Mount (built-in module mounting), in the Module behaviour section
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.folk_mount",
            titleRes = R.string.settings_folk_mount,
            summaryRes = R.string.settings_folk_mount_summary,
            icon = Icons.Filled.Layers,
            anchor = SettingsSection.MODULE_BEHAVIOUR,
            aliases = listOf("mount", "metamodule", "overlay", "builtin", "nomount", "folk mount"),
        )
    )
    add(
        pageRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.sui_manage",
            titleRes = R.string.sui_manage,
            summaryRes = R.string.sui_manage_summary,
            icon = Icons.AutoMirrored.Filled.OpenInNew,
            route = Route.SuiPermissions,
            aliases = listOf("sui", "permission", "authorize", "izin"),
            available = { it.suiStatus?.installed == true },
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.sui_repair",
            titleRes = R.string.sui_repair,
            summaryRes = R.string.sui_repair_summary,
            icon = Icons.Filled.Build,
            anchor = SettingsSection.MODULE_BEHAVIOUR,
            aliases = listOf("sui", "repair", "fix", "perbaiki"),
            available = {
                val status = it.suiStatus
                status != null && status.needsDiagnostics && (!status.installed || !status.payloadCurrent)
            },
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.module_banner_enabled",
            titleRes = R.string.module_banner_enabled,
            summaryRes = R.string.module_banner_enabled_summary,
            icon = Icons.Filled.Wallpaper,
            anchor = SettingsSection.MODULE_BEHAVIOUR,
            aliases = listOf("module", "banner", "cover", "image", "spanduk"),
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.module_banner_folk",
            titleRes = R.string.module_banner_folk,
            summaryRes = R.string.module_banner_folk_summary,
            icon = Icons.Filled.Wallpaper,
            anchor = SettingsSection.MODULE_BEHAVIOUR,
            aliases = listOf("module", "banner", "folk"),
            available = { ModuleBannerConfig.enabled },
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.module_banner_api_mode",
            titleRes = R.string.module_banner_api_mode,
            summaryRes = R.string.module_banner_api_mode_summary,
            icon = Icons.Outlined.Cloud,
            anchor = SettingsSection.MODULE_BEHAVIOUR,
            aliases = listOf("module", "banner", "api", "mode", "custom"),
            available = { ModuleBannerConfig.policy.active },
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.module_banner_api_source",
            titleRes = R.string.module_banner_api_source,
            icon = Icons.Outlined.Cloud,
            anchor = SettingsSection.MODULE_BEHAVIOUR,
            aliases = listOf("module", "banner", "api", "source", "url"),
            available = { ModuleBannerConfig.policy.apiActive },
        )
    )
    add(
        pageRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.module_banner_marketplace",
            titleRes = R.string.module_banner_marketplace,
            summaryRes = R.string.module_banner_marketplace_summary,
            icon = Icons.Outlined.Cloud,
            route = Route.ApiMarketplace,
            aliases = listOf("module", "banner", "marketplace", "store", "toko"),
            available = { ModuleBannerConfig.policy.apiActive },
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.module_banner_clear_cache",
            titleRes = R.string.module_banner_clear_cache,
            icon = Icons.Outlined.Cloud,
            anchor = SettingsSection.MODULE_BEHAVIOUR,
            aliases = listOf("module", "banner", "cache", "clear", "hapus"),
            available = { ModuleBannerConfig.policy.apiActive },
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.module_banner_custom_opacity",
            titleRes = R.string.module_banner_custom_opacity,
            summaryRes = R.string.module_banner_custom_opacity_summary,
            icon = Icons.Filled.Wallpaper,
            anchor = SettingsSection.MODULE_BEHAVIOUR,
            aliases = listOf("module", "banner", "opacity", "custom", "transparan"),
            available = { ModuleBannerConfig.enabled },
        )
    )
    add(
        sectionRow(
            groupKey = SettingsSection.MODULE_BEHAVIOUR,
            groupTitleRes = R.string.settings_search_section_module_behaviour,
            id = "module_behaviour.module_banner_opacity",
            titleRes = R.string.module_banner_opacity,
            icon = Icons.Filled.Wallpaper,
            anchor = SettingsSection.MODULE_BEHAVIOUR,
            aliases = listOf("module", "banner", "opacity", "transparan"),
            available = { ModuleBannerConfig.enabled && ModuleBannerConfig.customOpacityEnabled },
        )
    )

    // Backup (local backup, ported from FinaSU, Milestone 4.5)
    add(
        sectionRow(
            groupKey = SettingsSection.BACKUP,
            groupTitleRes = R.string.settings_search_section_backup,
            id = "backup.backup",
            titleRes = R.string.settings_category_backup,
            summaryRes = R.string.backup_local_summary,
            icon = Icons.Filled.Backup,
            anchor = SettingsSection.BACKUP,
            aliases = listOf("backup", "restore", "cadangan", "pulihkan"),
        )
    )
    add(
        pageRow(
            groupKey = SettingsSection.BACKUP,
            groupTitleRes = R.string.settings_search_section_backup,
            id = "backup.backup_list",
            titleRes = R.string.backup_restore_title,
            icon = Icons.Filled.Backup,
            route = Route.BackupList,
            aliases = listOf("backup", "restore", "list", "cadangan", "daftar"),
        )
    )

    // Uninstall
    add(
        hubRow(
            groupKey = SettingsSection.UNINSTALL,
            groupTitleRes = R.string.settings_search_section_uninstall,
            id = "uninstall.uninstall",
            titleRes = R.string.settings_uninstall,
            icon = Icons.Filled.Delete,
            action = SettingsHubAction.UNINSTALL,
            aliases = listOf("uninstall", "remove", "hapus", "copot"),
            available = { it.isLkmMode },
        )
    )

    // Diagnostics
    add(
        hubRow(
            groupKey = SettingsSection.DIAGNOSTICS,
            groupTitleRes = R.string.settings_search_section_diagnostics,
            id = "diagnostics.send_log",
            titleRes = R.string.send_log,
            icon = Icons.Filled.BugReport,
            action = SettingsHubAction.SEND_LOG,
            aliases = listOf("log", "send", "debug", "report", "kirim"),
        )
    )
    add(
        pageRow(
            groupKey = SettingsSection.DIAGNOSTICS,
            groupTitleRes = R.string.settings_search_section_diagnostics,
            id = "diagnostics.about",
            titleRes = R.string.about,
            icon = Icons.Filled.Info,
            route = Route.About,
            aliases = listOf("about", "version", "credits", "license", "tentang", "versi"),
        )
    )

    // Theme (ColorPalette sub-screen) — rows live on the theme page, which the
    // General section links to, so they group under the theme title.
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.color_style",
            titleRes = R.string.settings_color_style,
            icon = Icons.Rounded.Style,
            route = Route.ColorPalette(),
            aliases = listOf("color", "colour", "style", "palette", "tonal", "warna", "gaya"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.color_spec",
            titleRes = R.string.settings_color_spec,
            icon = Icons.Rounded.DesignServices,
            route = Route.ColorPalette(),
            aliases = listOf("color", "spec", "version", "2021", "2025", "warna"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.appearance_overlay",
            titleRes = R.string.appearance_overlay_title,
            summaryRes = R.string.appearance_overlay_summary,
            icon = Icons.Filled.Palette,
            route = Route.ColorPalette(),
            aliases = listOf("appearance", "overlay", "color", "image effect", "warna", "tampilan"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.background_appearance",
            titleRes = R.string.settings_background_appearance,
            summaryRes = R.string.settings_background_appearance_summary,
            icon = Icons.Filled.Wallpaper,
            route = Route.ColorPalette(),
            aliases = listOf("background", "wallpaper", "image", "blur", "scrim", "latar"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.app_icon",
            titleRes = R.string.app_icon_title,
            summaryRes = R.string.app_icon_summary,
            icon = Icons.Filled.Apps,
            route = Route.AppIcon,
            aliases = listOf("app icon", "launcher", "icon", "logo", "geometry", "ikon"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.custom_texts",
            titleRes = R.string.settings_custom_texts,
            summaryRes = R.string.settings_custom_texts_summary,
            icon = Icons.Rounded.Edit,
            route = Route.CustomTexts,
            aliases = listOf("custom text", "text", "rename", "label", "teks"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.show_fullstatus",
            titleRes = R.string.settings_show_fullstatus,
            summaryRes = R.string.settings_show_fullstatus_summary,
            icon = Icons.Filled.Visibility,
            route = Route.ColorPalette(),
            aliases = listOf("status", "full", "show", "home", "tampil"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.home_card_image",
            titleRes = R.string.settings_home_card_image,
            icon = Icons.Rounded.Image,
            route = Route.ColorPalette(),
            aliases = listOf("home", "card", "image", "photo", "gambar", "kartu"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.hide_home_card_decoration",
            titleRes = R.string.settings_hide_home_card_decoration,
            summaryRes = R.string.settings_hide_home_card_decoration_summary,
            icon = Icons.Filled.VisibilityOff,
            route = Route.ColorPalette(),
            aliases = listOf("home", "card", "decoration", "hide", "sembunyi"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.hide_home_card_status_text",
            titleRes = R.string.settings_hide_home_card_status_text,
            summaryRes = R.string.settings_hide_home_card_status_text_summary,
            icon = Icons.AutoMirrored.Rounded.LabelOff,
            route = Route.ColorPalette(),
            aliases = listOf("home", "card", "status", "text", "hide", "sembunyi"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.home_card_image_position",
            titleRes = R.string.settings_home_card_image_position,
            icon = Icons.Rounded.SwapHoriz,
            route = Route.ColorPalette(),
            aliases = listOf("home", "card", "image", "position", "posisi"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.home_card_image_clear",
            titleRes = R.string.settings_home_card_image_clear,
            icon = Icons.Filled.Delete,
            route = Route.ColorPalette(),
            aliases = listOf("home", "card", "image", "clear", "remove", "hapus"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.show_home_card_image_without_background",
            titleRes = R.string.settings_show_home_card_image_without_background,
            summaryRes = R.string.settings_show_home_card_image_without_background_summary,
            icon = Icons.Filled.Image,
            route = Route.ColorPalette(),
            aliases = listOf("home", "card", "image", "background", "latar"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.home_status_alignment",
            titleRes = R.string.settings_home_status_alignment,
            icon = Icons.Filled.FormatAlignLeft,
            route = Route.ColorPalette(),
            aliases = listOf("home", "status", "alignment", "align", "rata"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.home_pill_scale",
            titleRes = R.string.settings_home_pill_scale,
            icon = Icons.Filled.Circle,
            route = Route.ColorPalette(),
            aliases = listOf("home", "pill", "scale", "size", "ukuran"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.home_status_scale",
            titleRes = R.string.settings_home_status_scale,
            icon = Icons.Rounded.AspectRatio,
            route = Route.ColorPalette(),
            aliases = listOf("home", "status", "scale", "size", "ukuran"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.home_status_width",
            titleRes = R.string.settings_home_status_width,
            icon = Icons.Filled.SwapHoriz,
            route = Route.ColorPalette(),
            aliases = listOf("home", "status", "width", "wide", "lebar"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.support_links",
            titleRes = R.string.settings_support_links,
            summaryRes = R.string.settings_support_links_summary,
            icon = Icons.Filled.Info,
            route = Route.ColorPalette(),
            aliases = listOf("support", "links", "card", "dukungan"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.support_links_title",
            titleRes = R.string.settings_support_links_custom_title,
            summaryRes = R.string.settings_support_links_custom_title_summary,
            icon = Icons.Rounded.TextFields,
            route = Route.ColorPalette(),
            aliases = listOf("support", "title", "custom", "judul"),
            available = { it.supportLinksStyle == SupportLinksStyle.CUSTOM },
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.support_links_subtitle",
            titleRes = R.string.settings_support_links_custom_subtitle,
            summaryRes = R.string.settings_support_links_custom_subtitle_summary,
            icon = Icons.AutoMirrored.Rounded.Notes,
            route = Route.ColorPalette(),
            aliases = listOf("support", "subtitle", "custom", "subjudul"),
            available = { it.supportLinksStyle == SupportLinksStyle.CUSTOM },
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.support_links_url",
            titleRes = R.string.settings_support_links_custom_url,
            summaryRes = R.string.settings_support_links_custom_url_summary,
            icon = Icons.Rounded.Link,
            route = Route.ColorPalette(),
            aliases = listOf("support", "url", "link", "custom"),
            available = { it.supportLinksStyle == SupportLinksStyle.CUSTOM },
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.home_card_sticker",
            titleRes = R.string.settings_home_card_sticker,
            icon = Icons.Filled.EmojiEmotions,
            route = Route.ColorPalette(),
            aliases = listOf("home", "card", "sticker", "stiker"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.home_card_sticker_rotation",
            titleRes = R.string.settings_home_card_sticker_rotation,
            icon = Icons.Filled.RotateRight,
            route = Route.ColorPalette(),
            aliases = listOf("sticker", "rotation", "rotate", "putar", "stiker"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.home_card_sticker_size",
            titleRes = R.string.settings_home_card_sticker_size,
            icon = Icons.Rounded.AspectRatio,
            route = Route.ColorPalette(),
            aliases = listOf("sticker", "size", "scale", "ukuran", "stiker"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.page_scale",
            titleRes = R.string.settings_page_scale,
            summaryRes = R.string.settings_page_scale_summary,
            icon = Icons.Rounded.AspectRatio,
            route = Route.ColorPalette(),
            aliases = listOf("scale", "density", "size", "zoom", "ukuran"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_THEME,
            groupTitleRes = R.string.settings_theme,
            id = "theme.module_description_lines",
            titleRes = R.string.settings_module_description_max_lines,
            summaryRes = R.string.settings_module_description_max_lines_summary,
            icon = Icons.Rounded.Description,
            route = Route.ColorPalette(),
            aliases = listOf("module", "description", "lines", "collapse", "deskripsi"),
        )
    )

    // Authentication (Authentication sub-screen) — detail rows live on that screen.
    add(
        pageRow(
            groupKey = GROUP_AUTHENTICATION,
            groupTitleRes = R.string.settings_category_authentication,
            id = "authentication.default_method",
            titleRes = R.string.security_default_method,
            summaryRes = R.string.security_default_method_summary,
            icon = Icons.Filled.Fingerprint,
            route = Route.Authentication,
            aliases = listOf("authentication", "biometric", "fingerprint", "method", "sidik jari"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_AUTHENTICATION,
            groupTitleRes = R.string.settings_category_authentication,
            id = "authentication.pin",
            titleRes = R.string.security_pin_set,
            summaryRes = R.string.security_pin_configured,
            icon = Icons.Filled.Password,
            route = Route.Authentication,
            aliases = listOf("pin", "password", "code", "change pin", "set pin", "sandi"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_AUTHENTICATION,
            groupTitleRes = R.string.settings_category_authentication,
            id = "authentication.pattern",
            titleRes = R.string.security_pattern_set,
            summaryRes = R.string.security_pattern_configured,
            icon = Icons.Filled.Gesture,
            route = Route.Authentication,
            aliases = listOf("pattern", "gesture", "change pattern", "set pattern", "pola"),
        )
    )
    SecurityTarget.entries.forEach { target ->
        add(
            pageRow(
                groupKey = GROUP_AUTHENTICATION,
                groupTitleRes = R.string.settings_category_authentication,
                id = "authentication.target.${target.key}",
                titleRes = target.titleRes,
                summaryRes = R.string.security_target_protected_summary,
                icon = Icons.Filled.Lock,
                route = Route.Authentication,
                aliases = listOf("authentication", "protect", target.key.replace('_', ' '), "lindungi"),
            )
        )
    }

    // Multimedia (Multimedia sub-screen) — rows live on that screen.
    add(
        pageRow(
            groupKey = GROUP_MULTIMEDIA,
            groupTitleRes = R.string.media_music,
            id = "multimedia.vibration",
            titleRes = R.string.media_vibration,
            icon = Icons.Filled.Vibration,
            route = Route.Multimedia,
            aliases = listOf("vibration", "haptic", "feedback", "getar"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_MULTIMEDIA,
            groupTitleRes = R.string.media_music,
            id = "multimedia.music",
            titleRes = R.string.media_music,
            icon = Icons.Filled.MusicNote,
            route = Route.Multimedia,
            aliases = listOf("music", "audio", "background", "musik"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_MULTIMEDIA,
            groupTitleRes = R.string.media_music,
            id = "multimedia.autoplay",
            titleRes = R.string.media_autoplay,
            icon = Icons.Filled.PlayArrow,
            route = Route.Multimedia,
            aliases = listOf("music", "autoplay", "play", "otomatis"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_MULTIMEDIA,
            groupTitleRes = R.string.media_music,
            id = "multimedia.loop",
            titleRes = R.string.media_loop,
            icon = Icons.Filled.Repeat,
            route = Route.Multimedia,
            aliases = listOf("music", "loop", "repeat", "ulang"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_MULTIMEDIA,
            groupTitleRes = R.string.media_music,
            id = "multimedia.startup_sound",
            titleRes = R.string.media_startup,
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            route = Route.Multimedia,
            aliases = listOf("startup", "boot", "sound", "effect", "bunyi"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_MULTIMEDIA,
            groupTitleRes = R.string.media_music,
            id = "multimedia.sound",
            titleRes = R.string.media_sound,
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            route = Route.Multimedia,
            aliases = listOf("sound", "effect", "click", "bunyi"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_MULTIMEDIA,
            groupTitleRes = R.string.media_music,
            id = "multimedia.sound_source",
            titleRes = R.string.media_source,
            icon = Icons.Filled.LibraryMusic,
            route = Route.Multimedia,
            aliases = listOf("sound", "source", "local", "preset", "sumber"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_MULTIMEDIA,
            groupTitleRes = R.string.media_music,
            id = "multimedia.sound_preset",
            titleRes = R.string.media_preset,
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            route = Route.Multimedia,
            aliases = listOf("sound", "preset", "tone", "nada"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_MULTIMEDIA,
            groupTitleRes = R.string.media_music,
            id = "multimedia.sound_scope",
            titleRes = R.string.media_scope,
            icon = Icons.Filled.Tune,
            route = Route.Multimedia,
            aliases = listOf("sound", "scope", "global", "navigation", "cakupan"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_MULTIMEDIA,
            groupTitleRes = R.string.media_music,
            id = "multimedia.select_file",
            titleRes = R.string.media_select_file,
            icon = Icons.Filled.FolderOpen,
            route = Route.Multimedia,
            aliases = listOf("file", "select", "import", "choose", "pilih", "berkas"),
        )
    )
    add(
        pageRow(
            groupKey = GROUP_MULTIMEDIA,
            groupTitleRes = R.string.media_music,
            id = "multimedia.clear_file",
            titleRes = R.string.media_clear,
            icon = Icons.Filled.Delete,
            route = Route.Multimedia,
            aliases = listOf("clear", "remove", "delete", "hapus"),
        )
    )
}

/**
 * Resolves the index in the current locale.
 *
 * The folded copies the matcher works on are built here, so typing only ever folds the query.
 */
@Composable
fun rememberSettingsSearchEntries(): List<ResolvedSettingsEntry> {
    val resources = LocalResources.current
    return remember(resources) {
        settingsSearchIndex.map { entry ->
            resolveEntry(
                entry = entry,
                title = resources.getString(entry.titleRes),
                summary = entry.summaryRes.takeIf { it != 0 }?.let(resources::getString).orEmpty(),
                parentTitle = resources.getString(entry.groupTitleRes),
            )
        }
    }
}
