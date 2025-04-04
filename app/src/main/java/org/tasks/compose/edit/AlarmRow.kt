package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todoroo.astrid.ui.ReminderControlSetViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import org.tasks.R
import org.tasks.compose.AddAlarmDialog
import org.tasks.compose.AddReminderDialog
import org.tasks.compose.ClearButton
import org.tasks.compose.TaskEditRow
import org.tasks.data.entity.Alarm
import org.tasks.reminders.AlarmToString
import org.tasks.themes.TasksTheme

@OptIn(ExperimentalComposeUiApi::class)
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
                Alarms(
                    alarms = alarms,
                    ringMode = ringMode,
                    isNew = isNew,
                    hasStartDate = hasStartDate,
                    hasDueDate = hasDueDate,
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
                        text = stringResource(id = R.string.enable_reminders),
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(id = R.string.enable_reminders_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            AddAlarmDialog(
                viewState = viewState,
                existingAlarms = alarms,
                addAlarm = {
                    viewState.replace?.let(deleteAlarm)
                    addAlarm(it)
                },
                addRandom = { vm.showRandomDialog(visible = true) },
                addCustom = { vm.showCustomDialog(visible = true) },
                pickDateAndTime = { pickDateAndTime(viewState.replace) },
                dismiss = { vm.showAddAlarm(visible = false) },
            )

            AddReminderDialog.AddCustomReminderDialog(
                viewState = viewState,
                addAlarm = {
                    viewState.replace?.let(deleteAlarm)
                    addAlarm(it)
                },
                closeDialog = { vm.showCustomDialog(visible = false) }
            )

            AddReminderDialog.AddRandomReminderDialog(
                viewState = viewState,
                addAlarm = {
                    viewState.replace?.let(deleteAlarm)
                    addAlarm(it)
                },
                closeDialog = { vm.showRandomDialog(visible = false) }
            )
        },
    )
}

@Composable
fun Alarms(
    alarms: ImmutableSet<Alarm>,
    ringMode: Int,
    isNew: Boolean,
    hasStartDate: Boolean,
    hasDueDate: Boolean,
    replaceAlarm: (Alarm) -> Unit,
    addAlarm: () -> Unit,
    deleteAlarm: (Alarm) -> Unit,
    openRingType: () -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        alarms.forEach { alarm ->
            AlarmRow(
                text = AlarmToString(LocalContext.current).toString(alarm),
                color = when (alarm.type) {
                    Alarm.TYPE_REL_START -> if (hasStartDate) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    Alarm.TYPE_REL_END -> if (hasDueDate) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    else -> MaterialTheme.colorScheme.onSurface
                },
                onClick = { replaceAlarm(alarm) },
                remove = { deleteAlarm(alarm) }
            )
        }
        val showError = remember(alarms, hasDueDate, hasStartDate) {
            isNew && alarms.isEmpty() && (hasDueDate || hasStartDate)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.add_reminder),
                color = when {
                    showError -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 12.dp, end = 16.dp)
                    .defaultMinSize(24.dp)
                    .clickable(onClick = addAlarm)
            )
            Spacer(modifier = Modifier.weight(1f))
            if (alarms.isNotEmpty()) {
                Text(
                    text = stringResource(
                        id = when (ringMode) {
                            2 -> R.string.ring_nonstop
                            1 -> R.string.ring_five_times
                            else -> R.string.ring_once
                        }
                    ),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                        .clickable(onClick = openRingType),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AlarmRow(
    text: String,
    color: Color,
    onClick: () -> Unit,
    remove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .weight(weight = 1f),
            color = color,
        )
        ClearButton(onClick = remove)
    }
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