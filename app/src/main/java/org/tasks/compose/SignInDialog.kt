package org.tasks.compose

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import org.tasks.R
import org.tasks.auth.SignInActivity

@Composable
fun SignInDialog(
    selected: (SignInActivity.Platform) -> Unit,
    help: () -> Unit,
    cancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colors.surface)
    ) {
        Text(
            text = stringResource(id = R.string.sign_in_to_tasks),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(16.dp),
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
            tint = MaterialTheme.colors.onSurface.copy(
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
                Text(text = stringResource(id = R.string.help))
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = cancel) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    }
}

@Composable
fun ConsentDialog(
    agree: (Boolean) -> Unit,
) {
    Column(Modifier.background(MaterialTheme.colors.surface)) {
        Text(
            text = stringResource(id = R.string.sign_in_to_tasks),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(16.dp),
        )
        Text(
            text = stringResource(id = R.string.sign_in_to_tasks_disclosure),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            TextButton(onClick = { agree(false) }) {
                Text(text = stringResource(id = R.string.consent_deny))
            }
            TextButton(onClick = { agree(true) }) {
                Text(text = stringResource(id = R.string.consent_agree))
            }
        }
    }
}

@Preview(widthDp = 320)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun SignInDialogPreview() {
    MdcTheme {
        SignInDialog(selected = {}, help = {}, cancel = {})
    }
}

@Preview(widthDp = 320)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun DisclosurePreview() {
    MdcTheme {
        ConsentDialog(agree = {})
    }
}