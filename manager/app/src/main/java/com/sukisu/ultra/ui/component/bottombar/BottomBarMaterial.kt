package com.sukisu.ultra.ui.component.bottombar

import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixLocalContentColor
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import com.sukisu.ultra.ui.theme.LocalEnableFloatingBottomBarBlur
import com.sukisu.ultra.ui.theme.LocalEnableProgressiveBlur
import com.sukisu.ultra.ui.component.FloatingBottomBarItem
import com.sukisu.ultra.ui.component.FloatingBottomBar
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.gestures.detectTapGestures
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.Natives
import com.sukisu.ultra.ui.LocalMainPagerState
import com.sukisu.ultra.ui.component.material.ChromeEdge
import com.sukisu.ultra.ui.component.material.WallpaperChromeZone
import com.sukisu.ultra.ui.theme.LocalMaterialBottomBarDocked
import com.sukisu.ultra.ui.theme.backgroundAwareColor
import com.sukisu.ultra.ui.util.CustomTextKey
import com.sukisu.ultra.ui.util.customText
import com.sukisu.ultra.ui.util.rememberProgressiveBarBlurModifier

@Composable
fun BottomBarMaterial(
    backdrop: Backdrop,
    navigationBadge: NavigationBadgeState,
    modifier: Modifier = Modifier,
) {
    val fullFeatured = Natives.isFullFeatured()
    if (!fullFeatured) return

    val mainPagerState = LocalMainPagerState.current
    val bottomBarIcons = rememberBottomBarIconPaths()
    val progressiveBlur = LocalEnableProgressiveBlur.current && isRuntimeShaderSupported()

    // Items follow the visible destinations (M5.4): hiding SuperUser or Module in Settings
    // drops that tab from the bar and the pager.
    val items = mainPagerState.destinations.map { destination ->
        when (destination) {
            BottomBarDestination.Home -> MaterialNavItem(CustomTextKey.PAGE_HOME, Icons.Filled.Home, Icons.Outlined.Home, bottomBarIcons["home"])
            BottomBarDestination.Kpm -> MaterialNavItem(CustomTextKey.PAGE_KPM, Icons.Filled.Memory, Icons.Outlined.Memory, bottomBarIcons["kpm"])
            BottomBarDestination.Plugin -> MaterialNavItem(CustomTextKey.PAGE_PLUGIN, Icons.Filled.GridView, Icons.Outlined.GridView, bottomBarIcons["plugin"])
            BottomBarDestination.SuperUser -> MaterialNavItem(CustomTextKey.PAGE_SUPERUSER, Icons.Filled.Shield, Icons.Outlined.Shield, bottomBarIcons["superuser"])
            BottomBarDestination.Module -> MaterialNavItem(CustomTextKey.PAGE_MODULES, Icons.Filled.Extension, Icons.Outlined.Extension, bottomBarIcons["module"])
            BottomBarDestination.Setting -> MaterialNavItem(CustomTextKey.PAGE_SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings, bottomBarIcons["setting"])
        }
    }

    // Docked (pinned) variant: a full-width bar on the bottom edge instead of the floating
    // pill. The Scaffold's bottomBar slot reserves its height for the pager, so no content
    // hides behind it. The chrome zone draws the wallpaper reading platform in wallpaper
    // mode; outside it the bar keeps its opaque container.
    if (LocalMaterialBottomBarDocked.current) {
        DockedBottomBarMaterial(items, navigationBadge, modifier)
        return
    }

    if (LocalEnableFloatingBottomBarBlur.current && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LiquidBottomBarMaterial(
            backdrop = backdrop,
            items = items,
            navigationBadge = navigationBadge,
            modifier = modifier,
            progressiveBlur = progressiveBlur,
        )
        return
    }

    // Pages already reserve bottomInnerPadding, so the pill floats over scrolled content.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        val pillColor = backgroundAwareColor(MaterialTheme.colorScheme.surfaceContainerHighest)
        val progressiveModifier = if (progressiveBlur) {
            rememberProgressiveBarBlurModifier(
                backdrop = backdrop,
                gradient = ProgressiveBlur.Bottom,
                tint = MaterialTheme.colorScheme.surfaceContainerHighest,
                tintAlpha = 0.2f,
                shape = CircleShape,
            )
        } else {
            Modifier
        }
        Surface(
            modifier = Modifier.widthIn(max = 480.dp).then(progressiveModifier),
            shape = CircleShape,
            // A translucent sheet lets the progressive blur below the pill stay visible while
            // keeping the labels readable; the blur carries the edge gradient.
            color = if (progressiveBlur) pillColor.copy(alpha = 0.5f) else pillColor,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    val selected = mainPagerState.selectedPage == index
                    // Badges key off the destination, not the visible index: hiding a tab
                    // shifts every index after it.
                    val destination = mainPagerState.destinations.getOrNull(index)
                    FloatingNavItem(
                        selected = selected,
                        label = customText(item.label),
                        icon = if (selected) item.selectedIcon else item.unselectedIcon,
                        iconPath = item.iconPath,
                        badge = destination?.let { badgeFor(it.ordinal, navigationBadge) },
                        progressiveBlur = progressiveBlur,
                        onClick = {
                            if (!selected) {
                                mainPagerState.animateToPage(index)
                            }
                        },
                    )
                }
            }
        }
    }
}

// Docked variant: the standard full-width NavigationBar pinned to the bottom edge.
// The selection pill follows BottomBarControl: opaque secondary container normally,
// transparent in wallpaper mode (the selected item already switches to the filled icon,
// so a second background block would only re-introduce a floating layer over the photo).
@Composable
private fun DockedBottomBarMaterial(
    items: List<MaterialNavItem>,
    navigationBadge: NavigationBadgeState,
    modifier: Modifier = Modifier,
) {
    val mainPagerState = LocalMainPagerState.current
    val style = BottomBarControl.style(BottomBarLayout.Docked)
    WallpaperChromeZone(edge = ChromeEdge.Bottom) {
        NavigationBar(
            modifier = modifier.fillMaxWidth(),
            containerColor = style.containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
        ) {
            items.forEachIndexed { index, item ->
                val selected = mainPagerState.selectedPage == index
                // Badges key off the destination, not the visible index: hiding a tab
                // shifts every index after it.
                val destination = mainPagerState.destinations.getOrNull(index)
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            mainPagerState.animateToPage(index)
                        }
                    },
                    icon = {
                        NavigationIconWithBadge(
                            icon = if (selected) item.selectedIcon else item.unselectedIcon,
                            iconPath = item.iconPath,
                            contentDescription = customText(item.label),
                            badge = destination?.let { badgeFor(it.ordinal, navigationBadge) },
                            selected = selected,
                        )
                    },
                    label = { Text(text = customText(item.label)) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = style.indicatorColor
                            ?: MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
            }
        }
    }
}

// Liquid glass variant: reuses the Miuix FloatingBottomBar (lens, highlight, draggable indicator)
// with Material colors.
@Composable
private fun LiquidBottomBarMaterial(
    backdrop: Backdrop,
    items: List<MaterialNavItem>,
    navigationBadge: NavigationBadgeState,
    modifier: Modifier,
    progressiveBlur: Boolean = false,
) {
    val mainPagerState = LocalMainPagerState.current
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        .let { inset -> if (inset != 0.dp) 8.dp + inset else 28.dp }

    FloatingBottomBar(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { }
            }
            .padding(start = 28.dp, end = 28.dp, bottom = bottomPadding),
        selectedIndex = mainPagerState.selectedPage,
        onSelected = { mainPagerState.animateToPage(it) },
        backdrop = backdrop,
        tabsCount = items.size,
        isBlurEnabled = true,
        progressiveBlur = progressiveBlur,
        accentColor = MaterialTheme.colorScheme.primary,
        tabContentColor = MaterialTheme.colorScheme.onSurface,
        surfaceContainer = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) { activateTab ->
        items.forEachIndexed { index, item ->
            val selected = mainPagerState.selectedPage == index
            val destination = mainPagerState.destinations.getOrNull(index)
            FloatingBottomBarItem(
                selected = selected,
                onClick = { activateTab(index) },
                modifier = Modifier.defaultMinSize(minWidth = 76.dp)
            ) {
                // The bar recolors its indicator copy through the Miuix content color; forward it to Material.
                CompositionLocalProvider(LocalContentColor provides MiuixLocalContentColor.current) {
                    NavigationIconWithBadge(
                        icon = if (selected) item.selectedIcon else item.unselectedIcon,
                        iconPath = item.iconPath,
                        contentDescription = customText(item.label),
                        badge = destination?.let { badgeFor(it.ordinal, navigationBadge) },
                        selected = selected,
                    )
                    Text(
                        text = customText(item.label),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class MaterialNavItem(
    val label: CustomTextKey,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val iconPath: String?,
)

@Composable
private fun FloatingNavItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    iconPath: String?,
    badge: NavBadge?,
    progressiveBlur: Boolean = false,
    onClick: () -> Unit,
) {
    // With the progressive pill the unselected items must not paint an opaque capsule, otherwise
    // they would punch solid holes through the blur.
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            progressiveBlur -> Color.Transparent
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "nav_item_container"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "nav_item_content"
    )

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .clip(CircleShape)
                .background(containerColor)
                .selectable(selected = selected, onClick = onClick, role = Role.Tab)
                .animateContentSize()
                .padding(horizontal = if (selected) 16.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavigationIconWithBadge(
                icon = icon,
                iconPath = iconPath,
                contentDescription = if (selected) null else label,
                badge = badge,
                selected = selected,
            )
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun NavigationIconWithBadge(
    icon: ImageVector,
    contentDescription: String?,
    badge: NavBadge?,
    iconPath: String? = null,
    selected: Boolean = false,
) {
    if (badge != null) {
        BadgedBox(
            badge = {
                when (badge.tone) {
                    BadgeTone.Alert -> Badge {
                        Text(badge.count.toString())
                    }

                    BadgeTone.Accent -> Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(badge.count.toString())
                    }
                }
            }
        ) {
            BottomBarIconContent(
                icon = icon,
                iconPath = iconPath,
                contentDescription = contentDescription,
                tint = LocalContentColor.current,
                selected = selected,
            )
        }
    } else {
        BottomBarIconContent(
            icon = icon,
            iconPath = iconPath,
            contentDescription = contentDescription,
            tint = LocalContentColor.current,
            selected = selected,
        )
    }
}
