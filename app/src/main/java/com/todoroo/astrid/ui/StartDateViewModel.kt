package com.todoroo.astrid.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.tasks.R
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.dialogs.StartDatePicker
import org.tasks.dialogs.StartDatePicker.Companion.DAY_BEFORE_DUE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_DATE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_TIME
import org.tasks.dialogs.StartDatePicker.Companion.WEEK_BEFORE_DUE
import org.tasks.preferences.Preferences
import org.tasks.time.millisOfDay
import org.tasks.time.startOfDay
import javax.inject.Inject

@HiltViewModel
class StartDateViewModel @Inject constructor(
    private val preferences: Preferences
) : ViewModel() {
    private val _selectedDay = MutableStateFlow(StartDatePicker.NO_DAY)
    val selectedDay: StateFlow<Long>
        get() = _selectedDay.asStateFlow()

    private val _selectedTime = MutableStateFlow(StartDatePicker.NO_TIME)
    val selectedTime: StateFlow<Int>
        get() = _selectedTime.asStateFlow()

    fun init(dueDate: Long, startDate: Long, isNew: Boolean) {
        val dueDay = dueDate.startOfDay()
        val dueTime = dueDate.millisOfDay
        val hideUntil = startDate.takeIf { it > 0 }?.toDateTime()
        if (hideUntil == null) {
            if (isNew) {
                _selectedDay.value = when (preferences.getIntegerFromString(R.string.p_default_hideUntil_key, Task.HIDE_UNTIL_NONE)) {
                    Task.HIDE_UNTIL_DUE -> DUE_DATE
                    Task.HIDE_UNTIL_DUE_TIME -> DUE_TIME
                    Task.HIDE_UNTIL_DAY_BEFORE -> DAY_BEFORE_DUE
                    Task.HIDE_UNTIL_WEEK_BEFORE -> WEEK_BEFORE_DUE
                    else -> 0L
                }
            }
        } else {
            _selectedDay.value = hideUntil.startOfDay().millis
            _selectedTime.value = hideUntil.millisOfDay
            _selectedDay.value = when (_selectedDay.value) {
                dueDay -> if (_selectedTime.value == dueTime) {
                    _selectedTime.value = StartDatePicker.NO_TIME
                    DUE_TIME
                } else {
                    DUE_DATE
                }
                dueDay.toDateTime().minusDays(1).millis ->
                    DAY_BEFORE_DUE
                dueDay.toDateTime().minusDays(7).millis ->
                    WEEK_BEFORE_DUE
                else -> _selectedDay.value
            }
        }
    }

    fun setSelected(selectedDay: Long, selectedTime: Int) {
        _selectedDay.value = selectedDay
        _selectedTime.value = selectedTime
    }

    fun getSelectedValue(dueDate: Long): Long {
        val due = dueDate.takeIf { it > 0 }?.toDateTime()
        return when (selectedDay.value) {
            DUE_DATE -> due?.withMillisOfDay(selectedTime.value)?.millis ?: 0
            DUE_TIME -> due?.millis ?: 0
            DAY_BEFORE_DUE -> due?.minusDays(1)?.withMillisOfDay(selectedTime.value)?.millis ?: 0
            WEEK_BEFORE_DUE -> due?.minusDays(7)?.withMillisOfDay(selectedTime.value)?.millis ?: 0
            else -> selectedDay.value + selectedTime.value
        }
    }
}