package org.tasks.dialogs

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.data.entity.Task
import org.tasks.databinding.DialogStartDatePickerBinding
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.dialogs.MyTimePickerDialog.Companion.newTimePicker
import org.tasks.notifications.NotificationManager
import org.tasks.time.DateTime
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class StartDatePicker : BaseDateTimePicker() {

    @Inject lateinit var activity: Activity
    @Inject lateinit var locale: Locale
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var notificationManager: NotificationManager

    lateinit var binding: DialogStartDatePickerBinding
    private var customDate = NO_DAY
    private var customTime = NO_TIME
    private var selectedDay = NO_DAY
    private var selectedTime = NO_TIME
    private val today = newDateTime().startOfDay()
    override val calendarView get() = binding.calendarView
    override val morningButton get() = binding.shortcuts.morningButton
    override val afternoonButton get() = binding.shortcuts.afternoonButton
    override val eveningButton get() = binding.shortcuts.eveningButton
    override val nightButton get() = binding.shortcuts.nightButton

    companion object {
        private const val REQUEST_TIME = 10101
        private const val FRAG_TAG_TIME_PICKER = "frag_tag_time_picker"
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogStartDatePickerBinding.inflate(theme.getLayoutInflater(requireContext()))
        setupShortcutsAndCalendar()
        binding.calendarView.setOnDateChangeListener { _, y, m, d ->
            returnDate(day = DateTime(y, m + 1, d).millis)
            refreshButtons()
        }
        selectedDay = savedInstanceState?.getLong(EXTRA_DAY) ?: requireArguments().getLong(EXTRA_DAY)
        selectedTime =
                savedInstanceState?.getInt(EXTRA_TIME)
                        ?: requireArguments().getInt(EXTRA_TIME)
                                .takeIf { Task.hasDueTime(it.toLong()) }
                                ?: NO_TIME
        with(binding.shortcuts) {
            noDateButton.setOnClickListener { clearDate() }
            noTime.setOnClickListener { clearTime() }
            dueDateButton.setOnClickListener { setToday() }
            dayBeforeDueButton.setOnClickListener { setTomorrow() }
            weekBeforeDueButton.setOnClickListener { setNextWeek() }
            morningButton.setOnClickListener { setMorning() }
            afternoonButton.setOnClickListener { setAfternoon() }
            eveningButton.setOnClickListener { setEvening() }
            nightButton.setOnClickListener { setNight() }
            dueTimeButton.setOnClickListener { setDueTime() }
            currentDateSelection.setOnClickListener { currentDate() }
            currentTimeSelection.setOnClickListener { currentTime() }
            pickTimeButton.setOnClickListener { pickTime() }
        }
        return binding.root
    }

    override fun refreshButtons() {
        when (selectedDay) {
            0L -> binding.shortcuts.dateGroup.check(R.id.no_date_button)
            DUE_DATE -> binding.shortcuts.dateGroup.check(R.id.due_date_button)
            DUE_TIME -> {
                binding.shortcuts.dateGroup.check(R.id.due_time_button)
                binding.shortcuts.timeGroup.clearChecked()
            }
            DAY_BEFORE_DUE -> binding.shortcuts.dateGroup.check(R.id.day_before_due_button)
            WEEK_BEFORE_DUE -> binding.shortcuts.dateGroup.check(R.id.week_before_due_button)
            else -> {
                customDate = selectedDay
                binding.shortcuts.dateGroup.check(R.id.current_date_selection)
                binding.shortcuts.currentDateSelection.visibility = View.VISIBLE
                binding.shortcuts.currentDateSelection.text =
                        DateUtilities.getRelativeDay(requireContext(), selectedDay, locale, FormatStyle.MEDIUM)
            }
        }
        if (Task.hasDueTime(selectedTime.toLong())) {
            when (selectedTime) {
                morning -> binding.shortcuts.timeGroup.check(R.id.morning_button)
                afternoon -> binding.shortcuts.timeGroup.check(R.id.afternoon_button)
                evening -> binding.shortcuts.timeGroup.check(R.id.evening_button)
                night -> binding.shortcuts.timeGroup.check(R.id.night_button)
                else -> {
                    customTime = selectedTime
                    binding.shortcuts.timeGroup.check(R.id.current_time_selection)
                    binding.shortcuts.currentTimeSelection.visibility = View.VISIBLE
                    binding.shortcuts.currentTimeSelection.text = DateUtilities.getTimeString(requireContext(), today.withMillisOfDay(selectedTime))
                }
            }
            if (selectedDay == DUE_TIME) {
                selectedDay = DUE_DATE
            }
        } else if (selectedDay != DUE_TIME) {
            binding.shortcuts.timeGroup.check(R.id.no_time)
        }
        if (selectedDay > 0) {
            binding.calendarView.setDate(selectedDay, true, true)
        }
    }

    private fun clearDate() = returnDate(day = 0, time = 0)
    private fun clearTime() = returnDate(
            day = when (selectedDay) {
                DUE_TIME -> DUE_DATE
                else -> selectedDay
            },
            time = 0
    )
    private fun setToday() = returnDate(day = DUE_DATE)
    private fun setTomorrow() = returnDate(day = DAY_BEFORE_DUE)
    private fun setNextWeek() = returnDate(day = WEEK_BEFORE_DUE)
    private fun setMorning() = returnSelectedTime(morning)
    private fun setAfternoon() = returnSelectedTime(afternoon)
    private fun setEvening() = returnSelectedTime(evening)
    private fun setNight() = returnSelectedTime(night)
    private fun setDueTime() = returnDate(day = DUE_TIME, time = NO_TIME)
    private fun currentDate() = returnDate(day = customDate)
    private fun currentTime() = returnSelectedTime(customTime)
    private fun pickTime() {
        val time = if (selectedTime < 0 || !Task.hasDueTime(today.withMillisOfDay(selectedTime).millis)) {
            today.noon().millisOfDay
        } else {
            selectedTime
        }
        newTimePicker(this, REQUEST_TIME, today.withMillisOfDay(time).millis)
                .show(parentFragmentManager, FRAG_TAG_TIME_PICKER)
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
        if (closeAutomatically()) {
            sendSelected()
        } else {
            refreshButtons()
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
