package org.tasks.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import org.tasks.TasksUrls
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.ai_disclosure_accept
import tasks.kmp.generated.resources.ai_disclosure_body
import tasks.kmp.generated.resources.ai_disclosure_title
import tasks.kmp.generated.resources.ai_privacy_policy
import tasks.kmp.generated.resources.cancel

/**
 * Unlike [TosUpdateDialog] the dismiss action is Cancel, not Exit: declining an optional
 * feature leaves the feature off rather than closing the app.
 */
@Composable
fun AiDisclosureDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    openUrl: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        title = { Text(text = stringResource(Res.string.ai_disclosure_title)) },
        text = {
            Column {
                Text(text = stringResource(Res.string.ai_disclosure_body))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.ai_privacy_policy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { openUrl(TasksUrls.OPENROUTER_PRIVACY) },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.cancel))
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text(text = stringResource(Res.string.ai_disclosure_accept))
            }
        },
    )
}
