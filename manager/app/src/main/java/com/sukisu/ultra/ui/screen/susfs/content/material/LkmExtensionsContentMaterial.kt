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

@Composable
fun LkmExtensionsContentMaterial(
    hiddenModules: Set<String>,
    mountPrefixes: Set<String>,
    isLoading: Boolean,
    isLkm: Boolean,
    onAddModule: () -> Unit,
    onRemoveModule: (String) -> Unit,
    onEditModule: (String) -> Unit,
    onAddPrefix: () -> Unit,
    onRemovePrefix: (String) -> Unit,
    onEditPrefix: (String) -> Unit,
    onResetModules: () -> Unit,
    onResetPrefixes: () -> Unit
) {
    val sortedModules = remember(hiddenModules) { hiddenModules.sorted() }
    val sortedPrefixes = remember(mountPrefixes) { mountPrefixes.sorted() }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!isLkm) {
            EmptyStateCardMaterial(message = stringResource(R.string.susfs_lkm_only_notice))
        }

        // 隐藏模块
        if (sortedModules.isNotEmpty()) {
            SectionHeaderMaterial(
                title = stringResource(R.string.susfs_add_hidden_module),
                subtitle = null,
                icon = Icons.Default.VisibilityOff,
                count = sortedModules.size
            )
            sortedModules.forEach { name ->
                PathItemCardMaterial(
                    path = name,
                    icon = Icons.Default.VisibilityOff,
                    onDelete = { onRemoveModule(name) },
                    onEdit = { onEditModule(name) },
                    isLoading = isLoading
                )
            }
        }

        if (sortedModules.isEmpty()) {
            EmptyStateCardMaterial(message = stringResource(R.string.susfs_no_hidden_modules))
        }
    }

    BottomActionButtonsMaterial(
        primaryButtonText = stringResource(R.string.susfs_add_hidden_module),
        onPrimaryClick = onAddModule,
        isLoading = isLoading
    )

    if (sortedModules.isNotEmpty()) {
        ResetButtonMaterial(
            title = stringResource(R.string.susfs_reset_hidden_modules_title),
            onClick = onResetModules,
            enabled = !isLoading
        )
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 隐藏挂载前缀
        if (sortedPrefixes.isNotEmpty()) {
            SectionHeaderMaterial(
                title = stringResource(R.string.susfs_add_mount_prefix),
                subtitle = null,
                icon = Icons.Default.Folder,
                count = sortedPrefixes.size
            )
            sortedPrefixes.forEach { prefix ->
                PathItemCardMaterial(
                    path = prefix,
                    icon = Icons.Default.Folder,
                    onDelete = { onRemovePrefix(prefix) },
                    onEdit = { onEditPrefix(prefix) },
                    isLoading = isLoading
                )
            }
        }

        if (sortedPrefixes.isEmpty()) {
            EmptyStateCardMaterial(message = stringResource(R.string.susfs_no_mount_prefixes))
        }
    }

    BottomActionButtonsMaterial(
        primaryButtonText = stringResource(R.string.susfs_add_mount_prefix),
        onPrimaryClick = onAddPrefix,
        isLoading = isLoading
    )

    if (sortedPrefixes.isNotEmpty()) {
        ResetButtonMaterial(
            title = stringResource(R.string.susfs_reset_mount_prefixes_title),
            onClick = onResetPrefixes,
            enabled = !isLoading
        )
    }
}
