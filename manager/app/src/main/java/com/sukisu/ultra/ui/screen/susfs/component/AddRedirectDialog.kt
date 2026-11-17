package com.sukisu.ultra.ui.screen.susfs.component

import androidx.compose.runtime.Composable
import com.sukisu.ultra.ui.LocalUiMode
import com.sukisu.ultra.ui.UiMode
import com.sukisu.ultra.ui.screen.susfs.component.material.AddRedirectDialogMaterial
import com.sukisu.ultra.ui.screen.susfs.component.miuix.AddRedirectDialogMiuix
import com.sukisu.ultra.ui.screen.susfs.util.RedirectRule

/**
 * Add/edit dialog for one open-redirect rule (M4.4, ported from FinaSU).
 * [initialRule] is the raw "target|destination|uid_scheme" record being
 * edited, or null when adding a new rule.
 */
@Composable
fun AddRedirectDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (RedirectRule) -> Unit,
    isLoading: Boolean,
    titleRes: Int,
    initialRule: String? = null
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> AddRedirectDialogMiuix(
            showDialog = showDialog,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            isLoading = isLoading,
            titleRes = titleRes,
            initialRule = initialRule
        )
        UiMode.Material -> AddRedirectDialogMaterial(
            showDialog = showDialog,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            isLoading = isLoading,
            titleRes = titleRes,
            initialRule = initialRule
        )
    }
}
