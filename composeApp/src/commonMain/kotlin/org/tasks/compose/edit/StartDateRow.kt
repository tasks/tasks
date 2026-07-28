package org.tasks.compose.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.pickers.StartDate
import org.tasks.compose.pickers.labelWithTime
import org.tasks.compose.pickers.startDayOf
import org.tasks.compose.rememberRelativeDateTime
import org.tasks.themes.TasksIcons
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.day_before_due
import tasks.kmp.generated.resources.due_date
import tasks.kmp.generated.resources.due_time
import tasks.kmp.generated.resources.no_start_date
import tasks.kmp.generated.resources.start_date
import tasks.kmp.generated.resources.week_before_due

@Composable
fun StartDateRow(
    startDate: Long,
    selectedDay: Long,
    selectedTime: Int,
    hasDueDate: Boolean,
    hasStartAlarm: Boolean,
    is24Hour: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alwaysDisplayFullDate: Boolean = false,
) {
    val startDay = startDayOf(selectedDay)
    val value = when (startDay) {
        StartDate.DueDate -> labelWithTime(stringResource(Res.string.due_date), selectedTime, is24Hour)
        StartDate.DueTime -> stringResource(Res.string.due_time)
        StartDate.DayBeforeDue -> labelWithTime(stringResource(Res.string.day_before_due), selectedTime, is24Hour)
        StartDate.WeekBeforeDue -> labelWithTime(stringResource(Res.string.week_before_due), selectedTime, is24Hour)
        is StartDate.Absolute -> rememberRelativeDateTime(
            startDate,
            is24Hour,
            alwaysDisplayFullDate = alwaysDisplayFullDate,
        )
        StartDate.None -> stringResource(Res.string.no_start_date)
    }
    val overdue = (startDay.isRelative && !hasDueDate) ||
        (startDate != 0L && startDate < currentTimeMillis())
    TaskEditDateRow(
        value = value,
        overdue = overdue,
        isEmpty = startDate == 0L,
        missingDate = startDate == 0L && hasStartAlarm,
        title = stringResource(Res.string.start_date),
        icon = TasksIcons.PENDING_ACTIONS,
        onClick = onClick,
        modifier = modifier,
    )
}
