package org.tasks.compose.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.rememberRelativeDateTime
import org.tasks.themes.TasksIcons
import org.tasks.time.dueDateOverdue
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.due_date
import tasks.kmp.generated.resources.no_due_date

@Composable
fun DueDateRow(
    dueDate: Long,
    hasDueDateAlarm: Boolean,
    is24Hour: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alwaysDisplayFullDate: Boolean = false,
) {
    val overdue = dueDateOverdue(dueDate)
    val value = if (dueDate == 0L) {
        stringResource(Res.string.no_due_date)
    } else {
        rememberRelativeDateTime(dueDate, is24Hour, alwaysDisplayFullDate = alwaysDisplayFullDate)
    }
    TaskEditDateRow(
        value = value,
        overdue = overdue,
        isEmpty = dueDate == 0L,
        missingDate = dueDate == 0L && hasDueDateAlarm,
        title = stringResource(Res.string.due_date),
        icon = TasksIcons.SCHEDULE,
        onClick = onClick,
        modifier = modifier,
    )
}
