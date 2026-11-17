package com.sukisu.ultra.ui.screen.susfs.content

import androidx.compose.runtime.Composable
import com.sukisu.ultra.ui.LocalUiMode
import com.sukisu.ultra.ui.UiMode
import com.sukisu.ultra.ui.screen.susfs.content.material.RedirectRulesContentMaterial
import com.sukisu.ultra.ui.screen.susfs.content.miuix.RedirectRulesContentMiuix

/**
 * Open-redirect rules tab (M4.4, ported from FinaSU). Rules are stored as
 * "target|destination|uid_scheme" records; the rows carry the raw record
 * for remove/edit so the manager can match the persisted set exactly.
 */
@Composable
fun RedirectRulesContent(
    redirectRules: Set<String>,
    isLoading: Boolean,
    onAddRule: () -> Unit,
    onRemoveRule: (String) -> Unit,
    onEditRule: ((String) -> Unit)? = null,
    onReset: (() -> Unit)? = null
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> RedirectRulesContentMiuix(
            redirectRules = redirectRules,
            isLoading = isLoading,
            onAddRule = onAddRule,
            onRemoveRule = onRemoveRule,
            onEditRule = onEditRule,
            onReset = onReset
        )
        UiMode.Material -> RedirectRulesContentMaterial(
            redirectRules = redirectRules,
            isLoading = isLoading,
            onAddRule = onAddRule,
            onRemoveRule = onRemoveRule,
            onEditRule = { onEditRule?.invoke(it) },
            onReset = { onReset?.invoke() }
        )
    }
}
