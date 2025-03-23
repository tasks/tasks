package org.tasks.dialogs

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.compose.pickers.DatePickerBottomSheet
import org.tasks.compose.pickers.StartDateShortcuts
import org.tasks.compose.pickers.TimePickerDialog
import org.tasks.compose.pickers.TimeShortcuts
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.notifications.NotificationManager
import org.tasks.themes.TasksTheme
import org.tasks.time.DateTime
import javax.inject.Inject

@AndroidEntryPoint
class StartDatePicker : BaseDateTimePicker() {

    @Inject lateinit var activity: Activity
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var notificationManager: NotificationManager

    private var selectedDay by mutableLongStateOf(NO_DAY)
    private var selectedTime by mutableIntStateOf(NO_TIME)
    private val today = newDateTime().startOfDay()

    companion object {
        const val EXTRA_DAY = "extra_day"
        const val EXTRA_TIME = "extra_time"
        const val NO_DAY = 0L
        const val NO_TIME = 0
        const val DUE_DATE = -1L
        const val DAY_BEFORE_DUE = -2L
        const val WEEK_BEFORE_DUE = -3L
        const val DUE_TIME = -4L

        fun newDateTimePicker(target: Fragment, rc: Int, day: Long, time: Int, autoClose: Boolean): StartDatePicker {
            val bundle = Bundle()
            bundle.putLong(EXTRA_DAY, day)
            bundle.putInt(EXTRA_TIME, time)
            bundle.putBoolean(EXTRA_AUTO_CLOSE, autoClose)
            val fragment = StartDatePicker()
            fragment.arguments = bundle
            fragment.setTargetFragment(target, rc)
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedDay = savedInstanceState?.getLong(EXTRA_DAY) ?: requireArguments().getLong(EXTRA_DAY)
        selectedTime =
            savedInstanceState?.getInt(EXTRA_TIME)
                ?: requireArguments().getInt(EXTRA_TIME)
                    .takeIf { Task.hasDueTime(it.toLong()) }
                        ?: NO_TIME
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        TasksTheme(theme = theme.themeBase.index) {
            val state = rememberDatePickerState(
                initialDisplayMode = remember { preferences.calendarDisplayMode },
            )
            DatePickerBottomSheet(
                sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true
                ),
                state = state,
                showButtons = !autoclose,
                setDisplayMode = { preferences.calendarDisplayMode = it },
                dismiss = { onDismissHandler?.onDismiss() ?: dismiss() },
                accept = { sendSelected() },
                dateShortcuts = {
                    StartDateShortcuts(
                        selected = selectedDay,
                        selectedDay = { returnDate(it) },
                        selectedDayTime = { day, time -> returnDate(day, time) },
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
                                is24Hour = requireContext().is24HourFormat
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
                        },
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
    }

    private fun returnSelectedTime(millisOfDay: Int) {
        val day = when {
            selectedDay == DUE_TIME -> DUE_DATE
            selectedDay != 0L -> selectedDay
            today.withMillisOfDay(millisOfDay).isAfterNow -> today.millis
            else -> today.plusDays(1).millis
        }
        returnDate(day = day, time = millisOfDay)
    }

    private fun returnDate(day: Long = selectedDay, time: Int = selectedTime) {
        selectedDay = day
        selectedTime = time
        if (autoclose) {
            sendSelected()
        }
    }

    override fun sendSelected() {
        if (selectedDay != arguments?.getLong(EXTRA_DAY)
                || selectedTime != arguments?.getInt(EXTRA_TIME)) {
            val intent = Intent().apply {
                putExtra(EXTRA_DAY, selectedDay)
                putExtra(EXTRA_TIME, selectedTime)
            }
            targetFragment?.onActivityResult(targetRequestCode, RESULT_OK, intent)
        }
        dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putLong(EXTRA_DAY, selectedDay)
        outState.putInt(EXTRA_TIME, selectedTime)
    }
}
