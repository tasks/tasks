package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.compose.resources.stringResource
import org.tasks.R
import org.tasks.compose.TaskEditRow
import org.tasks.data.entity.Alarm
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.reminders.ReminderControlSetViewModel
import org.tasks.themes.TasksTheme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.enable_reminders
import tasks.kmp.generated.resources.enable_reminders_description

@Composable
fun AlarmRow(
    vm: ReminderControlSetViewModel = viewModel(),
    hasNotificationPermissions: Boolean,
    fixNotificationPermissions: () -> Unit,
    alarms: ImmutableSet<Alarm>,
    ringMode: Int,
    isNew: Boolean,
    hasStartDate: Boolean,
    hasDueDate: Boolean,
    addAlarm: (Alarm) -> Unit,
    deleteAlarm: (Alarm) -> Unit,
    openRingType: () -> Unit,
    pickDateAndTime: (replace: Alarm?) -> Unit,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_outline_notifications_24px,
        content = {
            val viewState = vm.viewState.collectAsStateWithLifecycle().value
            if (hasNotificationPermissions) {
                AlarmList(
                    alarms = alarms,
                    ringMode = ringMode,
                    isNew = isNew,
                    hasStartDate = hasStartDate,
                    hasDueDate = hasDueDate,
                    is24HourFormat = LocalContext.current.is24HourFormat,
                    replaceAlarm = {
                        vm.setReplace(it)
                        vm.showAddAlarm(visible = true)
                    },
                    addAlarm = {
                        vm.showAddAlarm(visible = true)
                    },
                    deleteAlarm = deleteAlarm,
                    openRingType = openRingType,
                )
            } else {
                Column(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .clickable { fixNotificationPermissions() }
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(Res.string.enable_reminders),
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(Res.string.enable_reminders_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
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
        },
    )
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoAlarms() {
    TasksTheme {
        AlarmRow(
            alarms = persistentSetOf(),
            ringMode = 0,
            isNew = false,
            hasStartDate = true,
            hasDueDate = true,
            addAlarm = {},
            deleteAlarm = {},
            openRingType = {},
            hasNotificationPermissions = true,
            fixNotificationPermissions = {},
            pickDateAndTime = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun PermissionDenied() {
    TasksTheme {
        AlarmRow(
            alarms = persistentSetOf(),
            ringMode = 0,
            isNew = false,
            hasStartDate = true,
            hasDueDate = true,
            addAlarm = {},
            deleteAlarm = {},
            openRingType = {},
            hasNotificationPermissions = false,
            fixNotificationPermissions = {},
            pickDateAndTime = {},
        )
    }
}
