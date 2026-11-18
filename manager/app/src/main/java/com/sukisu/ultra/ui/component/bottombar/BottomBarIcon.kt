package com.sukisu.ultra.ui.component.bottombar

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.io.File

/**
 * Draws a custom bottom bar icon image when [iconPath] points at a real file, otherwise falls
 * back to the built-in [icon] vector.
 *
 * Custom images keep their original colours (no tint) as chosen in the appearance settings, so
 * authors can ship multi-colour icons. The built-in vectors still take [tint].
 */
@Composable
fun BottomBarIconContent(
    icon: ImageVector,
    iconPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 24.dp,
    selected: Boolean = false,
    filledIcon: ImageVector? = null,
) {
    val file = iconPath?.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.isFile }
    if (file != null) {
        Image(
            painter = rememberAsyncImagePainter(model = file),
            contentDescription = contentDescription,
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
        )
    } else if (tint == Color.Unspecified) {
        // No tint to morph with: keep the old untinted vector path.
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
        )
    } else {
        // MorphIcon morphs the filled glyph's hole contours on selection (M5.4, ported from
        // FinaSU); it falls back to a plain Icon for glyphs it cannot parse. It draws on a
        // Canvas, so the content description is attached via semantics here.
        val semanticsModifier = if (contentDescription != null) {
            Modifier.semantics { this.contentDescription = contentDescription }
        } else {
            Modifier
        }
        val animationRevision by NavIconAnimationConfig.revision.collectAsState()
        val animation = remember(animationRevision) { NavIconAnimationConfig.animation }
        if (animation == NavIconAnimation.Fade && filledIcon != null) {
            FilledNavIcon(
                outlined = icon,
                filled = filledIcon,
                selected = selected,
                tint = tint,
                modifier = modifier.size(size).then(semanticsModifier),
            )
        } else {
            MorphIcon(
                vector = icon,
                selected = selected,
                tint = tint,
                modifier = modifier.size(size).then(semanticsModifier),
            )
        }
    }
}

/**
 * Switches between the outlined and the real filled Material glyph.
 *
 * Material's outlined and filled icons have structurally different path data, so a literal path
 * interpolation is not possible; the switch is animated as a short cross-fade with a subtle
 * scale so it still reads as a single transformation.
 *
 * Ported from FinaSU 60a6572 (NavBarIcon.kt).
 */
@Composable
private fun FilledNavIcon(
    outlined: ImageVector,
    filled: ImageVector,
    selected: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "navIconFill",
    )
    Box(modifier) {
        if (progress < 1f) {
            Icon(
                imageVector = outlined,
                contentDescription = null,
                tint = tint.copy(alpha = tint.alpha * (1f - progress)),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = 1f - OUTLINED_SCALE_LOSS * progress
                        scaleX = scale
                        scaleY = scale
                    },
            )
        }
        if (progress > 0f) {
            Icon(
                imageVector = filled,
                contentDescription = null,
                tint = tint.copy(alpha = tint.alpha * progress),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = 1f - OUTLINED_SCALE_LOSS * (1f - progress)
                        scaleX = scale
                        scaleY = scale
                    },
            )
        }
    }
}

private const val OUTLINED_SCALE_LOSS = 0.12f
