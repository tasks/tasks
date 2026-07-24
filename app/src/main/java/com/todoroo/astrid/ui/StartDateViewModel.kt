package com.todoroo.astrid.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.tasks.R
import org.tasks.data.entity.Task
import org.tasks.compose.pickers.NO_DAY
import org.tasks.compose.pickers.NO_TIME
import org.tasks.compose.pickers.defaultHideUntilDay
import org.tasks.compose.pickers.resolveStartDate
import org.tasks.compose.pickers.startDateSelection
import org.tasks.compose.pickers.startDayOf
import org.tasks.compose.pickers.toStartDay
import org.tasks.preferences.Preferences
import javax.inject.Inject

@HiltViewModel
class StartDateViewModel @Inject constructor(
    private val preferences: Preferences
) : ViewModel() {
    private val _selectedDay = MutableStateFlow(NO_DAY)
    val selectedDay: StateFlow<Long>
        get() = _selectedDay.asStateFlow()

    private val _selectedTime = MutableStateFlow(NO_TIME)
    val selectedTime: StateFlow<Int>
        get() = _selectedTime.asStateFlow()

    fun init(dueDate: Long, startDate: Long, isNew: Boolean) {
        if (startDate > 0) {
            val selection = startDateSelection(startDate, dueDate)
            _selectedDay.value = selection.day.toStartDay()
            _selectedTime.value = selection.time
        } else if (isNew) {
            _selectedDay.value = defaultHideUntilDay(
                preferences.getIntegerFromString(R.string.p_default_hideUntil_key, Task.HIDE_UNTIL_NONE)
            )
        }
    }

    fun setSelected(selectedDay: Long, selectedTime: Int) {
        _selectedDay.value = selectedDay
        _selectedTime.value = selectedTime
    }

    // The shared resolver normalizes; the caller's setStartDate normalizes again, but idempotently.
    fun getSelectedValue(dueDate: Long): Long =
        resolveStartDate(startDayOf(selectedDay.value), selectedTime.value, dueDate)
}