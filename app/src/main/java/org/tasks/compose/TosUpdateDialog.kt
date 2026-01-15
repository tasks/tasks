package org.tasks.compose

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.window.DialogProperties
import org.tasks.R
import org.tasks.themes.TasksTheme

@Composable
fun TosUpdateDialog(
    isUpdate: Boolean,
    onAccept: () -> Unit,
    onExit: () -> Unit,
    openUrl: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onExit,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        title = {
            Text(
                text = stringResource(
                    if (isUpdate) R.string.tos_updated_title else R.string.terms_of_service_proper
                )
            )
        },
        text = {
            LegalDisclosure(openLegalUrl = openUrl, textAlign = TextAlign.Start)
        },
        dismissButton = {
            TextButton(onClick = onExit) {
                Text(text = stringResource(R.string.exit))
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text(text = stringResource(R.string.accept))
            }
        }
    )
}

@PreviewLightDark
@Composable
fun TosUpdateDialogPreview() {
    TasksTheme {
        TosUpdateDialog(
            isUpdate = true,
            onAccept = {},
            onExit = {},
            openUrl = {},
        )
    }
}

@PreviewLightDark
@Composable
fun TosDialogPreview() {
    TasksTheme {
        TosUpdateDialog(
            isUpdate = false,
            onAccept = {},
            onExit = {},
            openUrl = {},
        )
    }
}
