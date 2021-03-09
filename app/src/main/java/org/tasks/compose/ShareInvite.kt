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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
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
    ShareInvite(mutableStateOf(""))
}

@Preview(showBackground = true, backgroundColor = 0x202124)
@Composable
private fun InviteDark() = MaterialTheme(darkColors()) {
    ShareInvite(mutableStateOf(""))
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun InviteFilled() = MaterialTheme {
    ShareInvite(mutableStateOf("user@example.com"))
}

@Preview(showBackground = true, backgroundColor = 0x202124)
@Composable
private fun InviteDarkFilled() = MaterialTheme(darkColors()) {
    ShareInvite(mutableStateOf("user@example.com"))
}

object ShareInvite {
    @Composable
    fun ShareInviteDialog(
        openDialog: MutableState<Boolean>,
        invite: (String?) -> Unit,
    ) {
        val email = rememberSaveable { mutableStateOf("") }
        // TODO: remove after beta02 release: https://issuetracker.google.com/issues/181282423
        val enableHack = rememberSaveable { mutableStateOf(true) }
        if (openDialog.value) {
            AlertDialog(
                onDismissRequest = {},
                text = { ShareInvite(email, enableHack) },
                confirmButton = {
                    TextButton(text = R.string.invite, onClick = {
                        enableHack.value = false
                        invite(email.value)
                    })
                },
                dismissButton = {
                    TextButton(text = R.string.cancel, onClick = {
                        enableHack.value = false
                        invite(null as String?)
                    })
                },
            )
        } else {
            email.value = ""
            enableHack.value = true
        }
    }

    @Composable
    fun ShareInvite(email: MutableState<String>, enableHack: MutableState<Boolean> = mutableStateOf(true)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.share_list),
                style = MaterialTheme.typography.h6,
            )
            Spacer(Modifier.height(Constants.KEYLINE_FIRST))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = email.value,
                label = { Text(stringResource(R.string.email)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                onValueChange = { email.value = it },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = stringResource(id = R.string.email)
                    )
                },
                enabled = enableHack.value,
                textStyle = MaterialTheme.typography.body1,
                colors = textFieldColors(),
            )
        }
    }
}