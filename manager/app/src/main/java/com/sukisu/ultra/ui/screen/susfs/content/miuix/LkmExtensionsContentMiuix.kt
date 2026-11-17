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

@Composable
fun LkmExtensionsContentMiuix(
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
    val sortedModules = remember(hiddenModules) { hiddenModules.sorted() }
    val sortedPrefixes = remember(mountPrefixes) { mountPrefixes.sorted() }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!isLkm) {
            EmptyStateCardMiuix(message = stringResource(R.string.susfs_lkm_only_notice))
        }

        // 隐藏模块
        if (sortedModules.isNotEmpty()) {
            SectionHeaderMiuix(
                title = stringResource(R.string.susfs_add_hidden_module),
                subtitle = null,
                icon = Icons.Default.VisibilityOff,
                count = sortedModules.size
            )
            sortedModules.forEach { name ->
                PathItemCardMiuix(
                    path = name,
                    icon = Icons.Default.VisibilityOff,
                    onDelete = { onRemoveModule(name) },
                    onEdit = if (onEditModule != null) {
                        { onEditModule(name) }
                    } else null,
                    isLoading = isLoading
                )
            }
        }

        if (sortedModules.isEmpty()) {
            EmptyStateCardMiuix(message = stringResource(R.string.susfs_no_hidden_modules))
        }
    }

    BottomActionButtonsMiuix(
        primaryButtonText = stringResource(R.string.susfs_add_hidden_module),
        onPrimaryClick = onAddModule,
        isLoading = isLoading
    )

    if (onResetModules != null && sortedModules.isNotEmpty()) {
        ResetButtonMiuix(
            title = stringResource(R.string.susfs_reset_hidden_modules_title),
            onClick = onResetModules,
            enabled = !isLoading
        )
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 隐藏挂载前缀
        if (sortedPrefixes.isNotEmpty()) {
            SectionHeaderMiuix(
                title = stringResource(R.string.susfs_add_mount_prefix),
                subtitle = null,
                icon = Icons.Default.Folder,
                count = sortedPrefixes.size
            )
            sortedPrefixes.forEach { prefix ->
                PathItemCardMiuix(
                    path = prefix,
                    icon = Icons.Default.Folder,
                    onDelete = { onRemovePrefix(prefix) },
                    onEdit = if (onEditPrefix != null) {
                        { onEditPrefix(prefix) }
                    } else null,
                    isLoading = isLoading
                )
            }
        }

        if (sortedPrefixes.isEmpty()) {
            EmptyStateCardMiuix(message = stringResource(R.string.susfs_no_mount_prefixes))
        }
    }

    BottomActionButtonsMiuix(
        primaryButtonText = stringResource(R.string.susfs_add_mount_prefix),
        onPrimaryClick = onAddPrefix,
        isLoading = isLoading
    )

    if (onResetPrefixes != null && sortedPrefixes.isNotEmpty()) {
        ResetButtonMiuix(
            title = stringResource(R.string.susfs_reset_mount_prefixes_title),
            onClick = onResetPrefixes,
            enabled = !isLoading
        )
    }
}
