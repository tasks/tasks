package com.todoroo.astrid.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.data.Task
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.databinding.ControlSetHideBinding
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.dialogs.StartDatePicker
import org.tasks.dialogs.StartDatePicker.Companion.DAY_BEFORE_DUE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_DATE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_TIME
import org.tasks.dialogs.StartDatePicker.Companion.EXTRA_DAY
import org.tasks.dialogs.StartDatePicker.Companion.EXTRA_TIME
import org.tasks.dialogs.StartDatePicker.Companion.NO_DAY
import org.tasks.dialogs.StartDatePicker.Companion.NO_TIME
import org.tasks.dialogs.StartDatePicker.Companion.WEEK_BEFORE_DUE
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils.millisOfDay
import org.tasks.time.DateTimeUtils.startOfDay
import org.tasks.ui.TaskEditControlFragment
import java.time.format.FormatStyle
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class StartDateControlSet : TaskEditControlFragment() {
    @Inject lateinit var activity: Activity
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var locale: Locale

    private lateinit var startDate: TextView

    private val dueDateTime
        get() = viewModel.dueDate!!

    private var selectedDay = NO_DAY
    private var selectedTime = NO_TIME

    override fun onRowClick() {
        val fragmentManager = parentFragmentManager
        if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER) == null) {
            StartDatePicker.newDateTimePicker(
                    this,
                    REQUEST_HIDE_UNTIL,
                    selectedDay,
                    selectedTime,
                    preferences.getBoolean(R.string.p_auto_dismiss_datetime_edit_screen, false))
                    .show(fragmentManager, FRAG_TAG_DATE_PICKER)
        }
    }

    override fun createView(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            val dueDay = dueDateTime.startOfDay()
            val dueTime = dueDateTime.millisOfDay()
            val hideUntil = viewModel.hideUntil?.takeIf { it > 0 }?.toDateTime()
            if (hideUntil == null) {
                if (viewModel.isNew) {
                    when (preferences.getIntegerFromString(R.string.p_default_hideUntil_key, Task.HIDE_UNTIL_NONE)) {
                        Task.HIDE_UNTIL_DUE -> selectedDay = DUE_DATE
                        Task.HIDE_UNTIL_DUE_TIME -> selectedDay = DUE_TIME
                        Task.HIDE_UNTIL_DAY_BEFORE -> selectedDay = DAY_BEFORE_DUE
                        Task.HIDE_UNTIL_WEEK_BEFORE -> selectedDay = WEEK_BEFORE_DUE
                    }
                }
            } else {
                selectedDay = hideUntil.startOfDay().millis
                selectedTime = hideUntil.millisOfDay
                selectedDay = when (selectedDay) {
                    dueDay -> if (selectedTime == dueTime) {
                        selectedTime = NO_TIME
                        DUE_TIME
                    } else {
                        DUE_DATE
                    }
                    dueDay.toDateTime().minusDays(1).millis ->
                        DAY_BEFORE_DUE
                    dueDay.toDateTime().minusDays(7).millis ->
                        WEEK_BEFORE_DUE
                    else -> selectedDay
                }
            }
        } else {
            selectedDay = savedInstanceState.getLong(EXTRA_DAY)
            selectedTime = savedInstanceState.getInt(EXTRA_TIME)
        }
        applySelectionToHideUntil()
    }

    override fun bind(parent: ViewGroup?) =
        ControlSetHideBinding.inflate(layoutInflater, parent, true).let {
            startDate = it.startDate
            it.root
        }

    override val icon = R.drawable.ic_pending_actions_24px

    override fun controlId() = TAG

    override val isClickable = true

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_HIDE_UNTIL) {
            if (resultCode == Activity.RESULT_OK) {
                selectedDay = data?.getLongExtra(EXTRA_DAY, 0L) ?: NO_DAY
                selectedTime = data?.getIntExtra(EXTRA_TIME, 0) ?: NO_TIME
                applySelectionToHideUntil()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(EXTRA_DAY, selectedDay)
        outState.putInt(EXTRA_TIME, selectedTime)
    }

    private fun getRelativeDateString(resId: Int) = if (selectedTime == NO_TIME) {
        getString(resId)
    } else {
        "${getString(resId)} ${DateUtilities.getTimeString(context, newDateTime().withMillisOfDay(selectedTime))}"
    }

    private fun refreshDisplayView() {
        startDate.text = when (selectedDay) {
            DUE_DATE -> getRelativeDateString(R.string.due_date)
            DUE_TIME -> getString(R.string.due_time)
            DAY_BEFORE_DUE -> getRelativeDateString(R.string.day_before_due)
            WEEK_BEFORE_DUE -> getRelativeDateString(R.string.week_before_due)
            in 1..Long.MAX_VALUE -> DateUtilities.getRelativeDateTime(
                    activity,
                    selectedDay + selectedTime,
                    locale,
                    FormatStyle.FULL,
                    preferences.alwaysDisplayFullDate,
                    false
            )
            else -> null
        }
        val started = viewModel.hideUntil?.takeIf { it > 0 }?.let { it < now() } ?: false
        startDate.setTextColor(
                activity.getColor(if (started) R.color.overdue else R.color.text_primary)
        )
    }

    fun onDueDateChanged() = applySelectionToHideUntil()

    private fun applySelectionToHideUntil() {
        val due = dueDateTime.takeIf { it > 0 }?.toDateTime()
        val millisOfDay = selectedTime
        viewModel.hideUntil = when (selectedDay) {
            DUE_DATE -> due?.withMillisOfDay(millisOfDay)?.millis ?: 0
            DUE_TIME -> due?.millis ?: 0
            DAY_BEFORE_DUE -> due?.minusDays(1)?.withMillisOfDay(millisOfDay)?.millis ?: 0
            WEEK_BEFORE_DUE -> due?.minusDays(7)?.withMillisOfDay(millisOfDay)?.millis ?: 0
            else -> selectedDay + selectedTime
        }
        refreshDisplayView()
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_hide_until_pref
        private const val REQUEST_HIDE_UNTIL = 11011
        private const val FRAG_TAG_DATE_PICKER = "frag_tag_date_picker"
    }
}