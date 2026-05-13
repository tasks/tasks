package org.tasks.compose.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.ok

@Composable
fun PromptAction(
    showDialog: Boolean,
    title: String,
    onAction: () -> Unit,
    onCancel: () -> Unit,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
            confirmButton = {
                TextButton(onClick = onAction) {
                    Text(text = stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancel) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
    }
}
