package com.sukisu.ultra.ui.screen.home

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.ui.component.material.TonalCard

/**
 * Ported from FinaSU (M5.3, GridUI home layout) — the `HomeCard` half only.
 *
 * Adaptations for ZephyrSU:
 * - FinaSU's `wallpaperRole` parameter is dropped: ZephyrSU's [TonalCard] has no wallpaper-role
 *   routing, and Nova's home cards stay opaque over the wallpaper (see M5.2 — the
 *   translucent-card migration is future work). The grid hero card still paints a user-chosen
 *   surface photo itself (see `HomeGridLayout`), which is what makes the photo background
 *   visible; only the translucent wallpaper material is not applied here.
 * - `FolkShape.Corner20` is the same 20dp corner, written inline.
 * - `navOriginRadius` does not exist on ZephyrSU's [TonalCard].
 * - `HomeTileCard` (Focus/Dashboard/Stats boards) is NOT ported — those layouts are not in
 *   scope for M5.3.
 */

/** A full-width tonal surface, the one shape every card on the home screen shares. */
@Composable
internal fun HomeCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceBright,
    contentColor: Color = contentColorFor(containerColor),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    TonalCard(
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        onLongClick = onLongClick,
        interactionSource = interactionSource,
        content = content,
    )
}
