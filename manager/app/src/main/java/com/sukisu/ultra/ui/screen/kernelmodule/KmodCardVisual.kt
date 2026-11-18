package com.sukisu.ultra.ui.screen.kernelmodule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.sukisu.ultra.data.modulebanner.BannerApiClient
import com.sukisu.ultra.data.modulebanner.CustomModuleInfo
import com.sukisu.ultra.data.modulebanner.LkmBannerSlot
import com.sukisu.ultra.data.modulebanner.ModuleBannerConfig
import com.sukisu.ultra.data.modulebanner.ModuleBannerResolver
import com.sukisu.ultra.data.modulebanner.ModuleBannerStorage
import com.sukisu.ultra.data.modulebanner.ModuleCustomInfoStorage
import com.sukisu.ultra.ui.screen.module.ModuleCardVisual
import com.sukisu.ultra.ui.screen.module.decodeBannerBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Everything one managed kernel-module card needs from the banner system,
 * resolved in the same order as the module/plugin cards: API banner first,
 * then the module's own banner (a user-picked image, then a banner file
 * shipped inside the module directory).
 *
 * This is the kernel-module counterpart of `rememberPluginCardVisual`:
 * managed .ko modules live in `/data/adb/ksu/lkm` and carry no
 * `module.prop`, so the slot-aware FinaSU sources ([ModuleBannerStorage],
 * [ModuleBannerResolver]) are used instead of ZephyrSU's module pipeline.
 * Decoded bitmaps go through ZephyrSU's own `bannerCache` via
 * [decodeBannerBytes], so no duplicate cache is introduced.
 */
@Composable
fun rememberKmodCardVisual(moduleId: String): ModuleCardVisual {
    val policy = ModuleBannerConfig.policy
    val reloadKey = ModuleBannerConfig.reloadKey
    val context = LocalContext.current

    // The API banner, fetched only when API mode is configured. BannerApiClient
    // keeps its own 24h disk cache; the decoded bitmap is cached in ZephyrSU's
    // bannerCache, so a scroll never re-decodes.
    val apiBanner by produceState<ImageBitmap?>(null, moduleId, policy, reloadKey) {
        value = if (policy.apiConfigured) {
            withContext(Dispatchers.IO) {
                BannerApiClient.load(context, moduleId, policy.apiSource)
                    ?.let { decodeBannerBytes(it, "$moduleId:api:${policy.apiSource}") }
            }
        } else {
            null
        }
    }

    // The module's own banner: a user-picked image first, then a banner file
    // shipped inside the module directory.
    val ownBanner by produceState<ImageBitmap?>(null, moduleId, reloadKey) {
        value = withContext(Dispatchers.IO) {
            ModuleBannerStorage(LkmBannerSlot, context).read(moduleId)
                ?.let { decodeBannerBytes(it, "$moduleId:user") }
                ?: ModuleBannerResolver.readModuleFileBanner(LkmBannerSlot, moduleId)
                    ?.let { decodeBannerBytes(it, "$moduleId:file") }
        }
    }

    // The user's metadata overrides are not a banner source, so they are read
    // either way.
    val customInfo by produceState<CustomModuleInfo?>(null, moduleId, reloadKey) {
        value = withContext(Dispatchers.IO) {
            runCatching { ModuleCustomInfoStorage(LkmBannerSlot, context).read(moduleId) }.getOrNull()
        }
    }

    val banner = when {
        !policy.active -> null
        apiBanner != null -> apiBanner
        else -> ownBanner
    }
    return ModuleCardVisual(banner = banner, customInfo = customInfo)
}
