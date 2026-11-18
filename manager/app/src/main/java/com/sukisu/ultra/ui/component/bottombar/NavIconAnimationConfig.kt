package com.sukisu.ultra.ui.component.bottombar

import android.content.Context
import androidx.core.content.edit
import com.sukisu.ultra.ksuApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How the selected navigation icon changes.
 *
 * [Morph] keeps the outlined glyph and collapses its enclosed contours, which reads as the icon
 * filling in. [Fade] cross-fades into the real filled Material glyph with a slight scale.
 *
 * Ported from FinaSU 60a6572 (manager: Refresh the splash screen and add a nav icon
 * animation picker), package-adapted. The splash-screen half of that commit was not
 * ported: ZephyrSU keeps its own Wind Spirit splash artwork.
 */
enum class NavIconAnimation {
    Morph, Fade;

    val storageValue: String get() = name.lowercase()

    companion object {
        fun fromValue(value: String?): NavIconAnimation =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: Morph
    }
}

/**
 * Persisted icon animation. [revision] bumps on every write so the bar icons recompose immediately.
 */
object NavIconAnimationConfig {

    private const val PREFS = "settings"
    private const val KEY_ANIMATION = "nav_icon_animation"

    private val prefs get() = ksuApp.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    var animation: NavIconAnimation
        get() = NavIconAnimation.fromValue(prefs.getString(KEY_ANIMATION, null))
        set(value) {
            prefs.edit { putString(KEY_ANIMATION, value.storageValue) }
            _revision.value++
        }
}
