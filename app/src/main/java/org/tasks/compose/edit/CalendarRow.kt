package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditRow

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
                            .padding(vertical = 20.dp)
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
    MdcTheme {
        CalendarRow(eventUri = null, selectedCalendar = null, onClick = {}, clear = {})
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NewCalendar() {
    MdcTheme {
        CalendarRow(eventUri = null, selectedCalendar = "Personal", onClick = {}, clear = {})
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun ExistingCalendar() {
    MdcTheme {
        CalendarRow(eventUri = "abcd", selectedCalendar = null, onClick = {}, clear = {})
    }
}