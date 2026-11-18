package com.sukisu.ultra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

// Miuix's own default accent, used when the Miuix skin runs without Monet or a custom key color.
private val MiuixDefaultSeed = Color(0xFF3482FF)

/**
 * Material 3 Expressive theme for Material pages shown under the Miuix skin.
 *
 * The Miuix skin provides no Material theme above the navigation host, so without
 * this wrapper MaterialTheme.colorScheme falls back to Material 3's default LIGHT
 * scheme — every Material page (ExpressiveScaffold's surfaceContainer, cards,
 * text) renders white in dark mode. The wrapper rebuilds the same dark/light
 * scheme Material mode uses (key color / Monet / Miuix default seed), so the page
 * reads dark like Nova's own Miuix screens. Material mode is untouched.
 * (Hoisted verbatim from AboutScreen, which was the first screen to need it.)
 */
@Composable
fun MiuixSkinMaterialTheme(content: @Composable () -> Unit) {
    val appSettings = remember { ThemeController.getAppSettings() }
    val colorMode = appSettings.colorMode
    val darkTheme = colorMode.isDark || (colorMode.isSystem && isSystemInDarkTheme())
    val seedColor = when {
        appSettings.keyColor != 0 -> Color(appSettings.keyColor)
        colorMode.isMonet -> Color.Unspecified
        else -> MiuixDefaultSeed
    }
    val colorScheme = rememberKernelSUColorScheme(
        seedColor = seedColor,
        isDark = darkTheme,
        isAmoled = colorMode.isAmoled,
        paletteStyle = appSettings.paletteStyle,
        colorSpec = appSettings.colorSpec,
    )

    MaterialExpressiveTheme(
        colorScheme = colorScheme.animateAsState(),
        motionScheme = MotionScheme.expressive(),
        shapes = Shapes,
        typography = Typography,
        content = content,
    )
}
