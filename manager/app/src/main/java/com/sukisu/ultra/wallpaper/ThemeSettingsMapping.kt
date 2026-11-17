package com.sukisu.ultra.wallpaper

import com.materialkolor.PaletteStyle
import com.sukisu.ultra.data.repository.SettingsRepository
import com.sukisu.ultra.data.repository.SettingsRepositoryImpl
import com.sukisu.ultra.ksuApp
import com.sukisu.ultra.ui.theme.ColorMode
import com.sukisu.ultra.ui.util.LocaleHelper
import org.json.JSONObject

/**
 * Protocol names map onto the existing settings repository; no second settings store.
 *
 * Adapted from FinaSU for Nova's settings: Nova has no `useSystemDynamicColor`,
 * `colorGenerationMode`, `colorContrast`, `statsTopLayout` or `listWorkingCardModeHidden`
 * preferences. Dynamic color in Nova IS `keyColor == 0` (see `MaterialTheme`), so
 * `useSystemDynamicColor=true` maps to `keyColor = 0`; the other four fields are
 * preserve-only — they survive in the theme passthrough for re-export but are not applied.
 * Language goes through Nova's [LocaleHelper] instead of FinaSU's `ThemeLanguage`
 * (different preference key: Nova uses `app_language`, FinaSU used `fpt_app_language`).
 */
internal object ThemeSettingsMapping {
    internal const val KEY_COLOR = "finasu_keyColor"
    internal const val LEGACY_KEY_COLOR = "folksu_keyColor"

    // FP applies these defaults for old/config-only themes. Language is intentionally absent:
    // a missing language field keeps the user's locale, while an explicit empty field resets it.
    val importDefaults: Map<String, Any> = mapOf(
        "customColor" to "indigo", "homeLayoutStyle" to "circle", "statsTopLayout" to "list",
        "nightModeEnabled" to true, "nightModeFollowSys" to true, "useSystemDynamicColor" to true,
        "colorGenerationMode" to "classic", "colorStandard" to "MD3_2021",
        "colorStyle" to "TONAL_SPOT", "colorContrast" to "STANDARD",
        "isListWorkingCardModeHidden" to false,
    )

    val colors = linkedMapOf(
        "indigo" to 0xFF4355B9, "blue" to 0xFF0061A4, "light_blue" to 0xFF006493,
        "cyan" to 0xFF006876, "teal" to 0xFF006A60, "green" to 0xFF006E1A,
        "light_green" to 0xFF006C48, "lime" to 0xFF5B6300, "yellow" to 0xFF695F00,
        "amber" to 0xFF785900, "orange" to 0xFF8B5000, "deep_orange" to 0xFFB02F00,
        "red" to 0xFFBB1614, "pink" to 0xFFBC004B, "purple" to 0xFF9A25AE,
        "deep_purple" to 0xFF6F43C0, "brown" to 0xFF9A4522, "blue_grey" to 0xFF00668A,
        "sakura" to 0xFF9B404F, "ink_wash" to 0xFF424242,
    ).mapValues { it.value.toInt() }
    val styles = PaletteStyle.entries.associateBy { style ->
        style.name.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
    }

    fun write(json: JSONObject, repo: SettingsRepository = SettingsRepositoryImpl()) {
        writeColors(json, repo)
        writeInterface(json, repo)
        writeLanguage(json)
    }

    fun writeColors(json: JSONObject, repo: SettingsRepository = SettingsRepositoryImpl()) {
        val color = colors.entries.firstOrNull { it.value == repo.keyColor }?.key
        json.put("customColor", color ?: "indigo")
        // Arbitrary FinaSU seeds have no FP catalog token. Keep the exact value in an extension.
        json.put(KEY_COLOR, repo.keyColor)
        // Nova renders dynamic color when keyColor == 0; there is no separate toggle to write.
        json.put("useSystemDynamicColor", repo.keyColor == 0)
        // colorGenerationMode / colorContrast have no Nova preference; the export keeps the
        // original theme.json values for them instead of overwriting.
        json.put("colorStandard", if (repo.colorSpec == "SPEC_2025") "M3E_2025" else "MD3_2021")
        json.put("colorStyle", styles.entries.firstOrNull { it.value.name == repo.colorStyle }?.key ?: "TONAL_SPOT")
    }

    fun writeInterface(json: JSONObject, repo: SettingsRepository = SettingsRepositoryImpl()) {
        val mode = ColorMode.fromValue(repo.themeMode)
        json.put("nightModeEnabled", mode.isDark)
        json.put("nightModeFollowSys", mode.isSystem)
        // statsTopLayout / isListWorkingCardModeHidden have no Nova preference; the export
        // keeps the original theme.json values for them instead of overwriting.
    }

    fun writeLanguage(json: JSONObject) {
        json.put("appLanguage", LocaleHelper.getCurrentLanguage(ksuApp))
    }

    fun apply(json: JSONObject, repo: SettingsRepository = SettingsRepositoryImpl()) {
        applyColors(json, repo)
        applyInterface(json, repo)
    }

    fun applyColors(json: JSONObject, repo: SettingsRepository = SettingsRepositoryImpl()) {
        if (json.has("customColor")) colors[json.optString("customColor")]?.let { repo.keyColor = it }
        when {
            json.has(KEY_COLOR) -> repo.keyColor = json.getInt(KEY_COLOR)
            json.has(LEGACY_KEY_COLOR) -> repo.keyColor = json.getInt(LEGACY_KEY_COLOR)
        }
        // Nova dynamic color is keyColor == 0, so a theme asking for dynamic color clears the seed.
        if (json.optBoolean("useSystemDynamicColor", false)) repo.keyColor = 0
        // colorGenerationMode / colorContrast have no Nova preference; preserved in passthrough.
        if (json.has("colorStandard")) repo.colorSpec = when (json.getString("colorStandard")) {
            "M3E_2025" -> "SPEC_2025"
            else -> "SPEC_2021"
        }
        if (json.has("colorStyle")) styles[json.getString("colorStyle")]?.let { repo.colorStyle = it.name }
    }

    fun applyInterface(json: JSONObject, repo: SettingsRepository = SettingsRepositoryImpl()) {
        if (json.has("nightModeEnabled") || json.has("nightModeFollowSys")) {
            val current = ColorMode.fromValue(repo.themeMode)
            repo.themeMode = when {
                json.optBoolean("nightModeFollowSys", current.isSystem) -> ColorMode.SYSTEM.value
                json.optBoolean("nightModeEnabled", current.isDark) -> ColorMode.DARK.value
                else -> ColorMode.LIGHT.value
            }
        }
        // statsTopLayout / isListWorkingCardModeHidden have no Nova preference; preserved.
    }

    /**
     * Restores every setting a theme can carry to the app defaults. Layout and the asset files
     * reset through their own assets, so only the preference-backed values live here.
     */
    fun reset(repo: SettingsRepository = SettingsRepositoryImpl()) {
        repo.themeMode = ColorMode.SYSTEM.value
        repo.keyColor = 0
        repo.colorStyle = PaletteStyle.TonalSpot.name
        repo.colorSpec = "SPEC_2025"
    }

    fun applyLanguage(json: JSONObject, repo: SettingsRepository = SettingsRepositoryImpl()) {
        if (!json.has("appLanguage")) return
        repo.appLanguage = json.getString("appLanguage")
    }
}
