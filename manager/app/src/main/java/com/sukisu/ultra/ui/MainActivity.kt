package com.sukisu.ultra.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.annotation.RequiresApi
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import com.sukisu.ultra.ui.component.onboarding.FirstRunGuide
import com.sukisu.ultra.ui.component.splash.SplashScreen
import com.sukisu.ultra.ui.theme.LocalAppBackgroundBackdrop
import com.sukisu.ultra.ui.theme.LocalAppSettings
import com.sukisu.ultra.ui.theme.MaterialExpressivePalette
import com.sukisu.ultra.ui.theme.LocalBackgroundImagePath
import com.sukisu.ultra.ui.theme.LocalCardBackgroundAlpha
import com.sukisu.ultra.ui.theme.appearanceImageEffects
import java.io.File
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults.flingBehavior
import androidx.compose.foundation.pager.PagerDefaults.pageNestedScrollConnection
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.sukisu.ultra.Natives
import com.sukisu.ultra.ui.component.bottombar.BottomBar
import com.sukisu.ultra.ui.component.bottombar.BottomBarDestination
import com.sukisu.ultra.ui.component.bottombar.LocalFloatingBarReservedHeight
import com.sukisu.ultra.ui.component.bottombar.LocalFloatingBarVisible
import com.sukisu.ultra.ui.component.bottombar.LocalIsFloatingNavMode
import com.sukisu.ultra.ui.component.bottombar.MainPagerState
import com.sukisu.ultra.ui.component.bottombar.NavigationBadgeState
import com.sukisu.ultra.ui.component.bottombar.SideRail
import com.sukisu.ultra.ui.component.bottombar.rememberFloatingBarVisibilityState
import com.sukisu.ultra.ui.component.bottombar.rememberMainPagerState
import com.sukisu.ultra.ui.component.bottombar.useNavigationRail
import com.sukisu.ultra.ui.component.bottombar.visibleDestinations
import com.sukisu.ultra.ui.kernelFlash.KernelFlashScreen
import com.sukisu.ultra.ui.navigation3.IntentDispatcher
import com.sukisu.ultra.ui.navigation3.LocalNavigator
import com.sukisu.ultra.ui.navigation3.Navigator
import com.sukisu.ultra.ui.navigation3.Route
import com.sukisu.ultra.ui.navigation3.rememberNavigator
import com.sukisu.ultra.media.VisualMediaConfig
import com.sukisu.ultra.ui.component.material.SliderTrackConfig
import com.sukisu.ultra.ui.navigation3.mainPageToWallpaperPage
import com.sukisu.ultra.ui.navigation3.wallpaperPageFor
import com.sukisu.ultra.ui.screen.home.LocalHomeLayoutStyle
import com.sukisu.ultra.ui.screen.wallpaper.WallpaperGalleryScreen
import com.sukisu.ultra.ui.screen.wallpaper.WallpaperScreen
import com.sukisu.ultra.wallpaper.LocalWallpaperPage
import com.sukisu.ultra.wallpaper.WallpaperBackgroundLayer
import com.sukisu.ultra.wallpaper.WallpaperConfig
import com.sukisu.ultra.ui.screen.about.AboutScreen
import com.sukisu.ultra.ui.screen.bannerapi.ApiMarketplaceScreen
import com.sukisu.ultra.ai.api.AiFeatureBridge
import com.sukisu.ultra.ai.host.ManagerAiTarget
import com.sukisu.ultra.ui.screen.appicon.AppIconScreen
import com.sukisu.ultra.ui.screen.customtext.CustomTextScreen
import com.sukisu.ultra.ui.screen.account.AccountScreen
import com.sukisu.ultra.ui.screen.fptstore.FptMyThemesScreen
import com.sukisu.ultra.ui.screen.fptstore.FptThemeStoreScreen
import com.sukisu.ultra.ui.screen.themestore.ThemeStoreScreen
import com.sukisu.ultra.ui.screen.themestore.ThemeStoreSourcesScreen
import com.sukisu.ultra.ui.util.LocalCustomTexts
import com.sukisu.ultra.ui.screen.appprofile.AppProfileScreen
import com.sukisu.ultra.ui.screen.backgroundcrop.BackgroundCropScreen
import com.sukisu.ultra.ui.screen.colorpalette.AdaptiveThemeBuilderScreen
import com.sukisu.ultra.ui.screen.colorpalette.ColorPaletteScreen
import com.sukisu.ultra.ui.screen.appearanceoverlay.AppearanceOverlayScreen
import com.sukisu.ultra.ui.screen.dynamicmanagers.DynamicManagersScreen
import com.sukisu.ultra.ui.screen.extensions.ExtensionsScreen
import com.sukisu.ultra.ui.screen.extensions.CpuSpoofScreen
import com.sukisu.ultra.ui.screen.extensions.VersionSpoofScreen
import com.sukisu.ultra.ui.screen.executemoduleaction.ExecuteModuleActionScreen
import com.sukisu.ultra.ui.screen.flash.FlashScreen
import com.sukisu.ultra.ui.screen.home.HomePager
import com.sukisu.ultra.ui.screen.home.LocalSupportLinksCustom
import com.sukisu.ultra.ui.screen.home.LocalSupportLinksStyle
import com.sukisu.ultra.ui.screen.home.SupportLinksCustomContent
import com.sukisu.ultra.ui.screen.install.InstallScreen
import com.sukisu.ultra.ui.screen.kernelmodules.KernelModulesScreen
import com.sukisu.ultra.ui.screen.kpm.KpmScreen
import com.sukisu.ultra.ui.screen.module.ModulePager
import com.sukisu.ultra.ui.screen.modulerepo.ModuleRepoDetailScreen
import com.sukisu.ultra.ui.screen.modulerepo.ModuleRepoScreen
import com.sukisu.ultra.ui.screen.scriptrun.ScriptRunScreen
import com.sukisu.ultra.ui.screen.settings.SettingPager
import com.sukisu.ultra.ui.screen.settings.BackupListScreen
import com.sukisu.ultra.ui.screen.settings.tools.ToolsScreen
import com.sukisu.ultra.ui.screen.sulog.SulogScreen
import com.sukisu.ultra.ui.screen.superuser.SuperUserPager
import com.sukisu.ultra.ui.screen.susfs.SuSFSScreen
import com.sukisu.ultra.ui.screen.template.AppProfileTemplateScreen
import com.sukisu.ultra.ui.screen.templateeditor.TemplateEditorScreen
import com.sukisu.ultra.ui.screen.umountmanager.UmountManagerScreen
import com.sukisu.ultra.ui.screen.uts.UtsViewScreen
import com.sukisu.ultra.data.sui.SuiPermissionHost
import com.sukisu.ultra.ui.screen.builtinzygisk.BuiltinZygiskScreen
import com.sukisu.ultra.ui.screen.sui.SuiPermissionRequestDialog
import com.sukisu.ultra.ui.screen.sui.SuiPermissionsScreen
import com.sukisu.ultra.media.MusicConfig
import com.sukisu.ultra.media.MusicManager
import com.sukisu.ultra.media.SoundEffectConfig
import com.sukisu.ultra.media.SoundEffectManager
import com.sukisu.ultra.media.VibrationConfig
import com.sukisu.ultra.ui.component.settings.ProfileConfig
import com.sukisu.ultra.ui.screen.font.FontScreen
import com.sukisu.ultra.ui.screen.settings.MultimediaScreen
import com.sukisu.ultra.ksuApp
import com.sukisu.ultra.ui.component.security.AppLockGate
import com.sukisu.ultra.ui.component.security.AuthenticationHost
import com.sukisu.ultra.ui.component.security.LocalAppAuthenticator
import com.sukisu.ultra.ui.component.security.rememberAuthenticationSession
import com.sukisu.ultra.ui.screen.settings.AuthenticationScreen
import com.sukisu.ultra.ui.theme.FontConfig
import com.sukisu.ultra.ui.theme.AiAssistantMaterialTheme
import com.sukisu.ultra.ui.theme.KernelSUTheme
import com.sukisu.ultra.ui.theme.LocalColorMode
import com.sukisu.ultra.ui.theme.LocalEnableBlur
import com.sukisu.ultra.ui.theme.LocalEnableFloatingBottomBar
import com.sukisu.ultra.ui.theme.LocalEnableFloatingBottomBarBlur
import com.sukisu.ultra.ui.theme.LocalEnableNavigationBadge
import com.sukisu.ultra.ui.theme.LocalEnableProgressiveBlur
import com.sukisu.ultra.ui.theme.LocalFloatingAutoHide
import com.sukisu.ultra.ui.theme.LocalFloatingSwipeHide
import com.sukisu.ultra.ui.theme.LocalShowNavModule
import com.sukisu.ultra.ui.theme.LocalShowNavSuperuser
import com.sukisu.ultra.ui.theme.LocalModuleDescriptionMaxLines
import com.sukisu.ultra.ui.util.BackgroundNavTransition
import com.sukisu.ultra.ui.util.ImageStorage
import com.sukisu.ultra.ui.util.LocaleHelper
import com.sukisu.ultra.ui.util.ThemeTrialManager
import com.sukisu.ultra.ui.util.getSuperuserCount
import com.sukisu.ultra.ui.util.install
import com.sukisu.ultra.ui.util.rememberBlurBackdrop
import com.sukisu.ultra.ui.util.rememberContentReady
import com.sukisu.ultra.ui.viewmodel.MainActivityViewModel
import com.sukisu.ultra.ui.viewmodel.ModuleViewModel
import com.sukisu.ultra.ui.viewmodel.SettingsViewModel
import com.sukisu.ultra.ui.viewmodel.SuperUserViewModel
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride
import androidx.core.graphics.drawable.toDrawable
import com.sukisu.ultra.R


// FragmentActivity (not ComponentActivity): App Lock's BiometricPrompt needs it.
// Ported from FinaSU, Milestone 2.
class MainActivity : FragmentActivity() {
    private val intentChannel = Channel<Intent>(capacity = Channel.BUFFERED)

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        FontConfig.load(this)
        ProfileConfig.load(this)
        com.sukisu.ultra.data.modulebanner.ModuleBannerConfig.loadOnce()
        MusicConfig.load(this)
        SoundEffectConfig.load(this)
        WallpaperConfig.load(this)
        VisualMediaConfig.load(this)
        SliderTrackConfig.load(this)
        VibrationConfig.load(this)
        MusicManager.init(this)
        SoundEffectManager.playStartup(this)
        if (ThemeTrialManager.isActive(this)) {
            ViewModelProvider(this)[SettingsViewModel::class.java]
                .restoreThemeTrial(this, taskId)
        } else {
            ThemeTrialManager.cleanupOrphan(this)
        }

        val isManager = Natives.isManager
        if (isManager && Natives.kernelUAPIVersion == Natives.managerUAPIVersion) install()

        if (savedInstanceState == null) intent?.let { intentChannel.trySend(it) }

        setContent {
            val viewModel = viewModel<MainActivityViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val selectedMainPage by viewModel.selectedMainPage.collectAsStateWithLifecycle()
            val appSettings = uiState.appSettings
            val uiMode = uiState.uiMode
            val darkMode = appSettings.colorMode.isDark || (appSettings.colorMode.isSystem && isSystemInDarkTheme())

            // Pick up launcher wallpaper changes while the "use system wallpaper" option is on.
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemWallpaper()
                // Also covers a mode change that happened while the app was in the background.
                viewModel.syncAdaptiveTheme(darkMode)
            }

            // Adaptive theme packages carry a light and a dark half; swap them with the effective
            // light/dark mode (system-following or a forced app mode).
            LaunchedEffect(darkMode) {
                viewModel.syncAdaptiveTheme(darkMode)
            }

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                )
                window.isNavigationBarContrastEnforced = false
                onDispose { }
            }

            LaunchedEffect(darkMode) {
                window.setBackgroundDrawable(
                    getColor(if (darkMode) android.R.color.background_dark else android.R.color.background_light).toDrawable()
                )
            }

            val navigator = rememberNavigator(Route.Main)
            // FinaSU wallpaper (M5.2): the page whose background paints, from the nav
            // back stack with the visible main pager page as the fallback. Nova's
            // pager has four pages; mainPageToWallpaperPage maps them onto FinaSU's
            // five page slots (there is no Plugin page in Nova).
            val wallpaperPage = wallpaperPageFor(navigator.backStack, mainPageToWallpaperPage(selectedMainPage))
            val videoActive = VisualMediaConfig.videoEnabled && VisualMediaConfig.videoFilename != null
            val wallpaperActive = WallpaperConfig.enabled && WallpaperConfig.isPageActive(wallpaperPage, videoActive)
            // App Lock session (ported from FinaSU, Milestone 2).
            val authenticationSession = rememberAuthenticationSession(this@MainActivity, ksuApp.securityRepository)
            val context = LocalContext.current
            val uriHandler = LocalUriHandler.current
            val systemDensity = LocalDensity.current
            val density = remember(systemDensity, uiState.pageScale) {
                Density(systemDensity.density * uiState.pageScale, systemDensity.fontScale)
            }
            // Bars sample the page backdrop, which is transparent wherever the page paints nothing.
            // Blurring that reads as black, so over a custom background they also need the wallpaper:
            // capture it here, where only the background image is drawn, and combine it underneath.
            val appBackgroundBackdrop = rememberLayerBackdrop()
            // The progressive blur covers both shells and works regardless of the plain bar blur.
            val progressiveBlurActive = uiState.enableProgressiveBlur && isRuntimeShaderSupported()
            val captureAppBackground = (uiState.customBackgroundUri.isNotEmpty() || wallpaperActive) &&
                ((uiMode == UiMode.Miuix && uiState.enableBlur) || progressiveBlurActive)

            val themedAppSettings = remember(appSettings, uiState.keyColorOverride) {
                uiState.keyColorOverride?.let { appSettings.copy(keyColor = it) } ?: appSettings
            }
            CompositionLocalProvider(
                LocalUriHandler provides object : UriHandler {
                    override fun openUri(uri: String) {
                        try {
                            uriHandler.openUri(uri)
                        } catch (_: IllegalArgumentException) {
                            Toast.makeText(context, R.string.external_link_open_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                LocalNavigator provides navigator,
                LocalAppAuthenticator provides authenticationSession.authenticator,
                LocalDensity provides density,
                LocalColorMode provides appSettings.colorMode.value,
                LocalAppSettings provides themedAppSettings,
                LocalEnableBlur provides uiState.enableBlur,
                LocalEnableProgressiveBlur provides uiState.enableProgressiveBlur,
                LocalEnableFloatingBottomBar provides uiState.enableFloatingBottomBar,
                LocalEnableFloatingBottomBarBlur provides uiState.enableFloatingBottomBarBlur,
                LocalEnableNavigationBadge provides uiState.enableNavigationBadge,
                LocalShowNavSuperuser provides uiState.showNavSuperuser,
                LocalShowNavModule provides uiState.showNavModule,
                LocalFloatingAutoHide provides uiState.floatingAutoHide,
                LocalFloatingSwipeHide provides uiState.floatingSwipeHide,
                LocalModuleDescriptionMaxLines provides uiState.moduleDescriptionMaxLines,
                LocalBackgroundImagePath provides uiState.customBackgroundUri,
                LocalWallpaperPage provides wallpaperPage,
                LocalHomeLayoutStyle provides uiState.homeLayoutStyle,
                LocalAppBackgroundBackdrop provides appBackgroundBackdrop.takeIf { captureAppBackground },
                LocalCardBackgroundAlpha provides (uiState.cardBackgroundAlphaPercent / 100f),
                LocalUiMode provides uiMode,
                LocalCustomTexts provides uiState.customTexts,
                LocalSupportLinksStyle provides uiState.supportLinksStyle,
                LocalSupportLinksCustom provides SupportLinksCustomContent(
                    title = uiState.supportLinksCustomTitle,
                    subtitle = uiState.supportLinksCustomSubtitle,
                    url = uiState.supportLinksCustomUrl,
                ),
            ) {
                KernelSUTheme(appSettings = themedAppSettings, uiMode = uiMode) {
                    IntentDispatcher(intentChannel = intentChannel)
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            SuiPermissionHost.register(this@MainActivity)
                        }
                    }
                    SuiPermissionRequestDialog()
                    val swipeDismiss = if (uiState.enableSwipeDismiss) {
                        if (LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl) {
                            NavSwipeDirection.RightToLeft
                        } else {
                            NavSwipeDirection.LeftToRight
                        }
                    } else {
                        NavSwipeDirection.None
                    }
                    val flashRoute = navigator.current() as? Route.Flash
                    var flashBackEnabled by remember(flashRoute) { mutableStateOf(false) }
                    val scriptRunRoute = navigator.current() as? Route.ScriptRun
                    var scriptRunBackEnabled by remember(scriptRunRoute) { mutableStateOf(true) }
                    // Read here, not inside the NavDisplay builder: NavDisplay only rebuilds its entries
                    // (and their swipeDismiss) when the builder lambda changes, so it must capture the value.
                    val scriptRunSwipeDismiss = if (scriptRunBackEnabled) swipeDismiss else NavSwipeDirection.None
                    val mainScreenEntry = @Composable {
                        MainScreen(
                            initialPage = selectedMainPage,
                            pagerInterceptionMode = uiState.pagerInterceptionMode,
                            onPageChanged = viewModel::setSelectedMainPage,
                        )
                    }

                    val navDisplay = @Composable {
                        val backgroundActive = uiState.customBackgroundUri.isNotEmpty()
                        NavDisplay(
                            backStack = navigator.backStack,
                            transition = if (backgroundActive) {
                                BackgroundNavTransition
                            } else {
                                NavTransitions.MiuixDefault
                            },
                            effects = NavDisplayEffects(
                                cornerClipRadius = rememberNavSystemCornerRadius(),
                                // The dim scrim sits under the top destination even when idle; an
                                // opaque page hid it, but a transparent one would stay darkened.
                                dimAmount = if (backgroundActive) 0f else 0.5f,
                            ),
                            onBack = {
                                when (val top = navigator.current()) {
                                    is Route.TemplateEditor -> {
                                        if (!top.readOnly) {
                                            navigator.setResult("template_edit", true)
                                        } else {
                                            navigator.pop()
                                        }
                                    }

                                    else -> navigator.pop()
                                }
                            }) {
                            entry<Route.Main>(swipeDismiss = swipeDismiss) { mainScreenEntry() }
                            entry<Route.About>(swipeDismiss = swipeDismiss) { AboutScreen() }
                            entry<Route.Sulog>(swipeDismiss = swipeDismiss) { SulogScreen() }
                            entry<Route.ColorPalette>(swipeDismiss = swipeDismiss) { key -> ColorPaletteScreen(key.section, key.importUri) }
                            entry<Route.AppearanceOverlay>(swipeDismiss = swipeDismiss) {
                                AppearanceOverlayScreen(onBack = { navigator.pop() })
                            }
                            entry<Route.CustomTexts>(swipeDismiss = swipeDismiss) { CustomTextScreen() }
                            entry<Route.ThemeStore>(swipeDismiss = swipeDismiss) { key ->
                                MaterialExpressivePalette {
                                    ThemeStoreScreen(initialThemeId = key.themeId, initialSourceBaseUrl = key.sourceBaseUrl)
                                }
                            }
                            entry<Route.ThemeStoreAdaptiveUpload>(swipeDismiss = swipeDismiss) {
                                MaterialExpressivePalette {
                                    ThemeStoreScreen(startAdaptiveUpload = true)
                                }
                            }
                            entry<Route.ThemeStoreCurrentUpload>(swipeDismiss = swipeDismiss) {
                                MaterialExpressivePalette {
                                    ThemeStoreScreen(startCurrentUpload = true)
                                }
                            }
                            entry<Route.ThemeStoreSources>(swipeDismiss = swipeDismiss) {
                                MaterialExpressivePalette { ThemeStoreSourcesScreen() }
                            }
                            entry<Route.FptThemeStore>(swipeDismiss = swipeDismiss) { FptThemeStoreScreen() }
                            entry<Route.FptMyThemes>(swipeDismiss = swipeDismiss) { FptMyThemesScreen() }
                            entry<Route.Wallpaper>(swipeDismiss = swipeDismiss) { WallpaperScreen() }
                            entry<Route.WallpaperGallery>(swipeDismiss = swipeDismiss) { WallpaperGalleryScreen() }
                            entry<Route.ThemeAccount>(swipeDismiss = swipeDismiss) {
                                MaterialExpressivePalette { AccountScreen() }
                            }
                            entry<Route.AdaptiveThemeBuilder>(swipeDismiss = swipeDismiss) {
                                MaterialExpressivePalette { AdaptiveThemeBuilderScreen() }
                            }
                            entry<Route.AppIcon>(swipeDismiss = swipeDismiss) { AppIconScreen() }
                            entry<Route.BackgroundCrop>(swipeDismiss = NavSwipeDirection.None) { key ->
                                BackgroundCropScreen(key.sourcePath)
                            }
                            entry<Route.AppProfileTemplate>(swipeDismiss = swipeDismiss) { AppProfileTemplateScreen() }
                            entry<Route.TemplateEditor>(swipeDismiss = swipeDismiss) { key -> TemplateEditorScreen(key.template, key.readOnly) }
                            entry<Route.AppProfile>(swipeDismiss = swipeDismiss) { key -> AppProfileScreen(key.uid) }
                            entry<Route.ModuleRepo>(swipeDismiss = swipeDismiss) { ModuleRepoScreen() }
                            entry<Route.ApiMarketplace>(swipeDismiss = swipeDismiss) { ApiMarketplaceScreen() }
                            entry<Route.ModuleRepoDetail>(swipeDismiss = swipeDismiss) { key -> ModuleRepoDetailScreen(key.module) }
                            entry<Route.Install>(swipeDismiss = swipeDismiss) { key -> InstallScreen(preselectedKernelUri = key.preselectedKernelUri) }
                            entry<Route.Flash>(swipeDismiss = if (flashBackEnabled) swipeDismiss else NavSwipeDirection.None) { key ->
                                FlashScreen(
                                    key.flashIt,
                                    onBackEnabledChange = { enabled ->
                                        if (navigator.current() == key) flashBackEnabled = enabled
                                    },
                                )
                            }
                            entry<Route.ExecuteModuleAction>(swipeDismiss = swipeDismiss) { key ->
                                ExecuteModuleActionScreen(
                                    key.moduleId,
                                    key.fromShortcut
                                )
                            }
                            entry<Route.ScriptRun>(swipeDismiss = scriptRunSwipeDismiss) { key ->
                                ScriptRunScreen(
                                    key.arg,
                                    onBackEnabledChange = { enabled ->
                                        if (navigator.current() == key) scriptRunBackEnabled = enabled
                                    },
                                )
                            }
                            entry<Route.Home>(swipeDismiss = swipeDismiss) { mainScreenEntry() }
                            entry<Route.SuperUser>(swipeDismiss = swipeDismiss) { mainScreenEntry() }
                            entry<Route.Module>(swipeDismiss = swipeDismiss) { mainScreenEntry() }
                            entry<Route.Settings>(swipeDismiss = swipeDismiss) { mainScreenEntry() }
                            entry<Route.KernelFlash>(swipeDismiss = swipeDismiss)  { key ->
                                    KernelFlashScreen(
                                        key.kernelUri,
                                        key.selectedSlot,
                                        key.kpmPatchEnabled,
                                        key.kpmUndoPatch
                                    )
                                }
                                entry<Route.Kpm>(swipeDismiss = swipeDismiss)  { KpmScreen() }
                                entry<Route.SuSFS>(swipeDismiss = swipeDismiss)  { key -> SuSFSScreen(key.tab) }
                                entry<Route.Tool>(swipeDismiss = swipeDismiss)  { ToolsScreen() }
                                entry<Route.UmountManager>(swipeDismiss = swipeDismiss)  { UmountManagerScreen() }
                                entry<Route.UtsView>(swipeDismiss = swipeDismiss)  { UtsViewScreen() }
                                entry<Route.BackupList>(swipeDismiss = swipeDismiss)  { BackupListScreen() }
                                entry<Route.KernelModules>(swipeDismiss = swipeDismiss)  { KernelModulesScreen() }
                                entry<Route.DynamicManagers>(swipeDismiss = swipeDismiss) { DynamicManagersScreen() }
                                entry<Route.Extensions>(swipeDismiss = swipeDismiss) { ExtensionsScreen() }
                                entry<Route.CpuSpoof>(swipeDismiss = swipeDismiss) { CpuSpoofScreen() }
                                entry<Route.VersionSpoof>(swipeDismiss = swipeDismiss) { VersionSpoofScreen() }
                                entry<Route.BuiltinZygisk>(swipeDismiss = swipeDismiss) { BuiltinZygiskScreen() }
                                entry<Route.SuiPermissions>(swipeDismiss = swipeDismiss) { SuiPermissionsScreen() }
                                entry<Route.Authentication>(swipeDismiss = swipeDismiss) { AuthenticationScreen() }
                                entry<Route.Font>(swipeDismiss = swipeDismiss) { FontScreen() }
                                entry<Route.Multimedia>(swipeDismiss = swipeDismiss) { MultimediaScreen() }
                                entry<Route.AiAssistant>(swipeDismiss = swipeDismiss) {
                                    if (!AiFeatureBridge.isAvailable) {
                                        LaunchedEffect(Unit) { navigator.pop() }
                                    } else {
                                        // The assistant is always MD3; inside the Miuix shell it needs
                                        // the MD3 colour scheme of the current appearance settings.
                                        AiAssistantMaterialTheme {
                                            AiFeatureBridge.ChatScreen(
                                                onClose = {
                                                    if (navigator.current() == Route.AiAssistant) navigator.pop()
                                                },
                                                onOpenSettings = { navigator.push(Route.AiSettings) },
                                                onNavigate = { target ->
                                                    when (target) {
                                                        is ManagerAiTarget.Tab -> {
                                                            viewModel.openMainPage(target.pageIndex, target.section)
                                                            navigator.popUntil { it is Route.Main }
                                                        }

                                                        is ManagerAiTarget.Push -> navigator.navigateTo(target.route)
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                                entry<Route.AiSettings>(swipeDismiss = swipeDismiss) {
                                    if (!AiFeatureBridge.isAvailable) {
                                        LaunchedEffect(Unit) { navigator.pop() }
                                    } else {
                                        AiAssistantMaterialTheme {
                                            AiFeatureBridge.SettingsScreen(
                                                onBack = { navigator.pop() },
                                            )
                                        }
                                    }
                                }
                        }
                    }

                    var appContentReady by remember { mutableStateOf(false) }
                    var splashDismissed by remember { mutableStateOf(false) }

                    SideEffect {
                        if (!appContentReady) appContentReady = true
                    }

                    // App Lock (ported from FinaSU, Milestone 2): gate all manager
                    // content behind the stored authentication policy; with no policy
                    // stored, no prompt appears.
                    AppLockGate(authenticationSession, onCancel = { finish() }) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val hasCustomBackground = uiState.customBackgroundUri.isNotEmpty()
                        // FinaSU wallpaper (M5.2) paints instead of Nova's background when
                        // active for this page; Nova's background stays the fallback.
                        if (wallpaperActive) {
                            WallpaperBackgroundLayer(
                                modifier = if (captureAppBackground) {
                                    Modifier.layerBackdrop(appBackgroundBackdrop)
                                } else {
                                    Modifier
                                },
                            )
                        } else {
                        val backgroundBlurRadius = (
                            ImageStorage.BACKGROUND_BLUR_MAX_RADIUS_DP * uiState.customBackgroundBlurPercent / 100f
                        ).dp
                        val backgroundEffects = themedAppSettings.appearance?.background
                        Crossfade(
                            modifier = if (captureAppBackground) {
                                Modifier.layerBackdrop(appBackgroundBackdrop)
                            } else {
                                Modifier
                            },
                            targetState = uiState.customBackgroundUri to uiState.customBackgroundUpdatedAt,
                            label = "global_background",
                        ) { (path, _) ->
                            if (path.isNotEmpty()) {
                                val context = LocalContext.current
                                val stamp = File(path).lastModified()
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(File(path))
                                        .crossfade(true)
                                        .memoryCacheKey("$path@$stamp")
                                        .diskCacheKey("$path@$stamp")
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(
                                            if (backgroundBlurRadius > 0.dp) {
                                                Modifier.blur(backgroundBlurRadius)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        // Sharpen after a blur would just fight the blur, so only
                                        // apply it while the background is unblurred.
                                        .appearanceImageEffects(
                                            backgroundEffects,
                                            allowSharpen = backgroundBlurRadius <= 0.dp,
                                        )
                                )
                                val scrimPercent = uiState.customBackgroundScrimPercent
                                if (scrimPercent != 0) {
                                    val scrimColor = if (scrimPercent < 0) {
                                        Color.Black.copy(alpha = -scrimPercent / 100f)
                                    } else {
                                        Color.White.copy(alpha = scrimPercent / 100f)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(scrimColor)
                                    )
                                }
                            }
                        }
                        }

                        when (uiMode) {
                            UiMode.Material -> androidx.compose.material3.Scaffold(
                                containerColor = if (hasCustomBackground || wallpaperActive) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer
                            ) { navDisplay() }

                            UiMode.Miuix -> Scaffold(
                                containerColor = if (hasCustomBackground || wallpaperActive) Color.Transparent else MiuixTheme.colorScheme.surface
                            ) { navDisplay() }
                        }

                        if (!splashDismissed) {
                            SplashScreen(
                                isReady = appContentReady,
                                onDismissed = { splashDismissed = true }
                            )
                        }
                    }

                    // First-run guide (ported from FinaSU, Milestone 3): show once, on the
                    // first launch after install. Placed inside the AppLockGate so it only
                    // appears after unlock; replayable from Settings -> About.
                    val prefs = remember {
                        ksuApp.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    }
                    var showFirstRunGuide by rememberSaveable {
                        mutableStateOf(
                            savedInstanceState == null &&
                                !prefs.getBoolean("first_run_guide_done", false),
                        )
                    }
                    if (showFirstRunGuide) {
                        FirstRunGuide(
                            onDismiss = {
                                prefs.edit().putBoolean("first_run_guide_done", true).apply()
                                showFirstRunGuide = false
                            },
                        )
                    }
                    }
                    AuthenticationHost(authenticationSession)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentChannel.trySend(intent)
    }
}

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> { error("LocalMainPagerState not provided") }

@RequiresApi(Build.VERSION_CODES.Q)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    initialPage: Int = 0,
    pagerInterceptionMode: Int = PagerInterceptionMode.CrossAxisInterceptor.ordinal,
    onPageChanged: (Int) -> Unit = {},
) {
    val navController = LocalNavigator.current
    val enableBlur = LocalEnableBlur.current
    val enableProgressiveBlur = LocalEnableProgressiveBlur.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val useNavigationRail = useNavigationRail(enableFloatingBottomBar)
    // M5.4: the visible destinations, from the visibility prefs. Home and Setting always
    // stay; SuperUser and Module can be hidden in Settings. The pager holds only these, so
    // hiding a tab shifts the pager indices — the persisted page is a destination ordinal,
    // not a slot, and is mapped onto the visible index here.
    val showNavSuperuser = LocalShowNavSuperuser.current
    val showNavModule = LocalShowNavModule.current
    val destinations = remember(showNavSuperuser, showNavModule) {
        visibleDestinations(showNavSuperuser, showNavModule)
    }
    val initialDestination = BottomBarDestination.entries.getOrElse(initialPage) { BottomBarDestination.Home }
    val initialVisiblePage = destinations.indexOf(initialDestination).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialVisiblePage, pageCount = { destinations.size })
    val mainPagerState = rememberMainPagerState(
        pagerState = pagerState,
        destinations = destinations,
        animatePageChanges = !useNavigationRail,
    )
    // The AI assistant can request a tab from outside MainScreen; follow the
    // selected page when it changes without a pager gesture.
    LaunchedEffect(initialPage) {
        mainPagerState.animateToDestination(initialDestination)
    }
    // Follow a change of the visible destinations: the selected destination keeps its place,
    // and only a destination that was itself hidden falls back to home.
    LaunchedEffect(destinations) {
        mainPagerState.syncToDestinations()
    }
    val isFullFeatured = Natives.isFullFeatured()
    val pagerMode = PagerInterceptionMode.entries.getOrElse(pagerInterceptionMode) {
        PagerInterceptionMode.Native
    }
    val interceptPagerGestures = pagerMode == PagerInterceptionMode.CrossAxisInterceptor
    var userScrollEnabled by remember(isFullFeatured) { mutableStateOf(isFullFeatured) }

    val enableNavigationBadge = LocalEnableNavigationBadge.current
    val badgeEnabled = enableNavigationBadge && isFullFeatured
    val moduleViewModel = viewModel<ModuleViewModel>()
    val moduleUiState by moduleViewModel.uiState.collectAsStateWithLifecycle()

    val superUserViewModel = viewModel<SuperUserViewModel>()
    val grantedUidCount by remember(superUserViewModel) {
        superUserViewModel.uiState
            .map { state -> state.groupedApps.count { it.anyAllowSu } }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(0)

    var startupPreloadStarted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isFullFeatured) {
        if (!isFullFeatured || startupPreloadStarted) {
            return@LaunchedEffect
        }

        moduleViewModel.initializePreferences()
        val moduleState = moduleViewModel.uiState.value
        if (!moduleState.hasLoaded) {
            if (!moduleState.isRefreshing) moduleViewModel.fetchModuleList()
            moduleViewModel.uiState.first { it.hasLoaded }
        }
        moduleViewModel.syncModuleUpdateInfo(moduleViewModel.uiState.value.modules)

        val superUserState = superUserViewModel.uiState.value
        if (!superUserState.hasLoaded) {
            superUserViewModel.initializePreferences()
            if (superUserState.isRefreshing) {
                superUserViewModel.uiState.first { it.hasLoaded }
            } else {
                superUserViewModel.loadAppList().join()
            }
        }

        startupPreloadStarted = true
    }

    // Loading the app list just for a badge is too expensive; read the kernel allowlist instead.
    var superuserCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(badgeEnabled, grantedUidCount) {
        superuserCount = if (badgeEnabled) withContext(Dispatchers.IO) { getSuperuserCount() } else 0
    }

    val navigationBadge = if (badgeEnabled) {
        NavigationBadgeState(
            superuserCount = superuserCount,
            moduleEnabledCount = moduleUiState.modules.count { it.enabled },
            moduleUpdatableCount = moduleUiState.updateInfo.count { it.value.downloadUrl.isNotBlank() },
        )
    } else {
        NavigationBadgeState()
    }
    val uiMode = LocalUiMode.current
    val hasCustomBackground = LocalBackgroundImagePath.current.isNotEmpty()
    // M5.4: the floating bar hides itself while the user scrolls down or after 3 s idle
    // (auto-hide / swipe-hide from Settings → Navigation), and reappears on any interaction.
    // Rail layouts show no bottom bar at all, so the state stays idle there. The bar is
    // hidden with a graphics-layer slide+fade, NOT by removing it from the Scaffold slot:
    // the slot keeps its size, so the pager content never jumps when the bar hides.
    val isFloatingBar = !useNavigationRail &&
        (uiMode == UiMode.Material || enableFloatingBottomBar)
    val floatingAutoHide = LocalFloatingAutoHide.current
    val floatingSwipeHide = LocalFloatingSwipeHide.current
    val floatingVisibility = rememberFloatingBarVisibilityState(
        enabled = isFloatingBar,
        autoHide = floatingAutoHide,
        swipeHide = floatingSwipeHide,
    )
    val floatingBarVisible = when {
        !floatingAutoHide && !floatingSwipeHide -> true
        !floatingAutoHide -> !floatingVisibility.isScrollingDown
        !floatingSwipeHide -> floatingVisibility.visible
        else -> floatingVisibility.visible && !floatingVisibility.isScrollingDown
    }
    val floatingBarAlpha by animateFloatAsState(
        targetValue = if (floatingBarVisible) 1f else 0f,
        label = "floatingBarAlpha",
    )
    // Slide the hidden bar below the screen edge (64 dp pill + padding); the slot keeps its
    // size, so the translated bar can no longer be tapped while hidden.
    val floatingBarHiddenOffsetPx = with(LocalDensity.current) { 96.dp.toPx() }
    val floatingBarOffsetYPx by animateFloatAsState(
        targetValue = if (floatingBarVisible) 0f else floatingBarHiddenOffsetPx,
        label = "floatingBarOffsetY",
    )
    val cardAlpha = LocalCardBackgroundAlpha.current
    val surfaceColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.surfaceContainer // Matches the Material page background behind the glass bar
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
    }
    // This backdrop only feeds the standard Miuix navigation bar. The floating bars use
    // `backdrop` for their liquid glass or progressive band, so the pager is only recorded into
    // a full-screen layer when one of those readers is active.
    val needsStandardMiuixBarBackdrop = uiMode == UiMode.Miuix && !enableFloatingBottomBar &&
        !useNavigationRail
    val blurBackdrop = rememberBlurBackdrop(
        enableBlur = enableBlur && needsStandardMiuixBarBackdrop,
        allowProgressive = needsStandardMiuixBarBackdrop,
    )
    // The floating pills (Material bar, Miuix floating bar) sample this pager layer for their
    // liquid glass and/or progressive blur. Rail layouts show no bottom bar at all.
    val needsBottomBarBackdrop = !useNavigationRail &&
        (uiMode == UiMode.Material || enableFloatingBottomBar) &&
        (enableFloatingBottomBarBlur || enableProgressiveBlur)

    val backdrop = rememberLayerBackdrop {
        if (!hasCustomBackground) {
            drawRect(surfaceColor)
        } else {
            drawRect(surfaceColor.copy(alpha = cardAlpha))
        }
        drawContent()
    }

    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(settledPage) {
        // Persist the destination, not the pager slot: hiding a tab shifts the indices.
        onPageChanged(destinations.getOrNull(settledPage)?.ordinal ?: BottomBarDestination.Home.ordinal)
    }

    val currentPage = mainPagerState.pagerState.currentPage
    LaunchedEffect(currentPage) {
        mainPagerState.syncPage()
    }

    MainScreenBackHandler(mainPagerState, navController)

    CompositionLocalProvider(
        LocalMainPagerState provides mainPagerState,
        // M5.4: navigation-mode signals for page content (e.g. the module FAB reads them to
        // ride with the floating bar). The reserved height is the 64 dp pill + 32 dp of
        // breathing room, matching FinaSU's formula for its 64 dp drawer form; no screen
        // consumes it yet.
        LocalIsFloatingNavMode provides isFloatingBar,
        LocalFloatingBarVisible provides floatingBarVisible,
        LocalFloatingBarReservedHeight provides if (isFloatingBar) 96.dp else 0.dp,
    ) {
        val contentReady = rememberContentReady()
        val pagerContent = @Composable { bottomInnerPadding: Dp ->
            Box(
                modifier = (if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier)
                    // M5.4: the floating bar's auto-hide/swipe-hide watches content scrolling.
                    .then(if (isFloatingBar) Modifier.nestedScroll(floatingVisibility.connection) else Modifier)
            ) {
                HorizontalPager(
                    modifier = Modifier
                        .pagerGestureOverride(
                            pagerState = mainPagerState.pagerState,
                            mode = pagerMode,
                            enabled = userScrollEnabled,
                        )
                        // Material always floats its bar, so only the glass/progressive band matters there.
                        .then(if (needsBottomBarBackdrop) Modifier.layerBackdrop(backdrop) else Modifier),
                    state = mainPagerState.pagerState,
                    beyondViewportPageCount = if (contentReady) 3 else 0,
                    overscrollEffect = null,
                    userScrollEnabled = userScrollEnabled && !interceptPagerGestures,
                    pageNestedScrollConnection = if (interceptPagerGestures) {
                        PagerGestureNestedScrollConnection
                    } else {
                        pageNestedScrollConnection(
                            state = mainPagerState.pagerState,
                            orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                        )
                    },
                    flingBehavior = flingBehavior(
                        state = mainPagerState.pagerState,
                        snapAnimationSpec = PagerNavigationSpringSpec,
                    ),
                ) { page ->
                    val isCurrentPage = page == settledPage
                    when (destinations.getOrNull(page)) {
                        BottomBarDestination.Home -> if (contentReady || isCurrentPage) HomePager(navController, bottomInnerPadding, isCurrentPage)
                        BottomBarDestination.SuperUser -> if (contentReady || isCurrentPage) SuperUserPager(navController, bottomInnerPadding, isCurrentPage)
                        BottomBarDestination.Module -> if (contentReady || isCurrentPage) ModulePager(bottomInnerPadding, isCurrentPage)
                        BottomBarDestination.Setting -> if (contentReady || isCurrentPage) SettingPager(navController, bottomInnerPadding, isCurrentPage)
                        null -> Unit
                    }
                }
            }
        }

        if (useNavigationRail) {
            val startInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Start)
            val navBarBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold(
                    containerColor = if (hasCustomBackground) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row {
                        SideRail(navigationBadge)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navBarBottomPadding)
                        }
                    }
                }

                UiMode.Miuix -> Scaffold(
                    containerColor = if (hasCustomBackground) Color.Transparent else MiuixTheme.colorScheme.surface
                ) { _ ->
                    Row {
                        SideRail(navigationBadge)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navBarBottomPadding)
                        }
                    }
                }
            }
        } else {
            val bottomBar = @Composable {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // M5.4: slide+fade the floating bar out (slot keeps its size, so the
                        // pager never jumps). Only when a hide behaviour is actually on.
                        .then(
                            if (isFloatingBar && (floatingAutoHide || floatingSwipeHide)) {
                                Modifier.graphicsLayer {
                                    alpha = floatingBarAlpha
                                    translationY = floatingBarOffsetYPx
                                }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    BottomBar(
                        blurBackdrop = blurBackdrop,
                        backdrop = backdrop,
                        navigationBadge = navigationBadge,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold(
                    bottomBar = bottomBar,
                    containerColor = if (hasCustomBackground) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer
                ) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }

                UiMode.Miuix -> Scaffold(
                    bottomBar = bottomBar,
                    containerColor = if (hasCustomBackground) Color.Transparent else MiuixTheme.colorScheme.surface
                ) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }
            }
        }
    }
}

@Composable
private fun MainScreenBackHandler(
    mainState: MainPagerState,
    navController: Navigator,
) {
    val isPagerBackHandlerEnabled by remember {
        derivedStateOf {
            navController.current() is Route.Main && navController.backStackSize() == 1 && mainState.selectedDestination != BottomBarDestination.Home
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = {
            mainState.animateToDestination(BottomBarDestination.Home)
        }
    )
}
