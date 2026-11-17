package com.sukisu.ultra.ui.screen.plugin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.sukisu.ultra.data.modulebanner.BannerApiClient
import com.sukisu.ultra.data.modulebanner.CustomModuleInfo
import com.sukisu.ultra.data.modulebanner.ModuleBannerConfig
import com.sukisu.ultra.data.modulebanner.ModuleBannerResolver
import com.sukisu.ultra.data.modulebanner.ModuleBannerStorage
import com.sukisu.ultra.data.modulebanner.ModuleCustomInfoStorage
import com.sukisu.ultra.data.modulebanner.PluginBannerSlot
import com.sukisu.ultra.ui.screen.module.ModuleCardVisual
import com.sukisu.ultra.ui.screen.module.decodeBannerBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Everything one plugin card needs from the banner system, resolved in the
 * same order as the module cards: API banner first, then the plugin's own
 * banner (a user-picked image, then a banner file shipped inside the plugin
 * directory).
 *
 * This is the plugin counterpart of `rememberModuleCardVisual`, which is
 * APM-specific (it reads `/data/adb/modules` and a `banner=` module.prop
 * line). Plugins live in `/data/adb/plugins` and carry no `module.prop`, so
 * the slot-aware FinaSU sources ([ModuleBannerStorage],
 * [ModuleBannerResolver]) are used instead of ZephyrSU's module pipeline.
 * Decoded bitmaps go through ZephyrSU's own `bannerCache` via
 * [decodeBannerBytes], so no duplicate cache is introduced.
 */
@Composable
fun rememberPluginCardVisual(pluginId: String): ModuleCardVisual {
    val policy = ModuleBannerConfig.policy
    val reloadKey = ModuleBannerConfig.reloadKey
    val context = LocalContext.current

    // The API banner, fetched only when API mode is configured. BannerApiClient
    // keeps its own 24h disk cache; the decoded bitmap is cached in ZephyrSU's
    // bannerCache, so a scroll never re-decodes.
    val apiBanner by produceState<ImageBitmap?>(null, pluginId, policy, reloadKey) {
        value = if (policy.apiConfigured) {
            withContext(Dispatchers.IO) {
                BannerApiClient.load(context, pluginId, policy.apiSource)
                    ?.let { decodeBannerBytes(it, "$pluginId:api:${policy.apiSource}") }
            }
        } else {
            null
        }
    }

    // The plugin's own banner: a user-picked image first, then a banner file
    // shipped inside the plugin directory.
    val ownBanner by produceState<ImageBitmap?>(null, pluginId, reloadKey) {
        value = withContext(Dispatchers.IO) {
            ModuleBannerStorage(PluginBannerSlot, context).read(pluginId)
                ?.let { decodeBannerBytes(it, "$pluginId:user") }
                ?: ModuleBannerResolver.readModuleFileBanner(PluginBannerSlot, pluginId)
                    ?.let { decodeBannerBytes(it, "$pluginId:file") }
        }
    }

    // The user's metadata overrides are not a banner source, so they are read
    // either way.
    val customInfo by produceState<CustomModuleInfo?>(null, pluginId, reloadKey) {
        value = withContext(Dispatchers.IO) {
            runCatching { ModuleCustomInfoStorage(PluginBannerSlot, context).read(pluginId) }.getOrNull()
        }
    }

    val banner = when {
        !policy.active -> null
        apiBanner != null -> apiBanner
        else -> ownBanner
    }
    return ModuleCardVisual(banner = banner, customInfo = customInfo)
}
