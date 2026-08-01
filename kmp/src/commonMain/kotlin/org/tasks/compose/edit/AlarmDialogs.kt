package org.tasks.compose.edit

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableSet
import org.tasks.compose.pickers.AddAlarmDialog
import org.tasks.compose.pickers.AddCustomReminderDialog
import org.tasks.compose.pickers.AddRandomReminderDialog
import org.tasks.data.entity.Alarm
import org.tasks.reminders.ReminderControlSetViewModel
import org.tasks.reminders.ReminderControlSetViewModel.ViewState

@Composable
fun AlarmDialogs(
    vm: ReminderControlSetViewModel,
    viewState: ViewState,
    alarms: ImmutableSet<Alarm>,
    addAlarm: (Alarm) -> Unit,
    deleteAlarm: (Alarm) -> Unit,
    pickDateAndTime: (replace: Alarm?) -> Unit,
) {
    val replaceAlarm: (Alarm) -> Unit = {
        viewState.replace?.let(deleteAlarm)
        addAlarm(it)
    }

    AddAlarmDialog(
        viewState = viewState,
        existingAlarms = alarms,
        addAlarm = replaceAlarm,
        addRandom = { vm.showRandomDialog(visible = true) },
        addCustom = { vm.showCustomDialog(visible = true) },
        pickDateAndTime = { pickDateAndTime(viewState.replace) },
        dismiss = { vm.showAddAlarm(visible = false) },
    )

    val cameFromChooser = viewState.replace == null

    if (viewState.showCustomDialog) {
        AddCustomReminderDialog(
            alarm = viewState.replace,
            updateAlarm = replaceAlarm,
            closeDialog = { vm.showCustomDialog(visible = false) },
            cancelDialog = {
                vm.showCustomDialog(visible = false)
                if (cameFromChooser) {
                    vm.showAddAlarm(visible = true)
                }
            },
        )
    }

    if (viewState.showRandomDialog) {
        AddRandomReminderDialog(
            alarm = viewState.replace,
            updateAlarm = replaceAlarm,
            closeDialog = { vm.showRandomDialog(visible = false) },
            cancelDialog = {
                vm.showRandomDialog(visible = false)
                if (cameFromChooser) {
                    vm.showAddAlarm(visible = true)
                }
            },
        )
    }
}
