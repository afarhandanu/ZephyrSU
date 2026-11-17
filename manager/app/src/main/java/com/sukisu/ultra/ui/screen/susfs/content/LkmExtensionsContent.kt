package com.sukisu.ultra.ui.screen.susfs.content

import androidx.compose.runtime.Composable
import com.sukisu.ultra.ui.LocalUiMode
import com.sukisu.ultra.ui.UiMode
import com.sukisu.ultra.ui.screen.susfs.content.material.LkmExtensionsContentMaterial
import com.sukisu.ultra.ui.screen.susfs.content.miuix.LkmExtensionsContentMiuix

/**
 * LKM Extensions tab (M4.4, ported from FinaSU): hidden kernel module names
 * and hidden mount prefixes, applied live via the LKM proc nodes
 * (/proc/susfs_hide_modules, /proc/susfs_hide_mounts). [isLkm] gates only
 * the notice — entries can still be persisted on a built-in build, but the
 * live apply will report the missing proc node.
 */
@Composable
fun LkmExtensionsContent(
    hiddenModules: Set<String>,
    mountPrefixes: Set<String>,
    isLoading: Boolean,
    isLkm: Boolean,
    onAddModule: () -> Unit,
    onRemoveModule: (String) -> Unit,
    onEditModule: ((String) -> Unit)? = null,
    onAddPrefix: () -> Unit,
    onRemovePrefix: (String) -> Unit,
    onEditPrefix: ((String) -> Unit)? = null,
    onResetModules: (() -> Unit)? = null,
    onResetPrefixes: (() -> Unit)? = null
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> LkmExtensionsContentMiuix(
            hiddenModules = hiddenModules,
            mountPrefixes = mountPrefixes,
            isLoading = isLoading,
            isLkm = isLkm,
            onAddModule = onAddModule,
            onRemoveModule = onRemoveModule,
            onEditModule = onEditModule,
            onAddPrefix = onAddPrefix,
            onRemovePrefix = onRemovePrefix,
            onEditPrefix = onEditPrefix,
            onResetModules = onResetModules,
            onResetPrefixes = onResetPrefixes
        )
        UiMode.Material -> LkmExtensionsContentMaterial(
            hiddenModules = hiddenModules,
            mountPrefixes = mountPrefixes,
            isLoading = isLoading,
            isLkm = isLkm,
            onAddModule = onAddModule,
            onRemoveModule = onRemoveModule,
            onEditModule = { onEditModule?.invoke(it) },
            onAddPrefix = onAddPrefix,
            onRemovePrefix = onRemovePrefix,
            onEditPrefix = { onEditPrefix?.invoke(it) },
            onResetModules = { onResetModules?.invoke() },
            onResetPrefixes = { onResetPrefixes?.invoke() }
        )
    }
}
