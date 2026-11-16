package com.sukisu.ultra.ui.screen.home

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.sukisu.ultra.data.model.SupportLinksStyle

/**
 * The user-entered contents of the `SupportLinksStyle.CUSTOM` home card.
 *
 * All three fields are required: the card renders only when every one of them is non-blank, so a
 * half-configured card silently behaves like [SupportLinksStyle.HIDDEN] instead of showing a
 * broken row.
 *
 * Ported from FinaSU (`me.weishu.kernelsu.ui.screen.home.LocalSupportLinksCustom`).
 */
@Immutable
data class SupportLinksCustomContent(
    val title: String = "",
    val subtitle: String = "",
    val url: String = "",
) {
    /** Whether the card has enough content to be drawn. */
    val isRenderable: Boolean
        get() = title.isNotBlank() && subtitle.isNotBlank() && url.isNotBlank()
}

/**
 * The active bottom-card style token, provided once at the top of the app so switching the
 * preference recomposes the home screen without a restart.
 */
val LocalSupportLinksStyle = staticCompositionLocalOf { SupportLinksStyle.DEFAULT }

/**
 * The active custom bottom-card content, provided once at the top of the app so editing the
 * preference recomposes the home screen without a restart.
 */
val LocalSupportLinksCustom = staticCompositionLocalOf { SupportLinksCustomContent() }

/** Whether [value] is a link the home card can safely hand to the browser. */
internal fun isSupportedLinkUrl(value: String): Boolean =
    Uri.parse(value.trim()).scheme?.lowercase() in setOf("http", "https")
