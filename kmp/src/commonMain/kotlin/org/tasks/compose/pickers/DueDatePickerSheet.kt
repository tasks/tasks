package org.tasks.compose.pickers

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.tasks.data.entity.Task
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.millisOfDay
import org.tasks.time.plusDays
import org.tasks.time.startOfDay
import org.tasks.time.startOfMinute
import org.tasks.time.withMillisOfDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueDatePickerSheet(
    initialDay: Long,
    initialTime: Int,
    is24Hour: Boolean,
    autoClose: Boolean = false,
    showNoDate: Boolean = true,
    showNoTime: Boolean = true,
    times: QuickPickTimes = QuickPickTimes(),
    initialDateInputMode: Boolean = false,
    onDateInputModeChange: (Boolean) -> Unit = {},
    initialTimeInputMode: Boolean = false,
    onTimeInputModeChange: (Boolean) -> Unit = {},
    onSelected: (day: Long, time: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { currentTimeMillis().startOfDay() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dismiss = rememberDismissWithAnimation(sheetState)
    val state = rememberDateSheetState(
        initialDay = initialDay,
        initialTime = initialDueTime(initialTime),
        autoClose = autoClose,
        onSelected = { day, time -> dismiss { onSelected(day, time) } },
        onDismiss = { dismiss(onDismiss) },
    )

    fun applyTime(millisOfDay: Int) {
        state.setDayTime(dayForAppliedTime(state.selectedDay, today, millisOfDay), millisOfDay)
    }

    DatePickerSheet(
        sheetState = sheetState,
        selectedDay = state.selectedDay,
        moveDisplayedMonth = true,
        autoClose = autoClose,
        initialDateInputMode = initialDateInputMode,
        onDateInputModeChange = onDateInputModeChange,
        onDaySelected = { state.setDay(it) },
        onAccept = { state.commit() },
        onDismiss = { dismiss(onDismiss) },
        dateShortcuts = {
            DueDateShortcuts(
                today = today,
                tomorrow = remember { today.plusDays(1) },
                nextWeek = remember { today.plusDays(7) },
                selected = state.selectedDay,
                showNoDate = showNoDate,
                selectedDay = { state.setDay(it.startOfDay()) },
                clearDate = { state.clearDate() },
            )
        },
        timeShortcuts = {
            SheetTimeShortcuts(
                day = NO_DAY,
                selectedTime = state.selectedTime,
                today = today,
                is24Hour = is24Hour,
                times = times,
                initialTimeInputMode = initialTimeInputMode,
                onTimeInputModeChange = onTimeInputModeChange,
                applyTime = { applyTime(it) },
                clearTime = {
                    state.selectedTime = NO_TIME
                    state.autoCommit()
                },
                showNoTime = showNoTime,
            )
        },
    )
}

internal fun initialDueTime(time: Int): Int =
    time.takeIf { it == MULTIPLE_TIMES || Task.hasDueTime(it.toLong()) } ?: NO_TIME

internal fun dayForAppliedTime(selectedDay: Long, today: Long, millisOfDay: Int): Long =
    if (selectedDay == NO_DAY && millisOfDay > NO_TIME) dayForNewTime(today, millisOfDay) else selectedDay

fun dueDateFromSelection(day: Long, time: Int): Long = when {
    day == NO_DAY -> 0L
    time <= NO_TIME -> day
    else -> day.withMillisOfDay(time)
}

fun dueDateToSelection(dueDate: Long): Pair<Long, Int> =
    (if (dueDate > 0) dueDate.startOfDay() else NO_DAY) to dueDate.millisOfDay

fun alarmToSelection(timestamp: Long): Pair<Long, Int> =
    if (timestamp > 0) {
        timestamp.startOfDay() to timestamp.startOfMinute().millisOfDay + TIME_MARKER
    } else {
        NO_DAY to NO_TIME
    }

fun alarmFromSelection(day: Long, time: Int): Long =
    dueDateFromSelection(day, time).takeIf { it > 0 }?.startOfMinute() ?: 0L
