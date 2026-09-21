package org.tasks.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.previews.PREVIEW_NIGHT_MODE
import org.tasks.themes.TasksTheme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cloud_onboarding_create_list_body
import tasks.kmp.generated.resources.cloud_onboarding_create_list_title
import tasks.kmp.generated.resources.create_a_list
import tasks.kmp.generated.resources.ic_round_icon
import tasks.kmp.generated.resources.tasks_org

@Composable
internal fun CreateListStep(
    onCreateList: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Image(
                painter = painterResource(Res.drawable.ic_round_icon),
                contentDescription = stringResource(Res.string.tasks_org),
                modifier = Modifier.size(80.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.cloud_onboarding_create_list_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.cloud_onboarding_create_list_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onCreateList,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(Res.string.create_a_list))
            }
        }
    }
}

@Preview(showBackground = true, name = "Create list - Light")
@Preview(showBackground = true, uiMode = PREVIEW_NIGHT_MODE, name = "Create list - Dark")
@Composable
private fun CreateListStepPreview() {
    TasksTheme {
        CreateListStep(
            onCreateList = {},
        )
    }
}
