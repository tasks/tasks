package org.tasks.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import org.tasks.R
import org.tasks.compose.Constants.TextButton
import org.tasks.compose.Constants.textFieldColors
import org.tasks.compose.ShareInvite.ShareInvite

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun Invite() = MaterialTheme {
    ShareInvite(true, remember { mutableStateOf("") })
}

@Preview(showBackground = true, backgroundColor = 0x202124)
@Composable
private fun InviteDark() = MaterialTheme(darkColors()) {
    ShareInvite(false, remember { mutableStateOf("") })
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun InviteFilled() = MaterialTheme {
    ShareInvite(true, remember { mutableStateOf("user@example.com") })
}

@Preview(showBackground = true, backgroundColor = 0x202124)
@Composable
private fun InviteDarkFilled() = MaterialTheme(darkColors()) {
    ShareInvite(false, remember { mutableStateOf("user@example.com") })
}

object ShareInvite {
    @Composable
    fun ShareInviteDialog(
        openDialog: MutableState<Boolean>,
        email: Boolean,
        invite: (String) -> Unit,
    ) {
        val text = rememberSaveable { mutableStateOf("") }
        if (openDialog.value) {
            AlertDialog(
                onDismissRequest = {},
                text = { ShareInvite(email, text) },
                confirmButton = {
                    TextButton(text = R.string.invite, onClick = { invite(text.value) })
                },
                dismissButton = {
                    TextButton(text = R.string.cancel, onClick = { openDialog.value = false })
                },
            )
        } else {
            text.value = ""
        }
    }

    @Composable
    fun ShareInvite(
        email: Boolean,
        text: MutableState<String>,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.share_list),
                style = MaterialTheme.typography.h6,
            )
            Spacer(Modifier.height(Constants.KEYLINE_FIRST))
            val label = stringResource(if (email) R.string.email else R.string.user)
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = text.value,
                label = { Text(label) },
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
                textStyle = MaterialTheme.typography.body1,
                colors = textFieldColors(),
            )
        }
    }
}