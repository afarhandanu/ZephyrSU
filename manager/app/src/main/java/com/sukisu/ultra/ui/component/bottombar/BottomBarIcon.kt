package com.sukisu.ultra.ui.component.bottombar

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        MorphIcon(
            vector = icon,
            selected = selected,
            tint = tint,
            modifier = modifier.size(size).then(semanticsModifier),
        )
    }
}
