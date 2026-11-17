package com.sukisu.ultra.wallpaper

import android.content.Context
import com.sukisu.ultra.data.repository.SettingsRepositoryImpl
import com.sukisu.ultra.ui.util.BottomBarIconSlot
import com.sukisu.ultra.ui.util.ImageStorage
import org.json.JSONObject
import java.io.File

/**
 * Custom bottom-bar icons, keyed by the `.fpt` destination name.
 *
 * Adapted from FinaSU: Nova has no `BottomBarIconConfig` with an enable flag — icons are
 * enabled by being present in `SettingsRepository.bottomBarIcons`, keyed by
 * [BottomBarIconSlot.key]. The FP theme keys map onto Nova slots as Home→home,
 * SuperUser→superuser, AModule→module, Settings→setting. `KModule` (the plugin tab) has
 * no Nova slot; its file is retained in passthrough for re-export.
 */
internal object NavIconsAsset : ThemedAssetGroup {
    override val part = ThemePart.NAV_ICONS

    private val themeKeyBySlot = mapOf(
        BottomBarIconSlot.Home to "Home",
        BottomBarIconSlot.SuperUser to "SuperUser",
        BottomBarIconSlot.Module to "AModule",
        BottomBarIconSlot.Setting to "Settings",
    )
    private val slotByThemeKey = themeKeyBySlot.entries.associate { (slot, key) -> key to slot }

    private fun entryName(themeKey: String) = "nav_icon_${themeKey}.png"

    override fun writeConfig(json: JSONObject) {
        val icons = SettingsRepositoryImpl().bottomBarIcons
        json.put("navIconCustomEnabled", icons.isNotEmpty())
        json.remove("navIcons")
        if (icons.isEmpty()) return
        val out = JSONObject()
        themeKeyBySlot.forEach { (slot, themeKey) ->
            if (icons.containsKey(slot.key)) out.put(themeKey, entryName(themeKey))
        }
        if (out.length() > 0) json.put("navIcons", out)
    }

    override fun currentFiles(context: Context): Map<String, File> {
        val icons = SettingsRepositoryImpl().bottomBarIcons
        if (icons.isEmpty()) return emptyMap()
        return buildMap {
            themeKeyBySlot.forEach { (slot, themeKey) ->
                val path = icons[slot.key] ?: return@forEach
                val file = File(path)
                if (file.isFile && file.length() > 0L) put(entryName(themeKey), file)
            }
        }
    }

    override suspend fun apply(context: Context, json: JSONObject, imported: Map<String, File>) {
        val enabled = json.optBoolean("navIconCustomEnabled", false) ||
            json.optBoolean("nav_icon_custom_enabled", false)
        val icons = json.optJSONObject("navIcons")
        val repo = SettingsRepositoryImpl()
        if (!enabled) {
            ImageStorage.clearBottomBarIcons(context)
            repo.bottomBarIcons = emptyMap()
            return
        }
        val updated = repo.bottomBarIcons.toMutableMap()
        slotByThemeKey.forEach { (themeKey, slot) ->
            val name = icons?.optString(themeKey).orEmpty()
            val source = imported[name]
            if (source != null) {
                ImageStorage.importBottomBarIcon(context, slot, source)?.let { path ->
                    updated[slot.key] = path
                }
            }
        }
        repo.bottomBarIcons = updated
    }

    override suspend fun reset(context: Context) {
        ImageStorage.clearBottomBarIcons(context)
        SettingsRepositoryImpl().bottomBarIcons = emptyMap()
    }
}
