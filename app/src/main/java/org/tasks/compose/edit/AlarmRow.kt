package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.todoroo.astrid.ui.ReminderControlSetViewModel
import org.tasks.R
import org.tasks.compose.AddAlarmDialog
import org.tasks.compose.AddReminderDialog
import org.tasks.compose.ClearButton
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditRow
import org.tasks.data.entity.Alarm
import org.tasks.reminders.AlarmToString
import org.tasks.themes.TasksTheme
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AlarmRow(
    vm: ReminderControlSetViewModel = viewModel(),
    hasNotificationPermissions: Boolean,
    fixNotificationPermissions: () -> Unit,
    alarms: List<Alarm>,
    ringMode: Int,
    locale: Locale,
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
                    locale = locale,
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
                        color = colorResource(id = org.tasks.kmp.R.color.red_500),
                    )
                    Text(
                        text = stringResource(id = R.string.enable_reminders_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorResource(id = org.tasks.kmp.R.color.red_500),
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
    alarms: List<Alarm>,
    ringMode: Int,
    locale: Locale,
    replaceAlarm: (Alarm) -> Unit,
    addAlarm: () -> Unit,
    deleteAlarm: (Alarm) -> Unit,
    openRingType: () -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        alarms.forEach { alarm ->
            AlarmRow(
                text = AlarmToString(LocalContext.current, locale).toString(alarm),
                onClick = { replaceAlarm(alarm) },
                remove = { deleteAlarm(alarm) }
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            DisabledText(
                text = stringResource(id = R.string.add_reminder),
                modifier = Modifier
                    .padding(vertical = 12.dp)
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
            color = MaterialTheme.colorScheme.onSurface,
        )
        ClearButton(onClick = remove)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoAlarms() {
    TasksTheme {
        AlarmRow(
            alarms = emptyList(),
            ringMode = 0,
            locale = Locale.getDefault(),
            addAlarm = {},
            deleteAlarm = {},
            openRingType = {},
            hasNotificationPermissions = true,
            fixNotificationPermissions = {},
            pickDateAndTime = {},
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun PermissionDenied() {
    TasksTheme {
        AlarmRow(
            alarms = emptyList(),
            ringMode = 0,
            locale = Locale.getDefault(),
            addAlarm = {},
            deleteAlarm = {},
            openRingType = {},
            hasNotificationPermissions = false,
            fixNotificationPermissions = {},
            pickDateAndTime = {},
        )
    }
}