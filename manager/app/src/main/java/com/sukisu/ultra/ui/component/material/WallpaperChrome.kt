package com.sukisu.ultra.ui.component.material

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.wallpaper.LocalFolkWallpaperTokens
import com.sukisu.ultra.wallpaper.WallpaperConfig
import com.sukisu.ultra.wallpaper.WallpaperMaterial

/** Which screen edge a chrome zone hugs. */
enum class ChromeEdge { Top, Bottom, Start }

/**
 * How opaque a collapsing top bar becomes once it is fully collapsed, no matter what transparency
 * the user picked for the content surfaces.
 */
private const val CollapsedChromeFloor = 0.85f

/** Alpha of the hairline drawn along the top edge of a bottom bar in wallpaper mode. */
private const val BottomDividerAlpha = 0.22f

/**
 * Draws the wallpaper reading platform behind a bar and fades it into the wallpaper *past* the
 * bar's edge, so the bar no longer terminates in a hard color band while its own content (which
 * may sit at the bottom of a large top bar) stays on the solid platform.
 *
 * Only active in wallpaper mode; otherwise [content] is emitted unchanged. [progress] lets a
 * collapsible bar strengthen its platform as it collapses (0 = expanded, 1 = collapsed).
 */
@Composable
fun WallpaperChromeZone(
    edge: ChromeEdge,
    modifier: Modifier = Modifier,
    progress: () -> Float = { 0f },
    tailHeight: Dp = 48.dp,
    material: WallpaperMaterial? = null,
    content: @Composable () -> Unit,
) {
    val tokens = LocalFolkWallpaperTokens.current
    if (tokens == null) {
        content()
        return
    }
    val chrome = material ?: tokens.chrome
    val tailPx = with(LocalDensity.current) { tailHeight.toPx() }
    val layoutDirection = LocalLayoutDirection.current
    // User knob for how strong the bar shade is (1 = as designed, 0 = none).
    val shade = WallpaperConfig.barShade
    val borderStyle = WallpaperConfig.barBorderStyle
    val dividerPx = with(LocalDensity.current) { 1.dp.toPx() }
    val divider = chrome.contentColor.copy(alpha = BottomDividerAlpha)
    Box(
        modifier = modifier.drawBehind {
            val collapsed = progress().coerceIn(0f, 1f)
            // The bar honours the user's transparency while it is expanded; once it collapses,
            // scrolled content passes underneath, so the platform strengthens to at least
            // [CollapsedChromeFloor] to keep the labels legible. A bottom bar never collapses and
            // therefore keeps the user's alpha as-is.
            val floor = if (edge == ChromeEdge.Top) CollapsedChromeFloor else chrome.alpha
            val alpha = chrome.alpha + (floor - chrome.alpha).coerceAtLeast(0f) * collapsed
            val solid = chrome.tint.copy(alpha = alpha.coerceAtMost(1f) * shade)
            val clear = solid.copy(alpha = 0f)
            val width = size.width
            when (edge) {
                ChromeEdge.Top -> {
                    drawRect(solid, topLeft = Offset.Zero, size = Size(width, size.height))
                    // Fade past the bottom edge, over the (transparent) page canvas below.
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(solid, clear),
                            startY = size.height,
                            endY = size.height + tailPx,
                        ),
                        topLeft = Offset(0f, size.height),
                        size = Size(width, tailPx),
                    )
                }

                ChromeEdge.Bottom -> {
                    // Fade past the top edge, over the (transparent) page canvas above.
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(clear, solid),
                            startY = -tailPx,
                            endY = 0f,
                        ),
                        topLeft = Offset(0f, -tailPx),
                        size = Size(width, tailPx),
                    )
                    drawRect(solid, topLeft = Offset.Zero, size = Size(width, size.height))
                    // Border marking where the bottom bar starts, in the shape the user picked.
                    val stroke = dividerPx * 1.5f
                    when (borderStyle) {
                        WallpaperConfig.BAR_BORDER_LINE ->
                            drawRect(divider, topLeft = Offset.Zero, size = Size(width, dividerPx))

                        WallpaperConfig.BAR_BORDER_DASHED -> drawLine(
                            color = divider,
                            start = Offset.Zero,
                            end = Offset(width, 0f),
                            strokeWidth = stroke,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(dividerPx * 10f, dividerPx * 6f),
                            ),
                        )

                        WallpaperConfig.BAR_BORDER_DOTTED -> drawLine(
                            color = divider,
                            start = Offset.Zero,
                            end = Offset(width, 0f),
                            strokeWidth = dividerPx * 3f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(0f, dividerPx * 9f),
                            ),
                        )

                        WallpaperConfig.BAR_BORDER_WAVE, WallpaperConfig.BAR_BORDER_ZIGZAG -> {
                            val half = dividerPx * 10f
                            val amp = dividerPx * 3f
                            val wave = borderStyle == WallpaperConfig.BAR_BORDER_WAVE
                            val path = Path().apply {
                                moveTo(0f, 0f)
                                var x = 0f
                                var up = true
                                while (x < width) {
                                    val peak = if (up) -amp else amp
                                    if (wave) {
                                        quadraticTo(x + half / 2f, peak * 2f, x + half, 0f)
                                    } else {
                                        lineTo(x + half / 2f, peak)
                                        lineTo(x + half, 0f)
                                    }
                                    x += half
                                    up = !up
                                }
                            }
                            drawPath(path, divider, style = Stroke(width = stroke))
                        }

                        else -> Unit
                    }
                }

                ChromeEdge.Start -> {
                    // A side rail fills the start edge; fade past its far edge (right in LTR, left in
                    // RTL) over the (transparent) page canvas, so the rail does not end in a hard band.
                    val rtl = layoutDirection == LayoutDirection.Rtl
                    drawRect(solid, topLeft = Offset.Zero, size = Size(width, size.height))
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = if (rtl) listOf(clear, solid) else listOf(solid, clear),
                            startX = if (rtl) -tailPx else width,
                            endX = if (rtl) 0f else width + tailPx,
                        ),
                        topLeft = Offset(if (rtl) -tailPx else width, 0f),
                        size = Size(tailPx, size.height),
                    )
                }
            }
        }
    ) {
        content()
    }
}
