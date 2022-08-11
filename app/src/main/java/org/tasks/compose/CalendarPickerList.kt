package org.tasks.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Event
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import org.tasks.R
import org.tasks.calendars.AndroidCalendar

@Composable
fun CalendarPickerList(
    calendars: List<AndroidCalendar>,
    selected: String?,
    onClick: (AndroidCalendar?) -> Unit,
) {
    val selectedCalendar = calendars.find { it.id == selected }
    MdcTheme {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            CheckableIconRow(
                icon = Icons.Outlined.Block,
                tint = MaterialTheme.colors.onSurface,
                text = stringResource(id = R.string.dont_add_to_calendar),
                selected = selectedCalendar == null,
                onClick = { onClick(null) },
            )
            calendars.forEach {
                CheckableIconRow(
                    icon = Icons.Outlined.Event,
                    tint = Color(it.color),
                    text = it.name,
                    selected = selectedCalendar == it,
                    onClick = { onClick(it) }
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun CalendarPicker() {
    MdcTheme {
        CalendarPickerList(
            calendars = listOf(
                AndroidCalendar("1", "Home", -765666),
                AndroidCalendar("2", "Work", -5434281),
                AndroidCalendar("3", "Personal", -10395295),
            ),
            selected = "2",
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun CalendarPickerNoneSelected() {
    MdcTheme {
        CalendarPickerList(
            calendars = listOf(
                AndroidCalendar("1", "Home", -765666),
                AndroidCalendar("2", "Work", -5434281),
                AndroidCalendar("3", "Personal", -10395295),
            ),
            selected = null,
            onClick = {},
        )
    }
}