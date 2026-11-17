package com.sukisu.ultra.ui.screen.susfs.component.material

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.screen.susfs.util.RedirectRule

@Composable
fun AddRedirectDialogMaterial(
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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text(stringResource(titleRes)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text(stringResource(R.string.susfs_redirect_target_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text(stringResource(R.string.susfs_redirect_destination_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true
                    )
                    Text(stringResource(R.string.susfs_redirect_uid_scheme_label))
                    listOf(
                        0 to R.string.susfs_redirect_uid_scheme_0,
                        1 to R.string.susfs_redirect_uid_scheme_1,
                        2 to R.string.susfs_redirect_uid_scheme_2
                    ).forEach { (value, labelRes) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isLoading) { scheme = value },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = scheme == value,
                                onClick = { scheme = value },
                                enabled = !isLoading
                            )
                            Text(stringResource(labelRes))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (target.isNotBlank() && destination.isNotBlank()) {
                            onConfirm(RedirectRule(target.trim(), destination.trim(), scheme))
                        }
                    },
                    enabled = target.isNotBlank() && destination.isNotBlank() && !isLoading
                ) {
                    Text(stringResource(if (initialRule != null) R.string.susfs_save else R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismiss() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
