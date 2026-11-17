package com.sukisu.ultra.wallpaper

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sukisu.ultra.media.MusicConfig
import com.sukisu.ultra.media.MusicManager
import com.sukisu.ultra.media.SoundEffectConfig
import com.sukisu.ultra.media.VisualMediaConfig
import com.sukisu.ultra.ui.theme.FontConfig
import com.sukisu.ultra.ui.util.LocaleHelper
import com.sukisu.ultra.wallpaper.surface.SurfaceRegistry
import com.sukisu.ultra.wallpaper.surface.SurfaceStore
import java.io.File

/**
 * Roll back only theme-owned files and preferences if application fails.
 *
 * M5.2 restores the wallpaper parts M5.1 had to drop: the `wallpaper`,
 * `wallpaper_surfaces` and `visual_media` preference files plus the surface file
 * stems are backed up and reloaded on rollback. Kept from M5.1: the locale handling
 * via [LocaleHelper] (Nova has no `LocaleManager` application-locales path here) and
 * the backup of Nova's custom-background + bottom-bar icon files under
 * `filesDir/backgrounds/`. Dropped from FinaSU: `BottomBarIconConfig.notifyChanged()`
 * — ZephyrSU has no such config; nav icons are plain `SettingsRepository` prefs
 * restored via the `settings` backup.
 */
internal class ThemeTransaction(private val context: Context, private val backup: File) {
    private val locales = LocaleHelper.getCurrentLanguage(context)
    private val preferenceNames = listOf("settings", "wallpaper", "wallpaper_surfaces",
        "font_settings", "music_settings", "sound_effect_settings", "visual_media", "theme_metadata")
    private val preferences = preferenceNames.associateWith {
        context.getSharedPreferences(it, Context.MODE_PRIVATE).all.toMap()
    }
    private val stems = SurfaceRegistry.themeSlots().mapNotNull { it.storageStem } + listOf(
        "wallpaper", "background", "background_home", "background_kernel", "background_superuser",
        "background_system_module", "background_settings", "work_card_background", "custom_font",
        "video_background", "title_image", "nav_icon",
    )
    private fun owned(file: File): Boolean = file.name in setOf("music", "sound_effects", "theme_passthrough") ||
        stems.any { file.name == it || file.name.startsWith("$it.") || file.name.startsWith("${it}_") }

    /** Nova background files owned by a theme: the custom background plus bottom-bar icons. */
    private val backgroundsDir = File(context.filesDir, "backgrounds")
    private fun ownedBackground(file: File): Boolean =
        file.name == "bg_image" || file.name.startsWith("bottom_bar_icon_")

    init {
        check(backup.mkdirs())
        try {
            context.filesDir.listFiles()?.filter(::owned)?.forEach { source ->
                check(source.copyRecursively(File(backup, source.name), overwrite = true))
            }
            backgroundsDir.listFiles()?.filter(::ownedBackground)?.forEach { source ->
                val targetDir = File(backup, "backgrounds").also { check(it.mkdirs() || it.isDirectory) }
                check(source.copyTo(File(targetDir, source.name), overwrite = true).exists())
            }
        } catch (error: Exception) {
            backup.deleteRecursively()
            throw error
        }
    }

    suspend fun rollback() {
        context.filesDir.listFiles()?.filter(::owned)?.forEach { check(it.deleteRecursively()) }
        backup.listFiles()?.filter { it.name != "backgrounds" }?.forEach { source ->
            check(source.copyRecursively(File(context.filesDir, source.name), overwrite = true))
        }
        File(backup, "backgrounds").listFiles()?.forEach { source ->
            backgroundsDir.listFiles()?.filter(::ownedBackground)?.forEach { check(it.delete()) }
            check(source.copyTo(File(backgroundsDir, source.name), overwrite = true).exists())
        }
        preferences.forEach { (name, values) ->
            val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit()
            with(editor) {
                clear()
                values.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Boolean -> putBoolean(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Float -> putFloat(key, value)
                        is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                    }
                }
            }
            check(editor.commit()) { "Could not restore $name" }
        }
        withContext(Dispatchers.Main) {
            LocaleHelper.setLanguage(context, locales)
            WallpaperConfig.load(context)
            SurfaceStore.load(context)
            FontConfig.load(context)
            MusicConfig.load(context)
            SoundEffectConfig.load(context)
            VisualMediaConfig.load(context)
            MusicManager.reload()
        }
    }

    fun persist() {
        preferenceNames.forEach { name ->
            check(context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().commit()) {
                "Could not persist $name"
            }
        }
    }
}
