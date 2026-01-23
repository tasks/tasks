package org.tasks.compose

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.tasks.R
import org.tasks.backup.TasksJsonImporter

@Composable
fun ImportTasksDialog(
    uri: Uri,
    viewModel: ImportTasksViewModel,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(uri) {
        viewModel.startImport(uri)
    }

    val isDone = state is ImportTasksViewModel.ImportState.Complete ||
            state is ImportTasksViewModel.ImportState.Error

    AlertDialog(
        onDismissRequest = { /* non-dismissable while importing */ },
        properties = DialogProperties(
            dismissOnBackPress = isDone,
            dismissOnClickOutside = false,
        ),
        title = {
            Text(
                text = stringResource(
                    when (state) {
                        is ImportTasksViewModel.ImportState.Complete -> R.string.import_summary_title
                        is ImportTasksViewModel.ImportState.Error -> R.string.import_failed
                        else -> R.string.backup_BAc_import
                    }
                )
            )
        },
        text = {
            when (val currentState = state) {
                is ImportTasksViewModel.ImportState.Idle,
                is ImportTasksViewModel.ImportState.Importing -> {
                    val message = (currentState as? ImportTasksViewModel.ImportState.Importing)?.message ?: ""
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = message.ifEmpty { stringResource(R.string.backup_BAc_import) }
                        )
                    }
                }
                is ImportTasksViewModel.ImportState.Complete -> {
                    Text(text = formatSummary(context, currentState.result))
                }
                is ImportTasksViewModel.ImportState.Error -> {
                    Text(text = stringResource(R.string.import_failed_message))
                }
            }
        },
        confirmButton = {
            if (isDone) {
                Button(onClick = {
                    viewModel.reset()
                    onFinished()
                }) {
                    Text(text = stringResource(R.string.ok))
                }
            }
        }
    )
}

private fun formatSummary(context: Context, result: TasksJsonImporter.ImportResult): String {
    val resources = context.resources
    return resources.getString(
        R.string.import_summary_message,
        "",
        resources.getQuantityString(R.plurals.Ntasks, result.taskCount, result.taskCount),
        resources.getQuantityString(R.plurals.Ntasks, result.importCount, result.importCount),
        resources.getQuantityString(R.plurals.Ntasks, result.skipCount, result.skipCount),
        resources.getQuantityString(R.plurals.Ntasks, 0, 0)
    )
}
