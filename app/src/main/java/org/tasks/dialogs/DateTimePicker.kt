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
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.databinding.DialogDateTimePickerBinding
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.dialogs.MyTimePickerDialog.newTimePicker
import org.tasks.injection.DialogFragmentComponent
import org.tasks.injection.InjectingBottomSheetDialogFragment
import org.tasks.locale.Locale
import org.tasks.notifications.NotificationManager
import org.tasks.preferences.Preferences
import org.tasks.themes.Theme
import org.tasks.time.DateTime
import java.time.format.FormatStyle
import javax.inject.Inject

class DateTimePicker : InjectingBottomSheetDialogFragment() {

    @Inject lateinit var activity: Activity
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var locale: Locale
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var theme: Theme

    lateinit var binding: DialogDateTimePickerBinding
    private var customDate: DateTime? = null
    private var selected: DateTime? = null
    private val today = newDateTime().startOfDay()
    private val tomorrow = today.plusDays(1)
    private val nextWeek = today.plusDays(7)
    private var customTime = 0
    private var morning = 32401000
    private var afternoon = 46801000
    private var evening = 61201000
    private var night = 72001000
    private var onDismissHandler: OnDismissHandler? = null

    interface OnDismissHandler {
        fun onDismiss()
    }

    companion object {
        const val EXTRA_TIMESTAMP = "extra_timestamp"
        const val EXTRA_TASK = "extra_task"
        private const val EXTRA_AUTO_CLOSE = "extra_auto_close"
        private const val EXTRA_SELECTED = "extra_selected"
        private const val REQUEST_TIME = 10101
        private const val FRAG_TAG_TIME_PICKER = "frag_tag_time_picker"

        fun newDateTimePicker(task: Long, current: Long, autoClose: Boolean): DateTimePicker {
            val bundle = Bundle()
            bundle.putLong(EXTRA_TASK, task)
            bundle.putLong(EXTRA_TIMESTAMP, current)
            bundle.putBoolean(EXTRA_AUTO_CLOSE, autoClose)
            val fragment = DateTimePicker()
            fragment.arguments = bundle
            return fragment
        }

        fun newDateTimePicker(target: Fragment, rc: Int, current: Long, autoClose: Boolean): DateTimePicker {
            val bundle = Bundle()
            bundle.putLong(EXTRA_TIMESTAMP, current)
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
            selected = DateTime(y, m + 1, d, selected?.hourOfDay ?: 0, selected?.minuteOfHour
                    ?: 0, selected?.secondOfMinute ?: 0)
            returnDate(selected!!.millis)
            refreshButtons()
        }
        val firstDayOfWeek = preferences.firstDayOfWeek
        if (firstDayOfWeek in 1..7) {
            binding.calendarView.firstDayOfWeek = firstDayOfWeek
        }
        val timestamp = savedInstanceState?.getLong(EXTRA_SELECTED)
                ?: requireArguments().getLong(EXTRA_TIMESTAMP)
        selected = if (timestamp > 0) DateTime(timestamp) else null

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
        when (selected?.startOfDay()) {
            null -> binding.shortcuts.dateGroup.check(R.id.no_date_button)
            today -> binding.shortcuts.dateGroup.check(R.id.today_button)
            tomorrow -> binding.shortcuts.dateGroup.check(R.id.tomorrow_button)
            nextWeek -> binding.shortcuts.dateGroup.check(R.id.next_week_button)
            else -> {
                customDate = selected
                binding.shortcuts.dateGroup.check(R.id.current_date_selection)
                binding.shortcuts.currentDateSelection.visibility = View.VISIBLE
                binding.shortcuts.currentDateSelection.text =
                        DateUtilities.getRelativeDay(context, selected!!.millis, locale.locale, FormatStyle.MEDIUM)
            }
        }
        if (Task.hasDueTime(selected?.millis ?: 0)) {
            when (selected?.millisOfDay) {
                morning -> binding.shortcuts.timeGroup.check(R.id.morning_button)
                afternoon -> binding.shortcuts.timeGroup.check(R.id.afternoon_button)
                evening -> binding.shortcuts.timeGroup.check(R.id.evening_button)
                night -> binding.shortcuts.timeGroup.check(R.id.night_button)
                else -> {
                    customTime = selected!!.millisOfDay
                    binding.shortcuts.timeGroup.check(R.id.current_time_selection)
                    binding.shortcuts.currentTimeSelection.visibility = View.VISIBLE
                    binding.shortcuts.currentTimeSelection.text = DateUtilities.getTimeString(context, selected)
                }
            }
        } else {
            binding.shortcuts.timeGroup.check(R.id.no_time)
        }
        if (selected != null) {
            binding.calendarView.setDate(selected!!.millis, true, true)
        }
    }

    @OnClick(R.id.no_date_button)
    fun clearDate() = returnDate(0)

    @OnClick(R.id.no_time)
    fun clearTime() = returnDate(selected?.startOfDay()?.millis ?: 0)

    @OnClick(R.id.today_button)
    fun setToday() = returnDate(today.withMillisOfDay(selected?.millisOfDay ?: 0))

    @OnClick(R.id.tomorrow_button)
    fun setTomorrow() = returnDate(tomorrow.withMillisOfDay(selected?.millisOfDay ?: 0))

    @OnClick(R.id.next_week_button)
    fun setNextWeek() = returnDate(nextWeek.withMillisOfDay(selected?.millisOfDay ?: 0))

    @OnClick(R.id.morning_button)
    fun setMorning() = returnSelectedTime(morning)

    @OnClick(R.id.afternoon_button)
    fun setAfternoon() = returnSelectedTime(afternoon)

    @OnClick(R.id.evening_button)
    fun setEvening() = returnSelectedTime(evening)

    @OnClick(R.id.night_button)
    fun setNight() = returnSelectedTime(night)

    @OnClick(R.id.current_date_selection)
    fun currentDate() = returnDate(customDate)

    @OnClick(R.id.current_time_selection)
    fun currentTime() = returnSelectedTime(customTime)

    @OnClick(R.id.pick_time_button)
    fun pickTime() {
        newTimePicker(this, REQUEST_TIME, selected?.millis ?: today.noon().millis)
                .show(parentFragmentManager, FRAG_TAG_TIME_PICKER)
    }

    private fun returnSelectedTime(millisOfDay: Int) {
        if (selected == null) {
            selected = today.withMillisOfDay(millisOfDay)
            if (selected!!.isBeforeNow) {
                selected = selected!!.plusDays(1)
            }
        } else {
            selected = selected!!.withMillisOfDay(millisOfDay)
        }
        returnDate(selected!!.millis)
    }

    private fun returnDate(dt: DateTime? = selected) = returnDate(dt?.millis ?: 0)

    private fun returnDate(date: Long? = selected?.millis) {
        selected = if (date == null || date <= 0) null else DateTime(date)
        if (closeAutomatically()) {
            sendSelected()
        } else {
            refreshButtons()
        }
    }

    private fun sendSelected() {
        val taskId = arguments?.getLong(EXTRA_TASK) ?: 0
        val dueDate = selected?.millis ?: 0
        if (dueDate != arguments?.getLong(EXTRA_TIMESTAMP)) {
            if (taskId > 0) {
                val task: Task = taskDao.fetch(taskId)!!
                if (newDateTime(dueDate).isAfterNow) {
                    notificationManager.cancel(task.id)
                }
                task.setDueDateAdjustingHideUntil(dueDate)
                taskDao.save(task)
            } else {
                val intent = Intent()
                intent.putExtra(EXTRA_TIMESTAMP, dueDate)
                targetFragment?.onActivityResult(targetRequestCode, RESULT_OK, intent)
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

        outState.putSerializable(EXTRA_SELECTED, selected?.millis)
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

    override fun inject(component: DialogFragmentComponent) = component.inject(this)
}
