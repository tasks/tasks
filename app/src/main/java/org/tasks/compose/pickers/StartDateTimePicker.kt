package org.tasks.compose.pickers

import android.content.Context
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
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.dialogs.BaseDateTimePicker
import org.tasks.dialogs.StartDatePicker.Companion.DUE_DATE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_TIME
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.kmp.org.tasks.time.getTimeString
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.millisOfDay
import org.tasks.time.startOfDay
import org.tasks.time.withMillisOfDay

object StartDateTimePicker {
    // This is a copy of special constants declarations from StartDatePicker
    const val NO_DAY = 0L
    const val NO_TIME = 0
    const val DUE_DATE = -1L
    const val DAY_BEFORE_DUE = -2L
    const val WEEK_BEFORE_DUE = -3L
    const val DUE_TIME = -4L

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun StartDateTimePicker(
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

    fun encodeDay(current: Long, dueDate: Long): Long {
        if (current <= 0L) return current
        return encodeStartDay(
            dueDate,
            current.startOfDay(),
            current.millisOfDay)
    }

    fun encodeTime(current: Long, day: Long, dueDate: Long): Int =
        if (current > 0L &&
            day == dueDate.startOfDay() &&
            current.millisOfDay == dueDate.millisOfDay)
        {
            NO_TIME
        } else {
            current.millisOfDay
        }

    fun encodeStartDay(dueDate: Long, selectedDay: Long, selectedTime: Int): Long {
        val dueDay = dueDate.startOfDay()
        val dueTime = dueDate.millisOfDay
        return when {
            dueDate <= 0 -> selectedDay
            dueDay == selectedDay -> if (selectedTime == dueTime) {
                DUE_TIME
            } else {
                DUE_DATE
            }
            dueDay.toDateTime().minusDays(1).millis == selectedDay ->
                DAY_BEFORE_DUE
            dueDay.toDateTime().minusDays(7).millis == selectedDay ->
                WEEK_BEFORE_DUE
            else -> selectedDay
        }
    }

    fun decodeStartDay(dueDate: Long, selectedDay: Long, selectedTime: Int): Long {
        val due = dueDate.takeIf { it > 0 }?.toDateTime()
        return when (selectedDay) {
            DUE_DATE -> due?.withMillisOfDay(selectedTime)?.millis ?: 0
            DUE_TIME -> due?.millis ?: 0
            DAY_BEFORE_DUE -> due?.minusDays(1)?.withMillisOfDay(selectedTime)?.millis ?: 0
            WEEK_BEFORE_DUE -> due?.minusDays(7)?.withMillisOfDay(selectedTime)?.millis ?: 0
            else -> selectedDay + selectedTime
        }
    }

    // copy from StartDateControlSet
    internal fun Context.getRelativeDateString(resId: Int, time: Int) =
        if (time == NO_TIME) {
            getString(resId)
        } else {
            "${getString(resId)} ${getTimeString(currentTimeMillis().withMillisOfDay(time), this.is24HourFormat)}"
        }

}