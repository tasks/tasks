package com.todoroo.astrid.ui

import androidx.compose.material3.DisplayMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.compose.edit.StartDateRow
import org.tasks.compose.pickers.StartDatePickerSheet
import org.tasks.compose.pickers.labelWithTime
import org.tasks.compose.rememberDateFormatter
import org.tasks.data.entity.Alarm
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.extensions.hideKeyboardThen
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay
import org.tasks.time.withMillisOfDay
import org.tasks.ui.TaskEditControlFragment
import javax.inject.Inject

@AndroidEntryPoint
class StartDateControlSet : TaskEditControlFragment() {
    @Inject lateinit var preferences: Preferences

    private val vm: StartDateViewModel by viewModels()

    @Composable
    override fun Content() {
        LaunchedEffect(Unit) {
            vm.init(
                dueDate = viewModel.dueDate.value,
                startDate = viewModel.startDate.value,
                isNew = viewModel.viewState.value.isNew
            )
        }
        val is24Hour = remember { requireContext().is24HourFormat }
        val dueDate = viewModel.dueDate.collectAsStateWithLifecycle().value
        val selectedDay = vm.selectedDay.collectAsStateWithLifecycle().value
        val selectedTime = vm.selectedTime.collectAsStateWithLifecycle().value
        val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
        var showPicker by rememberSaveable { mutableStateOf(false) }
        val today = currentTimeMillis().startOfDay()
        val dateFormatter = rememberDateFormatter(is24Hour)
        val printedStartDate = remember(
            selectedDay, selectedTime, is24Hour, preferences.alwaysDisplayFullDate, today,
            dateFormatter,
        ) {
            if (selectedDay > 0) {
                dateFormatter?.relativeDateTime(
                    selectedDay.withMillisOfDay(selectedTime),
                    DateStyle.FULL,
                    alwaysDisplayFullDate = preferences.alwaysDisplayFullDate,
                ) ?: ""
            } else {
                ""
            }
        }
        StartDateRow(
            startDate = viewModel.startDate.collectAsStateWithLifecycle().value,
            selectedDay = selectedDay,
            selectedTime = selectedTime,
            hasStartAlarm = remember (viewState.alarms) {
                viewState.alarms.any { it.type == Alarm.TYPE_REL_START }
            },
            hasDueDate = dueDate > 0,
            printDate = { printedStartDate },
            onClick = {
                val show = { showPicker = true }
                activity?.hideKeyboardThen(show) ?: show()
            }
        )

        if (showPicker) {
            StartDatePickerSheet(
                initialDay = selectedDay,
                initialTime = selectedTime,
                is24Hour = is24Hour,
                autoClose = preferences.getBoolean(
                    R.string.p_auto_dismiss_datetime_edit_screen,
                    false
                ),
                showDueDate = !viewState.list.account.isOpenTasks,
                times = remember { preferences.quickPickTimes },
                initialDateInputMode = preferences.calendarDisplayMode == DisplayMode.Input,
                onDateInputModeChange = {
                    preferences.calendarDisplayMode = if (it) DisplayMode.Input else DisplayMode.Picker
                },
                initialTimeInputMode = preferences.timeDisplayMode == DisplayMode.Input,
                onTimeInputModeChange = {
                    preferences.timeDisplayMode = if (it) DisplayMode.Input else DisplayMode.Picker
                },
                onSelected = { day, time ->
                    vm.setSelected(day, time)
                    applySelected()
                    showPicker = false
                },
                onDismiss = { showPicker = false },
            )
        }

        LaunchedEffect(dueDate) {
            applySelected()
        }
    }

    private fun applySelected() {
        viewModel.setStartDate(vm.getSelectedValue(viewModel.dueDate.value))
    }

    companion object {
        val TAG = R.string.TEA_ctrl_hide_until_pref

        @Composable
        internal fun getRelativeDateString(resId: Int, time: Int): String =
            labelWithTime(stringResource(resId), time, LocalContext.current.is24HourFormat)
    }
}
