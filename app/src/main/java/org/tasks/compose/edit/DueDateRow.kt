package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.compose.TaskEditRow
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.themes.TasksTheme

@Composable
fun DueDateRow(
    dueDate: Long,
    hasDueDateAlarm: Boolean,
    is24HourFormat: Boolean,
    alwaysDisplayFullDate: Boolean,
    onClick: () -> Unit,
) {
    val overdue = remember (dueDate) {
        when {
            Task.hasDueTime(dueDate) -> newDateTime(dueDate).isBeforeNow
            dueDate > 0 -> newDateTime(dueDate).endOfDay().isBeforeNow
            else -> false
        }
    }
    DueDateRow(
        dueDate = if (dueDate == 0L) {
            stringResource(id = R.string.no_due_date)
        } else {
            runBlocking {
                getRelativeDateTime(
                    dueDate,
                    is24HourFormat,
                    DateStyle.FULL,
                    alwaysDisplayFullDate = alwaysDisplayFullDate
                )
            }
        },
        color = when {
            overdue -> MaterialTheme.colorScheme.error
            dueDate == 0L && hasDueDateAlarm -> MaterialTheme.colorScheme.error
            dueDate == 0L -> MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
            else -> MaterialTheme.colorScheme.onSurface
        },
        onClick = { onClick() },
    )
}

@Composable
private fun DueDateRow(
    dueDate: String,
    color: Color,
    onClick: () -> Unit,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_outline_schedule_24px,
        content = {
            DueDate(
                dueDate = dueDate,
                color = color,
            )
        },
        onClick = onClick,
    )
}

@Composable
fun DueDate(
    dueDate: String,
    color: Color,
) {
    Text(
        text = dueDate,
        color = color,
        modifier = Modifier.padding(top = 20.dp, bottom = 20.dp, end = 16.dp)
    )
}

@ExperimentalComposeUiApi
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun DueDatePreview() {
    TasksTheme {
        DueDateRow(
            dueDate = "Today",
            color = MaterialTheme.colorScheme.onSurface,
        ) {}
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoDueDatePreview() {
    TasksTheme {
        DueDateRow(
            dueDate = stringResource(R.string.no_due_date),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled),
        ) {}
    }
}