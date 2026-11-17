package com.sukisu.ultra.wallpaper

import android.content.Context
import com.sukisu.ultra.data.repository.SettingsRepository
import com.sukisu.ultra.data.repository.SettingsRepositoryImpl
import com.sukisu.ultra.ui.theme.FontConfig
import com.sukisu.ultra.ui.theme.FontMode
import com.sukisu.ultra.ui.util.ImageStorage
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * The user's main background wallpaper and its opacity/blur/scrim settings.
 *
 * Adapted from FinaSU: Nova has no wallpaper config of its own, so the `.fpt` main
 * background maps onto Nova's custom background (`ImageStorage` + `customBackgroundUri`).
 * Blur maps to `customBackgroundBlurPercent`, dim to `customBackgroundScrimPercent`,
 * `wallpaper_use_color` to `extractColorFromBackground`. Opacity, dual/day/night dim and
 * the multi-background switch have no Nova equivalent and are preserved in passthrough
 * for re-export instead of being applied.
 */
internal object MainWallpaperAsset : ThemedAsset {
    override val base = "background"

    override fun currentFile(context: Context): File? {
        val uri = SettingsRepositoryImpl().customBackgroundUri
        if (uri.isBlank()) return null
        val file = File(uri)
        return if (file.isFile && file.length() > 0L) file else null
    }

    override fun writeConfig(json: JSONObject) {
        val repo = SettingsRepositoryImpl()
        json.put("isBackgroundEnabled", repo.customBackgroundUri.isNotBlank() && !repo.useSystemWallpaper)
        json.put("backgroundBlur", repo.customBackgroundBlurPercent.toDouble())
        json.put("backgroundDim", repo.customBackgroundScrimPercent / 100.0)
        json.put("wallpaper_use_color", repo.extractColorFromBackground)
    }

    override suspend fun apply(context: Context, json: JSONObject, file: File?) {
        val repo: SettingsRepository = SettingsRepositoryImpl()
        val enabled = json.optBoolean("isBackgroundEnabled", false)
        if (enabled && file != null) {
            // ImageStorage.replace moves its source (renameTo), so hand it a throwaway copy:
            // the staged file must survive for the passthrough copy later in the import.
            val temp = File(context.cacheDir, "fpt-bg-${UUID.randomUUID()}")
            try {
                file.copyTo(temp, overwrite = true)
                val path = ImageStorage.replace(context, temp)
                if (path != null) {
                    repo.customBackgroundUri = path
                    repo.useSystemWallpaper = false
                    repo.customBackgroundUpdatedAt = System.currentTimeMillis()
                }
            } finally {
                temp.delete()
            }
        } else if (!enabled) {
            ImageStorage.clear(context)
            repo.customBackgroundUri = ""
        }
        repo.customBackgroundBlurPercent =
            json.optDouble("backgroundBlur", repo.customBackgroundBlurPercent.toDouble())
                .toFloat().toInt().coerceIn(0, 100)
        repo.customBackgroundScrimPercent =
            (json.optDouble("backgroundDim", repo.customBackgroundScrimPercent / 100.0) * 100.0)
                .toFloat().toInt().coerceIn(0, 100)
        repo.extractColorFromBackground = json.optBoolean("wallpaper_use_color", repo.extractColorFromBackground)
    }

    override suspend fun reset(context: Context) {
        ImageStorage.clear(context)
        SettingsRepositoryImpl().customBackgroundUri = ""
    }
}

/**
 * The app font. Only [FontMode.CUSTOM] owns a file; the other two modes are pure configuration, so
 * they round-trip through `theme.json` alone.
 *
 * The key names and the `font.ttf` asset name are shared with the wider theme ecosystem, so a theme
 * exported elsewhere imports here (and vice versa).
 */
internal object FontAsset : ThemedAsset {
    override val base = "font"

    override fun extension(file: File): String = "ttf"

    override fun currentFile(context: Context): File? {
        if (FontConfig.fontMode != FontMode.CUSTOM) return null
        val filename = FontConfig.customFontFilename ?: return null
        val file = File(context.filesDir, filename)
        return if (file.exists() && file.length() > 0L) file else null
    }

    override fun writeConfig(json: JSONObject) {
        json.put("fontMode", FontConfig.fontMode.serializedName)
        json.put("isFontEnabled", FontConfig.isCustomFontEnabled)
    }

    override suspend fun apply(context: Context, json: JSONObject, file: File?) {
        // Themes written before the three-mode setting only carry the legacy boolean.
        val mode = FontMode.fromSerializedName(json.optString("fontMode").ifEmpty { null })
            ?: if (json.optBoolean("isFontEnabled", false)) FontMode.CUSTOM else FontMode.APP_DEFAULT

        when {
            mode != FontMode.CUSTOM -> FontConfig.setFontMode(context, mode)
            file != null -> FontConfig.applyCustomFont(context, file)
            else -> FontConfig.setFontMode(context, FontMode.APP_DEFAULT)
        }
    }

    override suspend fun reset(context: Context) {
        FontConfig.clearFont(context)
    }
}

/** The file-name stem of a zip entry: `nav_icon_Home.png` -> `nav_icon_Home`. */
internal fun zipStem(entryName: String): String =
    entryName.substringBefore('.').substringAfterLast('/')
