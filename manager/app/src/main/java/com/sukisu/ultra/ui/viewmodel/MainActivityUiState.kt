package com.sukisu.ultra.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.sukisu.ultra.ui.UiMode
import com.sukisu.ultra.ui.theme.AppSettings

@Immutable
data class MainActivityUiState(
    val appSettings: AppSettings,
    val pageScale: Float,
    val enableBlur: Boolean,
    val enableProgressiveBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val enableNavigationBadge: Boolean,
    val enableSwipeDismiss: Boolean,
    val pagerInterceptionMode: Int,
    val moduleDescriptionMaxLines: Int = 4,
    
    val customBackgroundUri: String = "",
    val customBackgroundUpdatedAt: Long = 0L,
    val cardBackgroundAlphaPercent: Int = 60,
    val customBackgroundBlurPercent: Int = 50,
    val customBackgroundScrimPercent: Int = -35,
    
    val supportLinksStyle: String = "",
    /** Active home layout token, see HomeLayoutStyle (M5.3). */
    val homeLayoutStyle: String = com.sukisu.ultra.data.model.HomeLayoutStyle.DEFAULT,
    /** M5.4: show the SuperUser / Module tabs in the bottom bar / rail. */
    val showNavSuperuser: Boolean = true,
    val showNavKpm: Boolean = true,
    val showNavModule: Boolean = true,
    /** M5.4: floating-bar auto-hide after 3 s idle / hide while scrolling down. */
    val floatingAutoHide: Boolean = true,
    val floatingSwipeHide: Boolean = true,
    val supportLinksCustomTitle: String = "",
    val supportLinksCustomSubtitle: String = "",
    val supportLinksCustomUrl: String = "",

    val keyColorOverride: Int? = null,
    val uiMode: UiMode,
    val customTexts: Map<String, String> = emptyMap(),
)
