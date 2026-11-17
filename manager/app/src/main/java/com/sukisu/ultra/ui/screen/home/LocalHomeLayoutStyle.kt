package com.sukisu.ultra.ui.screen.home

import androidx.compose.runtime.staticCompositionLocalOf
import com.sukisu.ultra.data.model.HomeLayoutStyle

/**
 * The active home layout token, provided once at the top of the app so switching the preference
 * recomposes the home screen without a restart.
 */
val LocalHomeLayoutStyle = staticCompositionLocalOf { HomeLayoutStyle.DEFAULT }
