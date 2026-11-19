package com.sukisu.ultra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.sukisu.ultra.ui.LocalUiMode
import com.sukisu.ultra.ui.UiMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme

// Miuix's own default accent, used when the Miuix skin runs without Monet or a custom key color.
private val MiuixDefaultSeed = Color(0xFF3482FF)

/**
 * Remap a Material 3 scheme's surface family to the live Miuix palette, so a Material page
 * shown under the Miuix skin reads like a native Miuix screen (KPM): white surfaces in light
 * mode and near-black in dark mode, instead of the Material scheme's lavender-grey
 * (light) / dark-grey (dark) surface roles.
 *
 * Only the surface/background/outline/scrim/error/tertiary-container roles are remapped.
 * The primary/secondary/tertiary accent families stay seed-derived, so the user's key colour
 * is unchanged in both modes. Roles Miuix has no equivalent for are derived from the closest
 * Miuix rung: surfaceContainerLowest/Low from the surface/container ladder, surfaceDim from
 * the surface, surfaceBright from the highest container, onSurfaceVariant from
 * onSurfaceVariantSummary, outlineVariant from the divider line, scrim from window dimming.
 * Inverse/fixed roles have no Miuix equivalent and are kept from the Material scheme.
 */
internal fun ColorScheme.withMiuixSurfaces(miuix: Colors): ColorScheme = copy(
    background = miuix.background,
    onBackground = miuix.onBackground,
    surface = miuix.surface,
    onSurface = miuix.onSurface,
    surfaceVariant = miuix.surfaceVariant,
    onSurfaceVariant = miuix.onSurfaceVariantSummary,
    surfaceDim = miuix.surface,
    surfaceBright = miuix.surfaceContainerHighest,
    surfaceContainerLowest = miuix.surface,
    surfaceContainerLow = lerp(miuix.surface, miuix.surfaceContainer, 0.5f),
    surfaceContainer = miuix.surfaceContainer,
    surfaceContainerHigh = miuix.surfaceContainerHigh,
    surfaceContainerHighest = miuix.surfaceContainerHighest,
    tertiaryContainer = miuix.tertiaryContainer,
    onTertiaryContainer = miuix.onTertiaryContainer,
    outline = miuix.outline,
    outlineVariant = miuix.dividerLine,
    scrim = miuix.windowDimming,
    error = miuix.error,
    onError = miuix.onError,
    errorContainer = miuix.errorContainer,
    onErrorContainer = miuix.onErrorContainer,
)

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
 *
 * The surface family of that scheme is additionally remapped to the live Miuix
 * palette ([withMiuixSurfaces]): without it the page renders Material surface
 * colours (lavender-grey in light mode, dark grey in dark mode) while a native
 * Miuix screen next to it is white / near-black.
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

    // Under the Miuix shell the page must read like a native Miuix screen (KPM):
    // remap the surface family from the live Miuix palette (key colour / Monet /
    // appearance overlay already baked into it). The AMOLED force-black is
    // re-applied after the remap so AMOLED mode keeps its pure-black surfaces.
    // In Material mode the wrapper is never used; the gate keeps it a no-op there.
    val mappedScheme = if (LocalUiMode.current == UiMode.Miuix) {
        colorScheme.withMiuixSurfaces(MiuixTheme.colorScheme)
            .amoledBackground(colorMode.isAmoled)
    } else {
        colorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = mappedScheme.animateAsState(),
        motionScheme = MotionScheme.expressive(),
        shapes = Shapes,
        typography = Typography,
        content = content,
    )
}
