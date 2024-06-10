package org.tasks.compose

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.auth.SignInActivity
import org.tasks.themes.TasksTheme

@Composable
fun SignInDialog(
    selected: (SignInActivity.Platform) -> Unit,
    help: () -> Unit,
    cancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = stringResource(id = R.string.sign_in_to_tasks),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        SyncAccount(
            title = R.string.sign_in_with_google,
            description = R.string.google_play_subscribers,
            icon = R.drawable.ic_google,
            onClick = { selected(SignInActivity.Platform.GOOGLE) }
        )
        SyncAccount(
            title = R.string.sign_in_with_github,
            description = R.string.github_sponsors,
            icon = R.drawable.ic_octocat,
            tint = MaterialTheme.colorScheme.onSurface.copy(
                alpha = ContentAlpha.medium
            ),
            onClick = { selected(SignInActivity.Platform.GITHUB) }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TextButton(onClick = help) {
                Text(
                    text = stringResource(id = R.string.help),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = cancel) {
                Text(
                    text = stringResource(id = R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun ConsentDialog(
    agree: (Boolean) -> Unit,
) {
    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
        Text(
            text = stringResource(id = R.string.sign_in_to_tasks),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(id = R.string.sign_in_to_tasks_disclosure),
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            TextButton(onClick = { agree(false) }) {
                Text(
                    text = stringResource(id = R.string.consent_deny),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            TextButton(onClick = { agree(true) }) {
                Text(
                    text = stringResource(id = R.string.consent_agree),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Preview(widthDp = 320)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun SignInDialogPreview() {
    TasksTheme {
        SignInDialog(selected = {}, help = {}, cancel = {})
    }
}

@Preview(widthDp = 320)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun DisclosurePreview() {
    TasksTheme {
        ConsentDialog(agree = {})
    }
}