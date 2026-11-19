package com.sukisu.ultra.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.PaletteStyle
import com.sukisu.ultra.media.VisualMediaConfig
import com.sukisu.ultra.ui.LocalUiMode
import com.sukisu.ultra.wallpaper.LocalFolkWallpaperTokens
import com.sukisu.ultra.wallpaper.LocalWallpaperDim
import com.sukisu.ultra.wallpaper.LocalWallpaperPage
import com.sukisu.ultra.wallpaper.WallpaperConfig
import com.sukisu.ultra.wallpaper.adaptColorScheme
import com.sukisu.ultra.wallpaper.guardedDim
import com.sukisu.ultra.wallpaper.resolveFolkWallpaperTokens
import com.sukisu.ultra.wallpaper.useDarkNeutral
import com.sukisu.ultra.ui.UiMode
import com.sukisu.ultra.ui.webui.MonetColorsProvider
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Wraps assistant entry points that can render inside the Miuix shell. In the
 * Material shell the outer theme already is MD3, so this only adds a nested
 * theme under Miuix rather than re-deriving the palette on every recomposition.
 */
@Composable
fun AiAssistantMaterialTheme(
    uiMode: UiMode = LocalUiMode.current,
    content: @Composable () -> Unit,
) {
    when (uiMode) {
        UiMode.Material -> content()
        UiMode.Miuix -> MaterialExpressivePalette(content)
    }
}

/**
 * Material 3 Expressive colours for pages that stay MD3 inside the Miuix shell
 * (the AI assistant, theme store and source management). Same pipeline as
 * [MaterialKernelSUTheme] so light/dark, AMOLED, key colour and monet follow the
 * manager's appearance settings, without touching the window bars the outer
 * shell already owns.
 */
@Composable
fun MaterialExpressivePalette(
    content: @Composable () -> Unit,
) {
    val fontFamily = FontConfig.getFontFamily(LocalContext.current)
    val typography = remember(fontFamily) { Typography.withFontFamily(fontFamily) }
    val appSettings = LocalAppSettings.current ?: ThemeController.getAppSettings()
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = appSettings.colorMode.isDark || (appSettings.colorMode.isSystem && systemDarkTheme)
    val colorScheme = rememberKernelSUColorScheme(
        seedColor = if (appSettings.keyColor == 0) Color.Unspecified else Color(appSettings.keyColor),
        isDark = darkTheme,
        isAmoled = appSettings.colorMode.isAmoled,
        paletteStyle = appSettings.paletteStyle,
        colorSpec = appSettings.colorSpec,
    )
    val overlayTokens = appSettings.appearance?.colorsFor(darkTheme)
    val overlaidScheme = remember(colorScheme, overlayTokens) {
        colorScheme.withAppearance(overlayTokens)
    }
    // Under the Miuix shell the page must read like a native Miuix screen (KPM):
    // remap the surface family from the live Miuix palette (key colour / Monet /
    // appearance overlay already baked into it). The AMOLED force-black is
    // re-applied after the remap so AMOLED mode keeps its pure-black surfaces.
    // Material mode is untouched: the gate keeps the palette exactly as before.
    // (This also covers the AI assistant pages: they stay MD3 in layout and
    // components, only their surfaces now match the Miuix shell around them.)
    val finalScheme = if (LocalUiMode.current == UiMode.Miuix) {
        overlaidScheme.withMiuixSurfaces(MiuixTheme.colorScheme)
            .amoledBackground(appSettings.colorMode.isAmoled)
    } else {
        overlaidScheme
    }
    MaterialExpressiveTheme(
        colorScheme = finalScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = Shapes,
        typography = typography,
        content = content,
    )
}

@Composable
fun MaterialKernelSUTheme(
    appSettings: AppSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val fontFamily = FontConfig.getFontFamily(context)
    val typography = remember(fontFamily) { Typography.withFontFamily(fontFamily) }
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = appSettings.colorMode.isDark || (appSettings.colorMode.isSystem && systemDarkTheme)
    val amoledMode = appSettings.colorMode.isAmoled
    val dynamicColor = appSettings.keyColor == 0

    // FinaSU wallpaper (M5.2): the accent palette is derived from one fixed image, never
    // from the page currently on screen — a page swipe must not re-theme every component
    // (`animateAsState` would then drag the whole app through a color transition and make
    // the swipe feel laggy). Only the background itself and its content polarity follow
    // the page. ZephyrSU's AppSettings has no `useSystemDynamicColor`/`colorGenerationMode`/
    // `contrastLevel`, so those FinaSU inputs are dropped (dynamic color is `keyColor == 0`).
    val wallpaperPage = LocalWallpaperPage.current
    val videoActive = WallpaperConfig.enabled &&
        VisualMediaConfig.let { it.videoEnabled && it.file(context, true) != null }
    val wallpaperActive = WallpaperConfig.isPageActive(wallpaperPage, videoActive)
    val seedPage = if (WallpaperConfig.multiBackgroundEnabled) WallpaperConfig.PAGE_HOME else wallpaperPage
    val pageSeed = WallpaperConfig.pageSeed(seedPage)
    val wallpaperSeed = if (
        WallpaperConfig.isActive && WallpaperConfig.useWallpaperColor &&
        pageSeed != 0
    ) {
        Color(pageSeed)
    } else {
        Color.Unspecified
    }
    val baseSeed = when {
        wallpaperSeed != Color.Unspecified -> wallpaperSeed
        dynamicColor -> Color.Unspecified
        else -> Color(appSettings.keyColor)
    }

    val colorScheme = rememberKernelSUColorScheme(
        seedColor = baseSeed,
        isDark = darkTheme,
        isAmoled = amoledMode,
        paletteStyle = appSettings.paletteStyle,
        colorSpec = appSettings.colorSpec,
    )

    // In wallpaper mode the neutral roles follow the wallpaper's intrinsic brightness so text stays
    // legible; a dark theme never flips to light, readability there is left to the night dim. The
    // scrim is intentionally not an input here: dragging the day dim must never invert polarity.
    val wallpaperDim = WallpaperConfig.effectiveDim(darkTheme)
    val darkNeutral = useDarkNeutral(darkTheme, WallpaperConfig.pageLuminance(wallpaperPage))
    val neutralScheme = rememberKernelSUColorScheme(
        seedColor = baseSeed,
        isDark = darkNeutral,
        isAmoled = false,
        paletteStyle = PaletteStyle.Neutral,
        colorSpec = appSettings.colorSpec,
    )
    val chosenNeutral = if (wallpaperActive && darkNeutral != darkTheme) neutralScheme else colorScheme
    val adaptedColorScheme = adaptColorScheme(
        base = colorScheme,
        neutral = chosenNeutral,
        active = wallpaperActive,
    )
    // Animate the scheme once, then derive both the MaterialTheme palette and the wallpaper tokens
    // from the same animated instance. Otherwise a panel's fill/content comes from the target scheme
    // while the text inside it is still interpolating, leaving a transient low-contrast frame.
    val animatedColorScheme = adaptedColorScheme.animateAsState()
    val wallpaperTokens = if (wallpaperActive) {
        resolveFolkWallpaperTokens(animatedColorScheme, WallpaperConfig.opacity)
    } else {
        null
    }
    val renderDim = if (wallpaperActive && darkNeutral && !darkTheme) {
        guardedDim(wallpaperDim, adaptedColorScheme) ?: wallpaperDim
    } else {
        wallpaperDim
    }.takeIf { wallpaperActive }

    // In wallpaper mode the top of the screen is the wallpaper itself, so the system-bar icon
    // brightness must follow the adapted content brightness, not just the theme mode.
    val systemBarDark = darkTheme || (wallpaperActive && darkNeutral)

    LaunchedEffect(systemBarDark) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !systemBarDark
            isAppearanceLightNavigationBars = !systemBarDark
        }
    }

    val hasCustomBackground = LocalBackgroundImagePath.current.isNotEmpty()
    val cardAlpha = LocalCardBackgroundAlpha.current
    // Default Card/Surface containers bypass backgroundAwareColor(), so apply the card opacity
    // to the card surface roles themselves. Menu/dialog/sheet tokens stay opaque: making them
    // translucent lets their elevation shadow show through (a dark rectangular halo behind the
    // rounded popup).
    val surfaceAwareColorScheme = remember(animatedColorScheme, hasCustomBackground, cardAlpha) {
        if (!hasCustomBackground) {
            animatedColorScheme
        } else {
            animatedColorScheme.copy(
                surfaceBright = animatedColorScheme.surfaceBright.copy(alpha = cardAlpha),
                surfaceContainerLowest = animatedColorScheme.surfaceContainerLowest.copy(alpha = cardAlpha),
                surfaceContainerHighest = animatedColorScheme.surfaceContainerHighest.copy(alpha = cardAlpha),
            )
        }
    }
    val overlayTokens = appSettings.appearance?.colorsFor(darkTheme)
    val overlaidScheme = remember(surfaceAwareColorScheme, overlayTokens) {
        surfaceAwareColorScheme.withAppearance(overlayTokens)
    }

    MaterialExpressiveTheme(
        colorScheme = overlaidScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = Shapes,
        typography = typography,
        content = {
            MonetColorsProvider.UpdateCss(overlaidScheme)
            CompositionLocalProvider(
                LocalWallpaperDim provides renderDim,
                LocalFolkWallpaperTokens provides wallpaperTokens,
            ) {
                content()
            }
        }
    )
}
