package org.tasks.compose

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.settings.serverTypes
import org.tasks.themes.TasksTheme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.caldav_server_type

@Composable
fun ServerSelector(selected: Int, onSelected: (Int) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier
        .padding(16.dp)
        .clickable { expanded = !expanded }) {
        Text(
            text = stringResource(Res.string.caldav_server_type),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
        )
        Spinner(
            options = serverTypes.map { (_, res) -> stringResource(res) },
            values = serverTypes.map { (value, _) -> value },
            selected = selected,
            expanded = expanded,
            onSelected = {
                expanded = false
                onSelected(it)
            },
            setExpanded = { expanded = it },
            modifier = Modifier
                .padding(vertical = 6.dp)
                .fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ServerSelectorPreview() =
    TasksTheme {
        ServerSelector(1) {}
    }
