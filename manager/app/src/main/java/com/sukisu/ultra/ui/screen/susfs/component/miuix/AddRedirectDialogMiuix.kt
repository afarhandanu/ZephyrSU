package com.sukisu.ultra.ui.screen.susfs.component.miuix

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.screen.susfs.util.RedirectRule
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun AddRedirectDialogMiuix(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (RedirectRule) -> Unit,
    isLoading: Boolean,
    titleRes: Int,
    initialRule: String? = null
) {
    val initial = remember(initialRule) { initialRule?.let { RedirectRule.parse(it) } }
    var target by remember { mutableStateOf(initial?.target ?: "") }
    var destination by remember { mutableStateOf(initial?.destination ?: "") }
    var scheme by remember { mutableStateOf(initial?.uidScheme ?: 0) }

    LaunchedEffect(showDialog, initialRule) {
        if (showDialog) {
            target = initial?.target ?: ""
            destination = initial?.destination ?: ""
            scheme = initial?.uidScheme ?: 0
        }
    }

    val showDialogState = remember { mutableStateOf(showDialog) }

    LaunchedEffect(showDialog) {
        showDialogState.value = showDialog
    }

    if (showDialogState.value) {
        WindowDialog(
            show = showDialogState.value,
            title = stringResource(titleRes),
            onDismissRequest = { onDismiss() },
            content = {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextField(
                        value = target,
                        onValueChange = { target = it },
                        label = stringResource(R.string.susfs_redirect_target_label),
                        useLabelAsPlaceholder = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                    TextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = stringResource(R.string.susfs_redirect_destination_label),
                        useLabelAsPlaceholder = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                    Text(stringResource(R.string.susfs_redirect_uid_scheme_label))
                    listOf(
                        0 to R.string.susfs_redirect_uid_scheme_0,
                        1 to R.string.susfs_redirect_uid_scheme_1,
                        2 to R.string.susfs_redirect_uid_scheme_2
                    ).forEach { (value, labelRes) ->
                        Button(
                            onClick = { scheme = value },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = if (scheme == value) {
                                    "✓ ${stringResource(labelRes)}"
                                } else {
                                    stringResource(labelRes)
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onDismiss() },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                if (target.isNotBlank() && destination.isNotBlank()) {
                                    onConfirm(RedirectRule(target.trim(), destination.trim(), scheme))
                                }
                            },
                            enabled = target.isNotBlank() && destination.isNotBlank() && !isLoading,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    if (initialRule != null) R.string.susfs_save else R.string.add
                                )
                            )
                        }
                    }
                }
            }
        )
    }
}
