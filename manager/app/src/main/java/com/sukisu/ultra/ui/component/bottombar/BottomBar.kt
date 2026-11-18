package com.sukisu.ultra.ui.component.bottombar

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukisu.ultra.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import com.sukisu.ultra.ui.LocalUiMode
import com.sukisu.ultra.ui.UiMode
import com.sukisu.ultra.ui.util.shouldShowSplitPane
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.utils.springAnimateToPage

class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
    private val animatePageChanges: Boolean,
    destinations: List<BottomBarDestination>,
) {
    /** The destinations the pager holds right now, in the order the bar shows them. */
    var destinations by mutableStateOf(destinations)
        internal set

    /**
     * The destination the user is on. It is the source of truth rather than the pager index: a
     * hidden destination shifts the index of every destination after it, so the index alone would
     * lose the user's place when the visible list changes.
     */
    var selectedDestination by mutableStateOf(
        destinations.getOrElse(pagerState.currentPage) { BottomBarDestination.Home }
    )
        private set

    /** Position of [selectedDestination] in the visible destinations, for the selection indicator. */
    val selectedPage: Int get() = destinations.indexOf(selectedDestination).coerceAtLeast(0)

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    /** Navigate by visible pager index (the liquid-glass bar's callback is index-based). */
    fun animateToPage(targetIndex: Int) {
        val destination = destinations.getOrNull(targetIndex) ?: return
        animateToDestination(destination)
    }

    fun animateToDestination(destination: BottomBarDestination) {
        val targetIndex = destinations.indexOf(destination)
        if (targetIndex < 0 || destination == selectedDestination) return

        navJob?.cancel()

        selectedDestination = destination
        isNavigating = true

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                if (animatePageChanges) {
                    pagerState.springAnimateToPage(targetIndex)
                } else {
                    pagerState.scrollToPage(targetIndex)
                }
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        destinations.getOrNull(pagerState.currentPage)?.let { selectedDestination = it }
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (isNavigating) return
        val settled = destinations.getOrNull(pagerState.currentPage) ?: return
        if (settled != selectedDestination) {
            selectedDestination = settled
        }
    }

    /**
     * Follow a change of the visible destinations: the selected destination keeps its place, and
     * only a destination that was itself hidden falls back to home.
     */
    suspend fun syncToDestinations() {
        val index = destinations.indexOf(selectedDestination).coerceAtLeast(0)
        if (index != pagerState.currentPage) {
            pagerState.scrollToPage(index)
        }
        selectedDestination = destinations[index]
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    destinations: List<BottomBarDestination>,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    animatePageChanges: Boolean = true,
): MainPagerState {
    val state = remember(pagerState, coroutineScope, animatePageChanges) {
        MainPagerState(pagerState, coroutineScope, animatePageChanges, destinations)
    }
    state.destinations = destinations
    return state
}

/**
 * The destinations the bottom bar shows, from the visibility prefs (M5.4). Home and Setting
 * always stay; Kpm, Plugin, SuperUser and Module can be hidden.
 */
fun visibleDestinations(showSuperuser: Boolean, showModule: Boolean, showKpm: Boolean, showPlugin: Boolean): List<BottomBarDestination> =
    BottomBarDestination.entries.filter { destination ->
        when (destination) {
            BottomBarDestination.Kpm -> showKpm
            BottomBarDestination.Plugin -> showPlugin
            BottomBarDestination.SuperUser -> showSuperuser
            BottomBarDestination.Module -> showModule
            else -> true
        }
    }

@Immutable
data class NavigationBadgeState(
    val superuserCount: Int = 0,
    val moduleEnabledCount: Int = 0,
    val moduleUpdatableCount: Int = 0,
)

internal enum class BadgeTone { Alert, Accent }

@Immutable
internal data class NavBadge(val count: Int, val tone: BadgeTone)

internal fun badgeFor(index: Int, state: NavigationBadgeState): NavBadge? = when (index) {
    BottomBarDestination.SuperUser.ordinal ->
        state.superuserCount.takeIf { it > 0 }?.let { NavBadge(it, BadgeTone.Accent) }

    BottomBarDestination.Module.ordinal -> when {
        state.moduleUpdatableCount > 0 -> NavBadge(state.moduleUpdatableCount, BadgeTone.Alert)
        state.moduleEnabledCount > 0 -> NavBadge(state.moduleEnabledCount, BadgeTone.Accent)
        else -> null
    }

    else -> null
}

@Composable
fun useNavigationRail(enableFloatingBottomBar: Boolean): Boolean {
    return shouldShowSplitPane() && !(LocalUiMode.current == UiMode.Miuix && enableFloatingBottomBar)
}

@Composable
internal fun rememberBottomBarIconPaths(): Map<String, String> {
    val state by viewModel<SettingsViewModel>().uiState.collectAsStateWithLifecycle()
    return state.bottomBarIcons
}

@Composable
fun BottomBar(
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop,
    navigationBadge: NavigationBadgeState,
    modifier: Modifier = Modifier,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> BottomBarMiuix(blurBackdrop, backdrop, navigationBadge, modifier)
        UiMode.Material -> BottomBarMaterial(backdrop, navigationBadge, modifier)
    }
}

@Composable
fun SideRail(
    navigationBadge: NavigationBadgeState,
    modifier: Modifier = Modifier,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> NavigationRailMiuix(navigationBadge, modifier)
        UiMode.Material -> NavigationRailMaterial(navigationBadge, modifier)
    }
}
