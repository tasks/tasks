package org.tasks.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toPersistentSet
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.ClearButton
import org.tasks.compose.edit.AlarmDialogs
import org.tasks.data.entity.Alarm
import org.tasks.reminders.ReminderControlSetViewModel
import org.tasks.reminders.alarmText
import tasks.kmp.generated.resources.EPr_default_reminders_title
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.add_reminder

private const val DISABLED_ALPHA = 0.38f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultRemindersDialog(
    vm: ReminderControlSetViewModel,
    initialAlarms: List<Alarm>,
    is24HourFormat: Boolean,
    onAlarmsChanged: (List<Alarm>) -> Unit,
    onDismiss: () -> Unit,
) {
    val viewState by vm.viewState.collectAsState()
    var alarms by remember { mutableStateOf(initialAlarms.toSet()) }
    val dismiss = {
        vm.showCustomDialog(visible = false)
        vm.showRandomDialog(visible = false)
        vm.showAddAlarm(visible = false)
        onDismiss()
    }
    val update: (Set<Alarm>) -> Unit = {
        alarms = it
        onAlarmsChanged(it.toList())
    }

    BasicAlertDialog(onDismissRequest = dismiss) {
        DefaultRemindersList(
            alarms = alarms,
            is24HourFormat = is24HourFormat,
            onAlarmClick = { alarm ->
                vm.setReplace(alarm)
                vm.showAddAlarm(visible = true)
            },
            onAlarmRemove = { update(alarms - it) },
            onAddClick = { vm.showAddAlarm(visible = true) },
        )
    }

    AlarmDialogs(
        vm = vm,
        viewState = viewState,
        alarms = alarms.toPersistentSet(),
        addAlarm = { update(alarms + it) },
        deleteAlarm = { update(alarms - it) },
        pickDateAndTime = { },
        showDateTimePicker = false,
    )
}

@Composable
fun DefaultRemindersList(
    alarms: Set<Alarm>,
    is24HourFormat: Boolean,
    onAlarmClick: (Alarm) -> Unit,
    onAlarmRemove: (Alarm) -> Unit,
    onAddClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(Res.string.EPr_default_reminders_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))
            alarms.forEach { alarm ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlarmClick(alarm) }
                ) {
                    Text(
                        text = alarmText(alarm, is24HourFormat),
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .weight(weight = 1f),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    ClearButton(onClick = { onAlarmRemove(alarm) })
                }
            }
            Text(
                text = stringResource(Res.string.add_reminder),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .clickable(onClick = onAddClick)
            )
        }
    }
}
