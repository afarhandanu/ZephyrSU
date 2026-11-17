package com.sukisu.ultra.wallpaper

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sukisu.ultra.ui.util.LocaleHelper
import com.sukisu.ultra.wallpaper.surface.SurfaceRegistry
import org.json.JSONObject
import java.io.File

/**
 * Asset registry and compatibility facade. Container, validation and transactions are separate.
 *
 * M5.2 completes the registry: the main background, video/title visuals, one asset per
 * wallpaper surface slot, and the five page backgrounds now apply alongside the font,
 * nav icons, music and sound from M5.1. Only the home layout is still missing — its
 * asset needs the `homeLayoutStyle` preference and the home layouts (M5.3); its files
 * stay in the archive and in passthrough for re-export, but it is not offered as an
 * applyable part.
 */
object FolkThemeIO {
    const val FILE_NAME = "wallpaper.fpt"
    internal val assets: List<ThemedAsset> = buildList {
        add(MainWallpaperAsset)
        add(VisualMediaThemeAsset(true))
        add(VisualMediaThemeAsset(false))
        addAll(SurfaceRegistry.themeSlots().map { SurfaceBackgroundAsset(it) })
        add(HomeBackgroundAsset)
        add(PluginBackgroundAsset)
        add(SuperuserBackgroundAsset)
        add(ModuleBackgroundAsset)
        add(SettingsBackgroundAsset)
        add(FontAsset)
    }
    internal val groups = listOf(NavIconsAsset, MusicThemeAsset, SoundThemeAsset)

    suspend fun exportBackground(context: Context, target: Uri, name: String): Boolean =
        ThemeExportService.export(context, target, ThemeMetadata.current(context).copy(name = name)).isSuccess

    suspend fun importBackground(context: Context, source: Uri): Boolean {
        val prepared = ThemeImportService.prepare(context, source).getOrNull() ?: return false
        return prepared.use { ThemeImportService.apply(context, it).isSuccess }
    }

    internal suspend fun applyAssets(
        context: Context,
        json: JSONObject,
        imported: Map<String, File>,
        selection: ThemeSelection = ThemeSelection.All,
    ): Set<ThemePart> {
        val applied = linkedSetOf<ThemePart>()
        assets.forEach { asset ->
            val part = themePartOf(asset.base)
            if (part !in selection) return@forEach
            asset.apply(context, json, ThemeResourcePolicy.select(asset.base, imported))
            applied += part
        }
        groups.forEach { group ->
            if (group.part !in selection) return@forEach
            group.apply(context, json, imported)
            applied += group.part
        }
        return applied
    }

    suspend fun resetTheme(context: Context): Boolean {
        val reset = withContext(Dispatchers.IO) {
            runCatching {
                assets.forEach { it.reset(context) }
                groups.forEach { it.reset(context) }
                ThemeSettingsMapping.reset()
                true
            }.getOrDefault(false)
        }
        // A theme may have switched the app language; restoring it can recreate the activity, so
        // it stays the final step exactly like the import path.
        if (reset) withContext(Dispatchers.Main) { LocaleHelper.setLanguage(context, "") }
        return reset
    }
}
