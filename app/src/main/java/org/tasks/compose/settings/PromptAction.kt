package org.tasks.compose.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.tasks.R
import org.tasks.compose.Constants
import org.tasks.themes.TasksTheme

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
            confirmButton = { Constants.TextButton(text = R.string.ok, onClick = onAction) },
            dismissButton = { Constants.TextButton(text = R.string.cancel, onClick = onCancel) }
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PromptActionPreview() {
    TasksTheme {
        PromptAction(
            showDialog = true,
            title = "Delete list?",
            onAction = { /*TODO*/ },
            onCancel = {},
        )
    }
}
