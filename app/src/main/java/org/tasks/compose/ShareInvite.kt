package org.tasks.compose

import android.util.Patterns
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import org.tasks.R
import org.tasks.compose.Constants.TextButton
import org.tasks.compose.Constants.textFieldColors
import org.tasks.compose.ShareInvite.ShareInvite
import org.tasks.themes.TasksTheme

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun Invite() = TasksTheme {
    ShareInvite(true, remember { mutableStateOf("") })
}

@Preview(showBackground = true, backgroundColor = 0x202124)
@Composable
private fun InviteDark() = TasksTheme(theme = 2) {
    ShareInvite(false, remember { mutableStateOf("") })
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun InviteFilled() = TasksTheme {
    ShareInvite(true, remember { mutableStateOf("user@example.com") })
}

@Preview(showBackground = true, backgroundColor = 0x202124)
@Composable
private fun InviteDarkFilled() = TasksTheme(theme = 2) {
    ShareInvite(false, remember { mutableStateOf("user@example.com") })
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun InviteError() = TasksTheme {
    ShareInvite(true, remember { mutableStateOf("invalid email") }, isError = true)
}

object ShareInvite {
    @Composable
    fun ShareInviteDialog(
        openDialog: MutableState<Boolean>,
        email: Boolean,
        invite: (String) -> Unit,
    ) {
        val text = rememberSaveable { mutableStateOf("") }
        val showError = rememberSaveable { mutableStateOf(false) }
        if (openDialog.value) {
            val trimmed = text.value.trim()
            val isValid = if (email) {
                trimmed.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
            } else {
                trimmed.isNotEmpty()
            }
            AlertDialog(
                onDismissRequest = {},
                text = { ShareInvite(email, text, isError = showError.value && !isValid) },
                confirmButton = {
                    TextButton(text = R.string.invite, onClick = {
                        if (isValid) {
                            invite(trimmed)
                        } else {
                            showError.value = true
                        }
                    })
                },
                dismissButton = {
                    TextButton(text = R.string.cancel, onClick = { openDialog.value = false })
                },
            )
        } else {
            text.value = ""
            showError.value = false
        }
    }

    @Composable
    fun ShareInvite(
        email: Boolean,
        text: MutableState<String>,
        isError: Boolean = false,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.share_list),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Constants.KEYLINE_FIRST))
            val label = stringResource(if (email) R.string.email else R.string.user)
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = text.value,
                label = {
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (email) KeyboardType.Email else KeyboardType.Text
                ),
                onValueChange = { text.value = it },
                leadingIcon = {
                    Icon(
                        imageVector = if (email) Icons.Outlined.Email else Icons.Outlined.Person,
                        contentDescription = label
                    )
                },
                isError = isError,
                supportingText = if (isError) {
                    { Text(stringResource(R.string.invalid_email_address)) }
                } else null,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textDirection = TextDirection.Content
                ),
                colors = textFieldColors(),
            )
        }
    }
}
