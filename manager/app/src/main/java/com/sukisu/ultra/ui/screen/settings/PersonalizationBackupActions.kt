package com.sukisu.ultra.ui.screen.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.sukisu.ultra.R
import com.sukisu.ultra.data.backup.PersonalizationBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The two personalization backup actions, shared by the Material and Miuix backup settings. */
class PersonalizationBackupActions(val export: () -> Unit, val restore: () -> Unit)

@Composable
fun rememberPersonalizationBackupActions(): PersonalizationBackupActions {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) { PersonalizationBackup.export(context, uri) }
                val text = if (result.isSuccess) {
                    resources.getString(R.string.personalization_backup_saved)
                } else {
                    resources.getString(
                        R.string.personalization_backup_failed,
                        result.exceptionOrNull()?.message.orEmpty(),
                    )
                }
                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) { PersonalizationBackup.restore(context, uri) }
                if (result.isSuccess) {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.personalization_restore_done),
                        Toast.LENGTH_LONG,
                    ).show()
                    PersonalizationBackup.restart(context)
                } else {
                    Toast.makeText(
                        context,
                        resources.getString(
                            R.string.personalization_backup_failed,
                            result.exceptionOrNull()?.message.orEmpty(),
                        ),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    return remember(exportLauncher, restoreLauncher) {
        PersonalizationBackupActions(
            export = { exportLauncher.launch(PersonalizationBackup.fileName()) },
            restore = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
        )
    }
}
