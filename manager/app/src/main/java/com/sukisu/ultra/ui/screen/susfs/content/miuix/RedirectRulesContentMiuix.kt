package com.sukisu.ultra.ui.screen.susfs.content.miuix

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.screen.susfs.component.miuix.BottomActionButtonsMiuix
import com.sukisu.ultra.ui.screen.susfs.component.miuix.EmptyStateCardMiuix
import com.sukisu.ultra.ui.screen.susfs.component.miuix.PathItemCardMiuix
import com.sukisu.ultra.ui.screen.susfs.component.miuix.ResetButtonMiuix
import com.sukisu.ultra.ui.screen.susfs.component.miuix.SectionHeaderMiuix
import com.sukisu.ultra.ui.screen.susfs.util.RedirectRule

@Composable
fun RedirectRulesContentMiuix(
    redirectRules: Set<String>,
    isLoading: Boolean,
    onAddRule: () -> Unit,
    onRemoveRule: (String) -> Unit,
    onEditRule: ((String) -> Unit)? = null,
    onReset: (() -> Unit)? = null
) {
    val rules = remember(redirectRules) {
        redirectRules.mapNotNull { RedirectRule.parse(it) }.sortedBy { it.target }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (rules.isNotEmpty()) {
            SectionHeaderMiuix(
                title = stringResource(R.string.susfs_tab_redirect_rules),
                subtitle = null,
                icon = Icons.Default.Loop,
                count = rules.size
            )
            rules.forEach { rule ->
                val scopeLabel = stringResource(
                    when (rule.uidScheme) {
                        0 -> R.string.susfs_redirect_uid_scheme_0
                        1 -> R.string.susfs_redirect_uid_scheme_1
                        else -> R.string.susfs_redirect_uid_scheme_2
                    }
                )
                PathItemCardMiuix(
                    path = "${rule.target} → ${rule.destination}",
                    icon = Icons.Default.Loop,
                    additionalInfo = scopeLabel,
                    onDelete = { onRemoveRule(rule.toRecord()) },
                    onEdit = if (onEditRule != null) {
                        { onEditRule(rule.toRecord()) }
                    } else null,
                    isLoading = isLoading
                )
            }
        }

        if (rules.isEmpty()) {
            EmptyStateCardMiuix(message = stringResource(R.string.susfs_no_redirect_rules))
        }
    }

    BottomActionButtonsMiuix(
        primaryButtonText = stringResource(R.string.susfs_add_redirect_rule),
        onPrimaryClick = onAddRule,
        isLoading = isLoading
    )

    if (onReset != null && rules.isNotEmpty()) {
        ResetButtonMiuix(
            title = stringResource(R.string.susfs_reset_redirect_rules_title),
            onClick = onReset,
            enabled = !isLoading
        )
    }
}
