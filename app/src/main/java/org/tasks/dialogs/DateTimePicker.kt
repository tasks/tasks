package org.tasks.dialogs

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.data.createDueDate
import org.tasks.data.entity.Task
import org.tasks.databinding.DialogDateTimePickerBinding
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.dialogs.MyTimePickerDialog.Companion.newTimePicker
import org.tasks.notifications.NotificationManager
import org.tasks.time.DateTime
import org.tasks.time.millisOfDay
import org.tasks.time.startOfDay
import java.time.format.FormatStyle
import java.util.Calendar.FRIDAY
import java.util.Calendar.MONDAY
import java.util.Calendar.SATURDAY
import java.util.Calendar.SUNDAY
import java.util.Calendar.THURSDAY
import java.util.Calendar.TUESDAY
import java.util.Calendar.WEDNESDAY
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class DateTimePicker : BaseDateTimePicker() {

    @Inject lateinit var activity: Activity
    @Inject lateinit var locale: Locale
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var notificationManager: NotificationManager

    lateinit var binding: DialogDateTimePickerBinding
    private var customDate = NO_DAY
    private var customTime = NO_TIME
    private var selectedDay = NO_DAY
    private var selectedTime = NO_TIME
    private val today = newDateTime().startOfDay()
    private val tomorrow = today.plusDays(1)
    private val nextWeek = today.plusDays(7)
    override val calendarView get() = binding.calendarView
    override val morningButton get() = binding.shortcuts.morningButton
    override val afternoonButton get() = binding.shortcuts.afternoonButton
    override val eveningButton get() = binding.shortcuts.eveningButton
    override val nightButton get() = binding.shortcuts.nightButton

    companion object {
        const val EXTRA_DAY = "extra_day"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_TASKS = "extra_tasks"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
        const val EXTRA_HIDE_NO_DATE = "extra_hide_no_date"
        private const val REQUEST_TIME = 10101
        private const val FRAG_TAG_TIME_PICKER = "frag_tag_time_picker"
        private const val NO_DAY = 0L
        private const val NO_TIME = 0
        private const val MULTIPLE_DAYS = -1L
        private const val MULTIPLE_TIMES = -1

        fun newDateTimePicker(
                autoClose: Boolean,
                vararg tasks: Task
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

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = DialogDateTimePickerBinding.inflate(theme.getLayoutInflater(requireContext()))
        setupShortcutsAndCalendar()
        with (binding.shortcuts) {
            nextWeekButton.text =
                getString(
                    when (newDateTime().plusWeeks(1).dayOfWeek) {
                        SUNDAY -> R.string.next_sunday
                        MONDAY -> R.string.next_monday
                        TUESDAY -> R.string.next_tuesday
                        WEDNESDAY -> R.string.next_wednesday
                        THURSDAY -> R.string.next_thursday
                        FRIDAY -> R.string.next_friday
                        SATURDAY -> R.string.next_saturday
                        else -> throw IllegalArgumentException()
                    }
                )
            noDateButton.isGone = requireArguments().getBoolean(EXTRA_HIDE_NO_DATE, false)
            noDateButton.setOnClickListener { clearDate() }
            noTime.setOnClickListener { clearTime() }
            todayButton.setOnClickListener { setToday() }
            tomorrowButton.setOnClickListener { setTomorrow() }
            nextWeekButton.setOnClickListener { setNextWeek() }
            morningButton.setOnClickListener { setMorning() }
            afternoonButton.setOnClickListener { setAfternoon() }
            eveningButton.setOnClickListener { setEvening() }
            nightButton.setOnClickListener { setNight() }
            currentDateSelection.setOnClickListener { currentDate() }
            currentTimeSelection.setOnClickListener { currentTime() }
            pickTimeButton.setOnClickListener { pickTime() }
        }
        binding.calendarView.setOnDateChangeListener { _, y, m, d ->
            returnDate(day = DateTime(y, m + 1, d).millis)
            refreshButtons()
        }
        selectedDay = savedInstanceState?.getLong(EXTRA_DAY) ?: requireArguments().getLong(EXTRA_DAY)
        selectedTime =
                savedInstanceState?.getInt(EXTRA_TIME)
                        ?: requireArguments().getInt(EXTRA_TIME)
                                .takeIf { it == MULTIPLE_TIMES || Task.hasDueTime(it.toLong()) }
                                ?: NO_TIME

        return binding.root
    }

    override fun refreshButtons() {
        when (selectedDay) {
            0L -> binding.shortcuts.dateGroup.check(R.id.no_date_button)
            today.millis -> binding.shortcuts.dateGroup.check(R.id.today_button)
            tomorrow.millis -> binding.shortcuts.dateGroup.check(R.id.tomorrow_button)
            nextWeek.millis -> binding.shortcuts.dateGroup.check(R.id.next_week_button)
            else -> {
                customDate = selectedDay
                binding.shortcuts.dateGroup.check(R.id.current_date_selection)
                binding.shortcuts.currentDateSelection.visibility = View.VISIBLE
                binding.shortcuts.currentDateSelection.text = if (customDate == MULTIPLE_DAYS) {
                    requireContext().getString(R.string.date_picker_multiple)
                } else {
                    DateUtilities.getRelativeDay(requireContext(), selectedDay, locale, FormatStyle.MEDIUM)
                }
            }
        }
        if (selectedTime == MULTIPLE_TIMES || Task.hasDueTime(selectedTime.toLong())) {
            when (selectedTime) {
                morning -> binding.shortcuts.timeGroup.check(R.id.morning_button)
                afternoon -> binding.shortcuts.timeGroup.check(R.id.afternoon_button)
                evening -> binding.shortcuts.timeGroup.check(R.id.evening_button)
                night -> binding.shortcuts.timeGroup.check(R.id.night_button)
                else -> {
                    customTime = selectedTime
                    binding.shortcuts.timeGroup.check(R.id.current_time_selection)
                    binding.shortcuts.currentTimeSelection.visibility = View.VISIBLE
                    binding.shortcuts.currentTimeSelection.text = if (customTime == MULTIPLE_TIMES) {
                        requireContext().getString(R.string.date_picker_multiple)
                    } else {
                        DateUtilities.getTimeString(requireContext(), today.withMillisOfDay(selectedTime))
                    }
                }
            }
        } else {
            binding.shortcuts.timeGroup.check(R.id.no_time)
        }
        if (selectedDay > 0) {
            binding.calendarView.setDate(selectedDay, true, true)
        }
    }

    private fun clearDate() = returnDate(day = 0, time = 0)
    private fun clearTime() = returnDate(time = 0)
    private fun setToday() = returnDate(day = today.startOfDay().millis)
    private fun setTomorrow() = returnDate(day = tomorrow.startOfDay().millis)
    private fun setNextWeek() = returnDate(day = nextWeek.startOfDay().millis)
    private fun setMorning() = returnSelectedTime(morning)
    private fun setAfternoon() = returnSelectedTime(afternoon)
    private fun setEvening() = returnSelectedTime(evening)
    private fun setNight() = returnSelectedTime(night)
    private fun currentDate() = returnDate(day = customDate)
    private fun currentTime() = returnSelectedTime(customTime)

    private fun pickTime() {
        val time = if (selectedTime == MULTIPLE_TIMES
                || !Task.hasDueTime(today.withMillisOfDay(selectedTime).millis)) {
            today.noon().millisOfDay
        } else {
            selectedTime
        }
        newTimePicker(this, REQUEST_TIME, today.withMillisOfDay(time).millis)
                .show(parentFragmentManager, FRAG_TAG_TIME_PICKER)
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
        if (closeAutomatically()) {
            sendSelected()
        } else {
            refreshButtons()
        }
    }

    private val taskIds: LongArray
        get() = arguments?.getLongArray(EXTRA_TASKS) ?: longArrayOf()

    override fun sendSelected() {
        if (selectedDay != arguments?.getLong(EXTRA_DAY)
                || selectedTime != arguments?.getInt(EXTRA_TIME)) {
            if (taskIds.isEmpty()) {
                val intent = Intent()
                intent.putExtra(EXTRA_TIMESTAMP, when {
                    selectedDay == NO_DAY -> 0
                    selectedTime == NO_TIME -> selectedDay
                    else -> selectedDay.toDateTime().withMillisOfDay(selectedTime).millis
                })
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
                                it.setDueDateAdjustingHideUntil(when {
                                    day == NO_DAY -> 0L
                                    time == NO_TIME -> createDueDate(
                                            Task.URGENCY_SPECIFIC_DAY,
                                            day
                                    )
                                    else -> createDueDate(
                                            Task.URGENCY_SPECIFIC_DAY_TIME,
                                            day.toDateTime().withMillisOfDay(time).millis
                                    )
                                })
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TIME) {
            if (resultCode == RESULT_OK) {
                val timestamp = data!!.getLongExtra(MyTimePickerDialog.EXTRA_TIMESTAMP, today.millis)
                returnSelectedTime(newDateTime(timestamp).millisOfDay + 1000)
            } else {
                refreshButtons()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
