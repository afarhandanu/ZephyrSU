package com.sukisu.ultra.ui.screen.module

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.sukisu.ultra.data.model.Module
import com.sukisu.ultra.data.modulebanner.ApmBannerSlot
import com.sukisu.ultra.data.modulebanner.BannerApiClient
import com.sukisu.ultra.data.modulebanner.CustomModuleInfo
import com.sukisu.ultra.data.modulebanner.ModuleBannerConfig
import com.sukisu.ultra.data.modulebanner.ModuleCustomInfoStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Everything one module card needs from the banner system, resolved in FinaSU's order:
 * API banner first, then the module's own banner (which also covers a user-picked banner,
 * because ZephyrSU writes picks back into the module as `banner_custom.*` + `module.prop`).
 *
 * This is the Nova-side counterpart of FinaSU's `rememberModuleCardVisual`: the module's own
 * banner link reuses ZephyrSU's existing [rememberModuleBanner] instead of FinaSU's
 * `ModuleBannerResolver` (no duplicate resolver), and decoded API bitmaps go through ZephyrSU's
 * own `bannerCache` instead of FinaSU's `ModuleBannerCache` (no duplicate cache).
 */
@Immutable
data class ModuleCardVisual(
    val banner: ImageBitmap? = null,
    val customInfo: CustomModuleInfo? = null,
)

@Composable
fun rememberModuleCardVisual(
    moduleId: String,
    bannerPath: String?,
): ModuleCardVisual {
    val policy = ModuleBannerConfig.policy
    val reloadKey = ModuleBannerConfig.reloadKey
    val context = LocalContext.current

    // The module's own banner, resolved by ZephyrSU's existing pipeline.
    val ownBanner = rememberModuleBanner(moduleId, bannerPath)

    // The API banner, fetched only when API mode is configured. BannerApiClient keeps its own
    // 24h disk cache; the decoded bitmap is cached in ZephyrSU's bannerCache, so a scroll
    // never re-decodes.
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

    // The user's metadata overrides are not a banner source, so they are read either way.
    val customInfo by produceState<CustomModuleInfo?>(null, moduleId, reloadKey) {
        value = withContext(Dispatchers.IO) {
            runCatching { ModuleCustomInfoStorage(ApmBannerSlot, context).read(moduleId) }.getOrNull()
        }
    }

    val banner = when {
        !policy.active -> null
        apiBanner != null -> apiBanner
        else -> ownBanner
    }
    return ModuleCardVisual(banner = banner, customInfo = customInfo)
}

/** Banner image alpha for module cards: the custom opacity when its switch is on, else full. */
fun bannerAlpha(): Float =
    if (ModuleBannerConfig.customOpacityEnabled) ModuleBannerConfig.customOpacity else 1f

/** Display fields for a module card, with the user's custom-info overrides applied. */
fun Module.displayName(customInfo: CustomModuleInfo?): String =
    customInfo?.name?.takeIf { it.isNotBlank() } ?: name

fun Module.displayVersion(customInfo: CustomModuleInfo?): String =
    customInfo?.version?.takeIf { it.isNotBlank() } ?: version

fun Module.displayAuthor(customInfo: CustomModuleInfo?): String =
    customInfo?.author?.takeIf { it.isNotBlank() } ?: author

fun Module.displayDescription(customInfo: CustomModuleInfo?): String =
    customInfo?.description?.takeIf { it.isNotBlank() } ?: description

/**
 * Edit state for the "Customize info" section of the module information dialogs. Reads and
 * writes happen on IO; [onChanged] asks the caller to re-read the module list.
 */
@Stable
class ModuleCustomInfoEditorState internal constructor(
    private val context: Context,
    private val module: Module,
    private val scope: CoroutineScope,
    private val onChanged: () -> Unit,
) {
    var name by mutableStateOf(module.name)
        private set
    var version by mutableStateOf(module.version)
        private set
    var author by mutableStateOf(module.author)
        private set
    var description by mutableStateOf(module.description)
        private set
    var busy by mutableStateOf(false)
        private set
    var hasSavedInfo by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set

    private var initialName = module.name
    private var initialVersion = module.version
    private var initialAuthor = module.author
    private var initialDescription = module.description

    val hasChanges: Boolean
        get() = name != initialName || version != initialVersion ||
            author != initialAuthor || description != initialDescription

    init {
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching { ModuleCustomInfoStorage(ApmBannerSlot, context).read(module.id) }.getOrNull()
            }
            if (saved != null) {
                hasSavedInfo = true
                initialName = saved.name.takeIf { it.isNotBlank() } ?: module.name
                initialVersion = saved.version.takeIf { it.isNotBlank() } ?: module.version
                initialAuthor = saved.author.takeIf { it.isNotBlank() } ?: module.author
                initialDescription = saved.description.takeIf { it.isNotBlank() } ?: module.description
                name = initialName
                version = initialVersion
                author = initialAuthor
                description = initialDescription
            }
            loaded = true
        }
    }

    fun updateName(value: String) {
        name = value
    }

    fun updateVersion(value: String) {
        version = value
    }

    fun updateAuthor(value: String) {
        author = value
    }

    fun updateDescription(value: String) {
        description = value
    }

    fun save() {
        if (busy) return
        busy = true
        scope.launch {
            withContext(Dispatchers.IO) {
                ModuleCustomInfoStorage(ApmBannerSlot, context).write(
                    module.id,
                    CustomModuleInfo(
                        name = name.takeIf { it.isNotBlank() }.orEmpty(),
                        version = version.takeIf { it.isNotBlank() }.orEmpty(),
                        author = author.takeIf { it.isNotBlank() }.orEmpty(),
                        description = description.takeIf { it.isNotBlank() }.orEmpty(),
                    ),
                )
            }
            hasSavedInfo = true
            initialName = name
            initialVersion = version
            initialAuthor = author
            initialDescription = description
            busy = false
            ModuleBannerConfig.requestReload()
            onChanged()
        }
    }

    fun reset() {
        if (busy) return
        busy = true
        scope.launch {
            withContext(Dispatchers.IO) {
                ModuleCustomInfoStorage(ApmBannerSlot, context).clear(module.id)
            }
            hasSavedInfo = false
            initialName = module.name
            initialVersion = module.version
            initialAuthor = module.author
            initialDescription = module.description
            name = module.name
            version = module.version
            author = module.author
            description = module.description
            busy = false
            ModuleBannerConfig.requestReload()
            onChanged()
        }
    }
}

@Composable
fun rememberModuleCustomInfoEditorState(
    module: Module,
    onChanged: () -> Unit,
): ModuleCustomInfoEditorState {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    return remember(module.id, context, scope) {
        ModuleCustomInfoEditorState(context, module, scope, onChanged)
    }
}
