package org.tasks.dialogs

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.databinding.DialogDateTimePickerBinding
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.dialogs.MyTimePickerDialog.newTimePicker
import org.tasks.locale.Locale
import org.tasks.notifications.NotificationManager
import org.tasks.preferences.Preferences
import org.tasks.themes.Theme
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils.millisOfDay
import org.tasks.time.DateTimeUtils.startOfDay
import java.time.format.FormatStyle
import javax.inject.Inject

@AndroidEntryPoint
class DateTimePicker : BottomSheetDialogFragment() {

    @Inject lateinit var activity: Activity
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var locale: Locale
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var theme: Theme

    lateinit var binding: DialogDateTimePickerBinding
    private var customDate = NO_DAY
    private var customTime = NO_TIME
    private var selectedDay = NO_DAY
    private var selectedTime = NO_TIME
    private val today = newDateTime().startOfDay()
    private val tomorrow = today.plusDays(1)
    private val nextWeek = today.plusDays(7)
    private var morning = 32401000
    private var afternoon = 46801000
    private var evening = 61201000
    private var night = 72001000
    private var onDismissHandler: OnDismissHandler? = null

    interface OnDismissHandler {
        fun onDismiss()
    }

    companion object {
        const val EXTRA_DAY = "extra_day"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_TASKS = "extra_tasks"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
        private const val EXTRA_AUTO_CLOSE = "extra_auto_close"
        private const val REQUEST_TIME = 10101
        private const val FRAG_TAG_TIME_PICKER = "frag_tag_time_picker"
        private const val NO_DAY = 0L
        private const val NO_TIME = 0
        private const val MULTIPLE_DAYS = -1L
        private const val MULTIPLE_TIMES = -1

        fun newDateTimePicker(autoClose: Boolean, vararg tasks: Task): DateTimePicker {
            val bundle = Bundle()
            bundle.putLongArray(EXTRA_TASKS, tasks.map { it.id }.toLongArray())
            val dueDates = tasks.map { it.dueDate.startOfDay() }.toSet()
            val dueTimes = tasks.map { it.dueDate.millisOfDay() }.toSet()
            bundle.putLong(EXTRA_DAY, if (dueDates.size == 1) dueDates.first() else MULTIPLE_DAYS)
            bundle.putInt(EXTRA_TIME, if (dueTimes.size == 1) dueTimes.first() else MULTIPLE_TIMES)
            bundle.putBoolean(EXTRA_AUTO_CLOSE, autoClose)
            val fragment = DateTimePicker()
            fragment.arguments = bundle
            return fragment
        }

        fun newDateTimePicker(target: Fragment, rc: Int, current: Long, autoClose: Boolean): DateTimePicker {
            val bundle = Bundle()
            bundle.putLong(EXTRA_DAY, current.startOfDay())
            bundle.putInt(EXTRA_TIME, current.millisOfDay())
            bundle.putBoolean(EXTRA_AUTO_CLOSE, autoClose)
            val fragment = DateTimePicker()
            fragment.arguments = bundle
            fragment.setTargetFragment(target, rc)
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogDateTimePickerBinding.inflate(theme.getLayoutInflater(context))
        morning = preferences.dateShortcutMorning + 1000
        afternoon = preferences.dateShortcutAfternoon + 1000
        evening = preferences.dateShortcutEvening + 1000
        night = preferences.dateShortcutNight + 1000
        binding.shortcuts.morningButton.text = DateUtilities.getTimeString(context, newDateTime().withMillisOfDay(morning))
        binding.shortcuts.afternoonButton.text = DateUtilities.getTimeString(context, newDateTime().withMillisOfDay(afternoon))
        binding.shortcuts.eveningButton.text = DateUtilities.getTimeString(context, newDateTime().withMillisOfDay(evening))
        binding.shortcuts.nightButton.text = DateUtilities.getTimeString(context, newDateTime().withMillisOfDay(night))
        ButterKnife.bind(this, binding.root)
        binding.shortcuts.nextWeekButton.text =
                getString(R.string.next, DateUtilities.getWeekdayShort(newDateTime().plusWeeks(1), locale.locale))
        binding.calendarView.setOnDateChangeListener { _, y, m, d ->
            returnDate(day = DateTime(y, m + 1, d).millis)
            refreshButtons()
        }
        val firstDayOfWeek = preferences.firstDayOfWeek
        if (firstDayOfWeek in 1..7) {
            binding.calendarView.firstDayOfWeek = firstDayOfWeek
        }
        selectedDay = savedInstanceState?.getLong(EXTRA_DAY) ?: requireArguments().getLong(EXTRA_DAY)
        selectedTime =
                savedInstanceState?.getInt(EXTRA_TIME)
                        ?: requireArguments().getInt(EXTRA_TIME)
                                .takeIf { it == MULTIPLE_TIMES || Task.hasDueTime(it.toLong()) }
                                ?: NO_TIME

        return binding.root
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)

        if (activity is OnDismissHandler) {
            onDismissHandler = activity
        }
    }

    private fun closeAutomatically(): Boolean = arguments?.getBoolean(EXTRA_AUTO_CLOSE) ?: false

    override fun onResume() {
        super.onResume()

        refreshButtons()
    }

    private fun refreshButtons() {
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
                    DateUtilities.getRelativeDay(context, selectedDay, locale.locale, FormatStyle.MEDIUM)
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
                        DateUtilities.getTimeString(context, today.withMillisOfDay(selectedTime))
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

    @OnClick(R.id.no_date_button)
    fun clearDate() = returnDate(day = 0, time = 0)

    @OnClick(R.id.no_time)
    fun clearTime() = returnDate(time = 0)

    @OnClick(R.id.today_button)
    fun setToday() = returnDate(day = today.startOfDay().millis)

    @OnClick(R.id.tomorrow_button)
    fun setTomorrow() = returnDate(day = tomorrow.startOfDay().millis)

    @OnClick(R.id.next_week_button)
    fun setNextWeek() = returnDate(day = nextWeek.startOfDay().millis)

    @OnClick(R.id.morning_button)
    fun setMorning() = returnSelectedTime(morning)

    @OnClick(R.id.afternoon_button)
    fun setAfternoon() = returnSelectedTime(afternoon)

    @OnClick(R.id.evening_button)
    fun setEvening() = returnSelectedTime(evening)

    @OnClick(R.id.night_button)
    fun setNight() = returnSelectedTime(night)

    @OnClick(R.id.current_date_selection)
    fun currentDate() = returnDate(day = customDate)

    @OnClick(R.id.current_time_selection)
    fun currentTime() = returnSelectedTime(customTime)

    @OnClick(R.id.pick_time_button)
    fun pickTime() {
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

    private fun sendSelected() {
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
                                    if (it.hasDueTime()) it.dueDate.millisOfDay() else NO_TIME
                                } else {
                                    selectedTime
                                }
                                it.setDueDateAdjustingHideUntil(when {
                                    day == NO_DAY -> 0L
                                    time == NO_TIME -> Task.createDueDate(
                                            Task.URGENCY_SPECIFIC_DAY,
                                            day
                                    )
                                    else -> Task.createDueDate(
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

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        onDismissHandler?.onDismiss()
    }

    override fun onCancel(dialog: DialogInterface) = sendSelected()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putLong(EXTRA_DAY, selectedDay)
        outState.putInt(EXTRA_TIME, selectedTime)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                dialog.behavior.halfExpandedRatio = .75f
                val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                BottomSheetBehavior.from(bottomSheet!!).state = BottomSheetBehavior.STATE_HALF_EXPANDED
                dialog.behavior.peekHeight = bottomSheet.height
            }

            if (!closeAutomatically()) {
                addButtons(dialog)
            }
        }
        return dialog
    }

    private fun addButtons(dialog: BottomSheetDialog) {
        val coordinator = dialog
                .findViewById<CoordinatorLayout>(com.google.android.material.R.id.coordinator)
        val containerLayout =
                dialog.findViewById<FrameLayout>(com.google.android.material.R.id.container)
        val buttons = theme.getLayoutInflater(context).inflate(R.layout.dialog_date_time_picker_buttons, null)
        buttons.findViewById<View>(R.id.cancel_button).setOnClickListener { dismiss() }
        buttons.findViewById<View>(R.id.ok_button).setOnClickListener { sendSelected() }
        buttons.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
        ).apply {
            gravity = Gravity.BOTTOM
        }
        containerLayout!!.addView(buttons)

        buttons.post {
            (coordinator!!.layoutParams as ViewGroup.MarginLayoutParams).apply {
                buttons.measure(
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                this.bottomMargin = buttons.measuredHeight
                containerLayout.requestLayout()
            }
        }
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
