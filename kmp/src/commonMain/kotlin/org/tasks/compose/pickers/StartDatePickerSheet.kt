package org.tasks.compose.pickers

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.tasks.data.entity.Task
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartDatePickerSheet(
    initialDay: Long,
    initialTime: Int,
    is24Hour: Boolean,
    autoClose: Boolean = false,
    showDueDate: Boolean = true,
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
        initialTime = initialStartTime(initialTime),
        autoClose = autoClose,
        onSelected = { day, time -> dismiss { onSelected(day, time) } },
        onDismiss = { dismiss(onDismiss) },
    )

    fun applyTime(millisOfDay: Int) {
        val day = if (state.selectedDay != NO_DAY) {
            demoteDueTime(state.selectedDay)
        } else {
            dayForNewTime(today, millisOfDay)
        }
        state.setDayTime(day, millisOfDay)
    }

    DatePickerSheet(
        sheetState = sheetState,
        selectedDay = state.selectedDay,
        moveDisplayedMonth = false,
        autoClose = autoClose,
        initialDateInputMode = initialDateInputMode,
        onDateInputModeChange = onDateInputModeChange,
        onDaySelected = { state.setDay(it) },
        onAccept = { state.commit() },
        onDismiss = { dismiss(onDismiss) },
        dateShortcuts = {
            StartDateShortcuts(
                selected = state.selectedDay,
                selectedDay = { state.setDay(it) },
                selectedDayTime = { day, time -> state.setDayTime(day, time) },
                showDueDate = showDueDate,
                clearDate = { state.clearDate() },
            )
        },
        timeShortcuts = {
            SheetTimeShortcuts(
                day = state.selectedDay,
                selectedTime = state.selectedTime,
                today = today,
                is24Hour = is24Hour,
                times = times,
                initialTimeInputMode = initialTimeInputMode,
                onTimeInputModeChange = onTimeInputModeChange,
                applyTime = { applyTime(it) },
                clearTime = {
                    state.selectedDay = demoteDueTime(state.selectedDay)
                    state.selectedTime = NO_TIME
                    state.autoCommit()
                },
            )
        },
    )
}

internal fun initialStartTime(time: Int): Int =
    time.takeIf { Task.hasDueTime(it.toLong()) } ?: NO_TIME
