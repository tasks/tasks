package org.tasks.dialogs

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
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
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.tasks.compose.pickers.DatePickerBottomSheet
import org.tasks.compose.pickers.DueDateShortcuts
import org.tasks.compose.pickers.TimePickerDialog
import org.tasks.compose.pickers.TimeShortcuts
import org.tasks.data.createDueDate
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.dialogs.MyTimePickerDialog.Companion.timeInputMode
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.notifications.NotificationManager
import org.tasks.themes.TasksTheme
import org.tasks.time.DateTime
import org.tasks.time.millisOfDay
import org.tasks.time.startOfDay
import javax.inject.Inject

@AndroidEntryPoint
class DateTimePicker : BaseDateTimePicker() {

    @Inject lateinit var activity: Activity
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var notificationManager: NotificationManager

    private var selectedDay by mutableLongStateOf(NO_DAY)
    private var selectedTime by mutableIntStateOf(NO_TIME)
    private val today = newDateTime().startOfDay()

    companion object {
        const val EXTRA_DAY = "extra_day"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_TASKS = "extra_tasks"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
        const val EXTRA_HIDE_NO_DATE = "extra_hide_no_date"
        const val NO_DAY = 0L
        const val NO_TIME = 0
        const val MULTIPLE_DAYS = -1L
        const val MULTIPLE_TIMES = -1

        fun newDateTimePicker(
            autoClose: Boolean,
            vararg tasks: Task,
        ): DateTimePicker {
            val fragment = DateTimePicker()
            val dueDates = tasks.map { it.dueDate.startOfDay() }.toSet()
            val dueTimes = tasks.map { it.dueDate.millisOfDay }.toSet()
            fragment.arguments = Bundle().apply {
                putLongArray(EXTRA_TASKS, tasks.map { it.id }.toLongArray())
                putLong(EXTRA_DAY, if (dueDates.size == 1) dueDates.first() else MULTIPLE_DAYS)
                putInt(EXTRA_TIME, if (dueTimes.size == 1) dueTimes.first() else MULTIPLE_TIMES)
                putBoolean(EXTRA_HIDE_NO_DATE, tasks.any { it.isRecurring })
                putBoolean(EXTRA_AUTO_CLOSE, autoClose)
            }
            return fragment
        }

        fun newDateTimePicker(
            target: Fragment,
            rc: Int,
            current: Long,
            autoClose: Boolean,
            hideNoDate: Boolean,
        ): DateTimePicker {
            val fragment = DateTimePicker()
            fragment.arguments = Bundle().apply {
                putLong(EXTRA_DAY, current.startOfDay())
                putInt(EXTRA_TIME, current.millisOfDay)
                putBoolean(EXTRA_AUTO_CLOSE, autoClose)
                putBoolean(EXTRA_HIDE_NO_DATE, hideNoDate)
            }
            fragment.setTargetFragment(target, rc)
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedDay =
            savedInstanceState?.getLong(EXTRA_DAY) ?: requireArguments().getLong(EXTRA_DAY)
        selectedTime =
            savedInstanceState?.getInt(EXTRA_TIME)
                ?: requireArguments().getInt(EXTRA_TIME)
                    .takeIf { it == MULTIPLE_TIMES || Task.hasDueTime(it.toLong()) }
                        ?: NO_TIME
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        TasksTheme(theme = theme.themeBase.index) {
            val state = rememberDatePickerState()
            DatePickerBottomSheet(
                state = state,
                showButtons = !autoclose,
                dismiss = { onDismissHandler?.onDismiss() ?: dismiss() },
                accept = { sendSelected() },
                dateShortcuts = {
                    DueDateShortcuts(
                        today = today.millis,
                        tomorrow = remember { today.plusDays(1).millis },
                        nextWeek = remember { today.plusDays(7).millis },
                        selected = selectedDay,
                        showNoDate = remember {
                            !requireArguments().getBoolean(
                                EXTRA_HIDE_NO_DATE,
                                false
                            )
                        },
                        selectedDay = { returnDate(it.startOfDay()) },
                        clearDate = { returnDate(day = 0, time = 0) },
                    )
                },
                timeShortcuts = {
                    var showTimePicker by rememberSaveable {
                        mutableStateOf(
                            false
                        )
                    }
                    if (showTimePicker) {
                        val time = if (selectedTime == MULTIPLE_TIMES
                            || !Task.hasDueTime(
                                today.withMillisOfDay(
                                    selectedTime
                                ).millis
                            )
                        ) {
                            today.noon().millisOfDay
                        } else {
                            selectedTime
                        }
                        TimePickerDialog(
                            millisOfDay = time,
                            is24Hour = remember { requireContext().is24HourFormat },
                            textInput = remember { preferences.timeInputMode == 1 },
                            selected = { returnSelectedTime(it + 1000) },
                            dismiss = { showTimePicker = false },
                        )
                    }
                    TimeShortcuts(
                        day = 0,
                        selected = selectedTime,
                        morning = remember { preferences.dateShortcutMorning + 1000 },
                        afternoon = remember { preferences.dateShortcutAfternoon + 1000 },
                        evening = remember { preferences.dateShortcutEvening + 1000 },
                        night = remember { preferences.dateShortcutNight + 1000 },
                        selectedMillisOfDay = { returnSelectedTime(it) },
                        pickTime = { showTimePicker = true },
                        clearTime = { returnDate(time = 0) },
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
            selectedDay == MULTIPLE_DAYS -> MULTIPLE_DAYS
            selectedDay > 0 -> selectedDay
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

    private val taskIds: LongArray
        get() = arguments?.getLongArray(EXTRA_TASKS) ?: longArrayOf()

    override fun sendSelected() {
        if (selectedDay != arguments?.getLong(EXTRA_DAY)
            || selectedTime != arguments?.getInt(EXTRA_TIME)
        ) {
            if (taskIds.isEmpty()) {
                val intent = Intent()
                intent.putExtra(
                    EXTRA_TIMESTAMP, when {
                        selectedDay == NO_DAY -> 0
                        selectedTime == NO_TIME -> selectedDay
                        else -> selectedDay.toDateTime().withMillisOfDay(selectedTime).millis
                    }
                )
                targetFragment?.onActivityResult(targetRequestCode, RESULT_OK, intent)
            } else {
                lifecycleScope.launch(NonCancellable) {
                    taskDao
                        .fetch(taskIds.toList())
                        .forEach {
                            val day = if (selectedDay == MULTIPLE_DAYS) {
                                if (it.hasDueDate()) it.dueDate else today.millis
                            } else {
                                selectedDay
                            }
                            val time = if (selectedTime == MULTIPLE_TIMES) {
                                if (it.hasDueTime()) it.dueDate.millisOfDay else NO_TIME
                            } else {
                                selectedTime
                            }
                            it.setDueDateAdjustingHideUntil(
                                when {
                                    day == NO_DAY -> 0L
                                    time == NO_TIME -> createDueDate(
                                        Task.URGENCY_SPECIFIC_DAY,
                                        day
                                    )

                                    else -> createDueDate(
                                        Task.URGENCY_SPECIFIC_DAY_TIME,
                                        day.toDateTime().withMillisOfDay(time).millis
                                    )
                                }
                            )
                            taskDao.save(it)
                        }
                }
            }
        }
        dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putLong(EXTRA_DAY, selectedDay)
        outState.putInt(EXTRA_TIME, selectedTime)
    }
}
