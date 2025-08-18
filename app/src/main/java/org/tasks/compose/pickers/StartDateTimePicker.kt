package org.tasks.compose.pickers

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.dialogs.BaseDateTimePicker
import org.tasks.dialogs.StartDatePicker.Companion.DUE_DATE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_TIME
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartDateTimePicker (
    selectedDay: Long,
    selectedTime: Int,
    updateValues: (Long, Int) -> Unit,
    accept: () -> Unit,
    dismiss: () -> Unit,
    autoclose: Boolean,
    showDueDate: Boolean,
    onDismissHandler: BaseDateTimePicker.OnDismissHandler? = null
) {
    val context = LocalContext.current
    val preferences = remember { Preferences(context) }
    val state = rememberDatePickerState(
        initialDisplayMode = remember { preferences.calendarDisplayMode },
    )

    val today = remember { newDateTime().startOfDay() }

    fun returnDate(day: Long = selectedDay, time: Int = selectedTime) {
        if (day != selectedDay || time != selectedTime) {
            updateValues(day, time)
        }
        if (autoclose) accept()
    }

    fun returnSelectedTime(millisOfDay: Int) {
        val day = when {
            selectedDay == DUE_TIME -> DUE_DATE
            selectedDay != 0L -> selectedDay
            today.withMillisOfDay(millisOfDay).isAfterNow -> today.millis
            else -> today.plusDays(1).millis
        }
        returnDate(day = day, time = millisOfDay)
    }

    DatePickerBottomSheet(
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        ),
        state = state,
        showButtons = !autoclose,
        setDisplayMode = { preferences.calendarDisplayMode = it },
        cancel = { dismiss(); onDismissHandler?.onDismiss() },
        accept = accept,
        dateShortcuts = {
            StartDateShortcuts(
                selected = selectedDay,
                selectedDay = { returnDate(it) },
                selectedDayTime = { day, time -> returnDate(day, time) },
                showDueDate = showDueDate,
                clearDate = { returnDate(day = 0, time = 0) },
            )
        },
        timeShortcuts = {
            var showTimePicker by rememberSaveable { mutableStateOf(false) }
            if (showTimePicker) {
                val time = if (selectedTime < 0 || !Task.hasDueTime(
                        today.withMillisOfDay(selectedTime).millis
                    )
                ) {
                    today.noon().millisOfDay
                } else {
                    selectedTime
                }
                TimePickerDialog(
                    state = rememberTimePickerState(
                        initialHour = time / (60 * 60_000),
                        initialMinute = (time / (60_000)) % 60,
                        is24Hour = LocalContext.current.is24HourFormat
                    ),
                    initialDisplayMode = remember { preferences.timeDisplayMode },
                    setDisplayMode = { preferences.timeDisplayMode = it },
                    selected = { returnSelectedTime(it + 1000) },
                    dismiss = { showTimePicker = false }
                )
            }
            TimeShortcuts(
                day = selectedDay,
                selected = selectedTime,
                morning = remember { preferences.dateShortcutMorning + 1000 },
                afternoon = remember { preferences.dateShortcutAfternoon + 1000 },
                evening = remember { preferences.dateShortcutEvening + 1000 },
                night = remember { preferences.dateShortcutNight + 1000 },
                selectedMillisOfDay = { returnSelectedTime(it) },
                pickTime = { showTimePicker = true },
                clearTime = {
                    returnDate(
                        day = when (selectedDay) {
                            DUE_TIME -> DUE_DATE
                            else -> selectedDay
                        },
                        time = 0
                    )
                }
            )
        }
    )
    LaunchedEffect(selectedDay) {
        if (selectedDay > 0) {
            state.selectedDateMillis = selectedDay + (DateTime(selectedDay).offset)
        } else {
            state.selectedDateMillis = null
        }
    }
    LaunchedEffect(state.selectedDateMillis) {
        if (state.selectedDateMillis == selectedDay + (DateTime(selectedDay).offset)) {
            return@LaunchedEffect
        }
        state.selectedDateMillis?.let {
            returnDate(day = it - DateTime(it).offset)
        }
    }
}