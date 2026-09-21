package org.tasks.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.tasks.compose.pickers.DueDatePickerSheet
import org.tasks.compose.pickers.MULTIPLE_DAYS
import org.tasks.compose.pickers.MULTIPLE_TIMES
import org.tasks.compose.pickers.NO_DAY
import org.tasks.compose.pickers.NO_TIME
import org.tasks.data.TaskSaver
import org.tasks.data.createDueDate
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.themes.TasksTheme
import org.tasks.time.millisOfDay
import org.tasks.time.startOfDay
import javax.inject.Inject

@AndroidEntryPoint
class DateTimePicker : BaseDateTimePicker() {

    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var taskSaver: TaskSaver

    private val today = newDateTime().startOfDay()

    companion object {
        const val EXTRA_DAY = "extra_day"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_TASKS = "extra_tasks"
        const val EXTRA_HIDE_NO_DATE = "extra_hide_no_date"

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
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        TasksTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            val args = requireArguments()
            DueDatePickerSheet(
                initialDay = args.getLong(EXTRA_DAY),
                initialTime = args.getInt(EXTRA_TIME),
                is24Hour = requireContext().is24HourFormat,
                autoClose = autoclose,
                showNoDate = !args.getBoolean(EXTRA_HIDE_NO_DATE, false),
                times = preferences.quickPickTimes,
                initialDateInputMode = preferences.calendarDisplayMode == DisplayMode.Input,
                onDateInputModeChange = {
                    preferences.calendarDisplayMode = if (it) DisplayMode.Input else DisplayMode.Picker
                },
                initialTimeInputMode = preferences.timeDisplayMode == DisplayMode.Input,
                onTimeInputModeChange = {
                    preferences.timeDisplayMode = if (it) DisplayMode.Input else DisplayMode.Picker
                },
                onSelected = { day, time -> save(day, time) },
                onDismiss = { onDismissHandler?.onDismiss() ?: dismiss() },
            )
        }
    }

    private fun save(selectedDay: Long, selectedTime: Int) {
        val args = requireArguments()
        if (selectedDay != args.getLong(EXTRA_DAY) || selectedTime != args.getInt(EXTRA_TIME)) {
            val taskIds = args.getLongArray(EXTRA_TASKS) ?: longArrayOf()
            lifecycleScope.launch(NonCancellable) {
                taskDao
                    .fetch(taskIds.toList())
                    .forEach {
                        val original = it.copy()
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
                        taskSaver.save(it, original)
                    }
            }
        }
        dismiss()
    }
}
