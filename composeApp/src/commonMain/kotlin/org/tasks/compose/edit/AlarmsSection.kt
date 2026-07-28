package org.tasks.compose.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableSet
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.settings.CardPosition
import org.tasks.data.entity.Alarm
import org.tasks.reminders.ReminderControlSetViewModel
import org.tasks.themes.TasksIcons
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.add_reminder
import tasks.kmp.generated.resources.notifications

@Composable
fun AlarmsSection(
    vm: ReminderControlSetViewModel,
    alarms: ImmutableSet<Alarm>,
    isNew: Boolean,
    hasStartDate: Boolean,
    hasDueDate: Boolean,
    is24HourFormat: Boolean,
    addAlarm: (Alarm) -> Unit,
    deleteAlarm: (Alarm) -> Unit,
    pickDateAndTime: (replace: Alarm?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewState = vm.viewState.collectAsState().value
    val showError = alarms.missingReminder(isNew, hasStartDate, hasDueDate)
    val tint = if (showError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TaskEditCardGap),
    ) {
        TaskEditCard(
            onClick = { vm.showAddAlarm(visible = true) },
            position = if (alarms.isEmpty()) CardPosition.Only else CardPosition.First,
        ) {
            TaskEditCardRowContent(
                value = stringResource(Res.string.add_reminder),
                valueColor = tint,
                title = stringResource(Res.string.notifications),
                icon = TasksIcons.NOTIFICATIONS,
                iconTint = tint,
            )
        }
        if (alarms.isNotEmpty()) {
            TaskEditCard(position = CardPosition.Last) {
                Column {
                    alarms.forEach { alarm ->
                        AlarmListItem(
                            alarm = alarm,
                            hasStartDate = hasStartDate,
                            hasDueDate = hasDueDate,
                            is24HourFormat = is24HourFormat,
                            onClick = {
                                vm.setReplace(alarm)
                                vm.showAddAlarm(visible = true)
                            },
                            remove = { deleteAlarm(alarm) },
                            textPadding = TaskEditCardContentStartPadding,
                        )
                    }
                }
            }
        }
    }

    AlarmDialogs(
        vm = vm,
        viewState = viewState,
        alarms = alarms,
        addAlarm = addAlarm,
        deleteAlarm = deleteAlarm,
        pickDateAndTime = pickDateAndTime,
    )
}
