package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditRow
import org.tasks.themes.TasksTheme

@Composable
fun CalendarRow(
    eventUri: String?,
    selectedCalendar: String?,
    onClick: () -> Unit,
    clear: () -> Unit,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_outline_event_24px,
        content = {
            if (eventUri?.isNotBlank() == true) {
                Row {
                    Text(
                        text = stringResource(id = R.string.gcal_TEA_showCalendar_label),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 20.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(
                        onClick = { clear() },
                        Modifier.padding(vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(id = R.string.delete),
                            modifier = Modifier.alpha(ContentAlpha.medium),
                        )
                    }
                }
            } else if (selectedCalendar?.isNotBlank() == true) {
                Text(
                    text = selectedCalendar,
                    modifier = Modifier.padding(vertical = 20.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                DisabledText(
                    text = stringResource(id = R.string.dont_add_to_calendar),
                    modifier = Modifier.padding(vertical = 20.dp),
                )
            }
        },
        onClick = onClick
    )
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoCalendar() {
    TasksTheme {
        CalendarRow(eventUri = null, selectedCalendar = null, onClick = {}, clear = {})
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NewCalendar() {
    TasksTheme {
        CalendarRow(eventUri = null, selectedCalendar = "Personal", onClick = {}, clear = {})
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun ExistingCalendar() {
    TasksTheme {
        CalendarRow(eventUri = "abcd", selectedCalendar = null, onClick = {}, clear = {})
    }
}