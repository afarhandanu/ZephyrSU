package com.sukisu.ultra.ui.screen.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.sukisu.ultra.ui.theme.isInDarkTheme
import com.sukisu.ultra.wallpaper.LocalFolkWallpaperTokens
import com.sukisu.ultra.wallpaper.surface.SurfaceConfig

/** Ported from FinaSU (M5.3, GridUI home layout). */

/** Which home layout owns the work (status) card. */
enum class HomeWorkCardLayout {
    Circle,
    Grid,
}

/**
 * The resolved surface for a home work card.
 *
 * Adaptation for ZephyrSU: FinaSU's `wallpaperRole` is dropped. It routes the card through
 * FinaSU's wallpaper-aware card, but ZephyrSU's `TonalCard` has no such routing and Nova's
 * home cards stay opaque over the wallpaper (see M5.2 — the translucent-card migration is
 * future work). The photo-background case below keeps working: a transparent container lets
 * the bitmap the card paints itself show through.
 *
 * [workCardBackgroundUri] is non-null only for the grid layout when the user opted into a photo
 * background. In that case [containerColor] is transparent and the card paints the image itself.
 */
@Immutable
data class HomeWorkCardStyle(
    val containerColor: Color,
    val contentColor: Color?,
    val workCardBackgroundUri: String? = null,
)

/**
 * Unified control core for the home screen work card.
 *
 * Each home layout keeps its own responsibility in [circleStyle] / [gridStyle]; callers only pass
 * their layout and the current state. This is the single place where the work card's surface is
 * decided, so adding a layout means adding one branch instead of editing that layout's card.
 */
object HomeWorkCardControl {

    @Composable
    fun style(
        layout: HomeWorkCardLayout,
        working: Boolean,
        surface: SurfaceConfig? = null,
        photo: SurfacePhoto = SurfacePhoto.None,
    ): HomeWorkCardStyle = when (layout) {
        HomeWorkCardLayout.Circle -> circleStyle(working)

        HomeWorkCardLayout.Grid -> gridStyle(working, surface, photo)
    }

    /**
     * CircleUI, plus the status card of every layout that reads as a list of tiles (Focus,
     * Dashboard and Stats). Outside wallpaper mode the card keeps its semantic accent pair; in
     * wallpaper mode it drops to the shared neutral material, because an alpha-reduced accent
     * container loses the contrast of its paired on-colour once a photo shows through.
     */
    @Composable
    private fun circleStyle(working: Boolean): HomeWorkCardStyle {
        val containerColor = if (working) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        }
        return if (LocalFolkWallpaperTokens.current != null) {
            HomeWorkCardStyle(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            HomeWorkCardStyle(
                containerColor = containerColor,
                contentColor = null,
            )
        }
    }

    /**
     * GridUI. Outside wallpaper mode the card keeps its semantic accent pair. In wallpaper mode it
     * joins the shared neutral material. When the user enabled a work-card photo, the surface drops
     * to a transparent container with white content so the bitmap painted by the card shows through.
     */
    @Composable
    private fun gridStyle(working: Boolean, surface: SurfaceConfig?, photo: SurfacePhoto): HomeWorkCardStyle {
        if (surface?.hasImage == true && photo.ready) {
            return HomeWorkCardStyle(
                containerColor = Color.Transparent,
                contentColor = photo.contentColor(surface, isInDarkTheme()),
                workCardBackgroundUri = surface.imageUri,
            )
        }
        val containerColor = if (working) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        }
        return if (LocalFolkWallpaperTokens.current != null) {
            HomeWorkCardStyle(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            HomeWorkCardStyle(
                containerColor = containerColor,
                contentColor = null,
            )
        }
    }
}
