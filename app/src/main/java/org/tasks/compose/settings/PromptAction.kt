package org.tasks.compose.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.tasks.R
import org.tasks.compose.Constants
import org.tasks.themes.TasksTheme

@Composable
fun PromptAction(
    showDialog: MutableState<Boolean>,
    title: String,
    onAction: () -> Unit,
    onCancel: () -> Unit = { showDialog.value = false }
) {
    if (showDialog.value) {
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
            showDialog = remember { mutableStateOf(true) },
            title = "Delete list?",
            onAction = { /*TODO*/ })
    }
}
