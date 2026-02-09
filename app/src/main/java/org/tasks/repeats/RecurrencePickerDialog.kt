package org.tasks.repeats

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.tasks.data.entity.Task
import org.tasks.preferences.Preferences
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrencePickerDialog (
    dismiss: () -> Unit,
    recurrence: String?,
    onRecurrenceChanged: (String?) -> Unit,
    repeatFrom: @Task.RepeatFrom Int,
    onRepeatFromChanged: (@Task.RepeatFrom Int) -> Unit,
    accountType: Int,
) {
    val context = LocalContext.current
    val preferences = remember { Preferences(context) }

    val basicDialog = remember { mutableStateOf(true) }
    if (basicDialog.value) {
        BasicRecurrencePicker(
            dismiss = dismiss,
            recurrence = recurrence,
            setRecurrence = onRecurrenceChanged,
            repeatFrom = repeatFrom,
            onRepeatFromChanged = { onRepeatFromChanged(it) },
            peekCustomRecurrence = { basicDialog.value = false },
        )
    } else {
        val state = CustomRecurrenceEditState.Companion
            .rememberCustomRecurrencePickerState(
                rrule = recurrence,
                dueDate = null,
                accountType = accountType,
                locale = Locale.getDefault()
            )

        CustomRecurrenceEdit(
            state = state.state.collectAsStateWithLifecycle().value,
            save = {
                onRecurrenceChanged(state.getRecur())
                dismiss()
            },
            discard = dismiss,
            setInterval = { state.setInterval(it) },
            setSelectedFrequency = { state.setFrequency(it) },
            setEndDate = { state.setEndDate(it) },
            setSelectedEndType = { state.setEndType(it) },
            setOccurrences = { state.setOccurrences(it) },
            toggleDay = { state.toggleDay(it) },
            setMonthSelection = { state.setMonthSelection(it) },
            calendarDisplayMode = preferences.calendarDisplayMode,
            setDisplayMode = { preferences.calendarDisplayMode = it }
        )
    }
}