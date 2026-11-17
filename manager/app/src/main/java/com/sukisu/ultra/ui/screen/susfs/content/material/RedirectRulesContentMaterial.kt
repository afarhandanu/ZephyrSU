package com.sukisu.ultra.ui.screen.susfs.content.material

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.screen.susfs.component.material.BottomActionButtonsMaterial
import com.sukisu.ultra.ui.screen.susfs.component.material.EmptyStateCardMaterial
import com.sukisu.ultra.ui.screen.susfs.component.material.PathItemCardMaterial
import com.sukisu.ultra.ui.screen.susfs.component.material.ResetButtonMaterial
import com.sukisu.ultra.ui.screen.susfs.component.material.SectionHeaderMaterial
import com.sukisu.ultra.ui.screen.susfs.util.RedirectRule

@Composable
fun RedirectRulesContentMaterial(
    redirectRules: Set<String>,
    isLoading: Boolean,
    onAddRule: () -> Unit,
    onRemoveRule: (String) -> Unit,
    onEditRule: (String) -> Unit,
    onReset: () -> Unit
) {
    val rules = remember(redirectRules) {
        redirectRules.mapNotNull { RedirectRule.parse(it) }.sortedBy { it.target }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (rules.isNotEmpty()) {
            SectionHeaderMaterial(
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
                PathItemCardMaterial(
                    path = "${rule.target} → ${rule.destination}",
                    icon = Icons.Default.Loop,
                    additionalInfo = scopeLabel,
                    onDelete = { onRemoveRule(rule.toRecord()) },
                    onEdit = { onEditRule(rule.toRecord()) },
                    isLoading = isLoading
                )
            }
        }

        if (rules.isEmpty()) {
            EmptyStateCardMaterial(message = stringResource(R.string.susfs_no_redirect_rules))
        }
    }

    BottomActionButtonsMaterial(
        primaryButtonText = stringResource(R.string.susfs_add_redirect_rule),
        onPrimaryClick = onAddRule,
        isLoading = isLoading
    )

    if (rules.isNotEmpty()) {
        ResetButtonMaterial(
            title = stringResource(R.string.susfs_reset_redirect_rules_title),
            onClick = onReset,
            enabled = !isLoading
        )
    }
}
