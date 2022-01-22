/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.activities.DateAndTimePickerActivity
import org.tasks.data.Alarm
import org.tasks.data.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.Alarm.Companion.TYPE_RANDOM
import org.tasks.data.Alarm.Companion.TYPE_REL_END
import org.tasks.data.Alarm.Companion.TYPE_REL_START
import org.tasks.data.Alarm.Companion.whenDue
import org.tasks.data.Alarm.Companion.whenOverdue
import org.tasks.data.Alarm.Companion.whenStarted
import org.tasks.databinding.ControlSetRemindersBinding
import org.tasks.date.DateTimeUtils
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.MyTimePickerDialog
import org.tasks.reminders.AlarmToString
import org.tasks.ui.TaskEditControlFragment
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Control set dealing with reminder settings
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
@AndroidEntryPoint
class ReminderControlSet : TaskEditControlFragment() {
    @Inject lateinit var activity: Activity
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var alarmToString: AlarmToString

    private lateinit var alertContainer: LinearLayout
    private lateinit var mode: TextView
    
    private var randomControlSet: RandomReminderControlSet? = null

    override fun createView(savedInstanceState: Bundle?) {
        mode.paintFlags = mode.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        when {
            viewModel.ringNonstop!! -> setRingMode(2)
            viewModel.ringFiveTimes!! -> setRingMode(1)
            else -> setRingMode(0)
        }
        if (viewModel.reminderPeriod!! > 0) {
            addRandomReminder(viewModel.reminderPeriod!!)
        }
        viewModel.selectedAlarms?.forEach(this::addAlarmRow)
    }

    private fun onClickRingType() {
        val modes = resources.getStringArray(R.array.reminder_ring_modes)
        val ringMode = when {
            viewModel.ringNonstop == true -> 2
            viewModel.ringFiveTimes == true -> 1
            else -> 0
        }
        dialogBuilder
                .newDialog()
                .setSingleChoiceItems(modes, ringMode) { dialog: DialogInterface, which: Int ->
                    setRingMode(which)
                    dialog.dismiss()
                }
                .show()
    }

    private fun setRingMode(ringMode: Int) {
        viewModel.ringNonstop = ringMode == 2
        viewModel.ringFiveTimes = ringMode == 1
        mode.setText(getRingModeString(ringMode))
    }

    @StringRes
    private fun getRingModeString(ringMode: Int): Int {
        return when (ringMode) {
            2 -> R.string.ring_nonstop
            1 -> R.string.ring_five_times
            else -> R.string.ring_once
        }
    }

    private fun addAlarm(selected: String) {
        when (selected) {
            getString(R.string.when_started) ->
                addAlarmRow(whenStarted(viewModel.task?.id ?: 0))
            getString(R.string.when_due) ->
                addAlarmRow(whenDue(viewModel.task?.id ?: 0))
            getString(R.string.when_overdue) ->
                addAlarmRow(whenOverdue(viewModel.task?.id ?: 0))
            getString(R.string.randomly) ->
                addRandomReminder(TimeUnit.DAYS.toMillis(14))
            getString(R.string.pick_a_date_and_time) ->
                addNewAlarm()
        }
    }

    private fun addAlarm() {
        val options = options
        if (options.size == 1) {
            addNewAlarm()
        } else {
            dialogBuilder
                    .newDialog()
                    .setItems(options) { dialog: DialogInterface, which: Int ->
                        addAlarm(options[which])
                        dialog.dismiss()
                    }
                    .show()
        }
    }

    override fun bind(parent: ViewGroup?) =
        ControlSetRemindersBinding.inflate(layoutInflater, parent, true).let {
            alertContainer = it.alertContainer
            mode = it.reminderAlarm.apply {
                setOnClickListener { onClickRingType() }
            }
            it.alarmsAdd.setOnClickListener { addAlarm() }
            it.root
        }

    override val icon = R.drawable.ic_outline_notifications_24px

    override fun controlId() = TAG

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_NEW_ALARM) {
            if (resultCode == Activity.RESULT_OK) {
                val timestamp = data!!.getLongExtra(MyTimePickerDialog.EXTRA_TIMESTAMP, 0L)
                if (viewModel.selectedAlarms?.any { it.type == TYPE_DATE_TIME && timestamp == it.time } == false) {
                    val alarm = Alarm(viewModel.task?.id ?: 0, timestamp)
                    viewModel.selectedAlarms?.add(alarm)
                    addAlarmRow(alarm)
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun addAlarmRow(alarm: Alarm) {
        addAlarmRow(alarm) {
            viewModel.selectedAlarms?.removeIf {
                it.type == alarm.type &&
                        it.time == alarm.time &&
                        it.repeat == alarm.repeat &&
                        it.interval == alarm.interval
            }
        }
    }

    private fun addNewAlarm() {
        val intent = Intent(activity, DateAndTimePickerActivity::class.java)
        intent.putExtra(
                DateAndTimePickerActivity.EXTRA_TIMESTAMP, DateTimeUtils.newDateTime().noon().millis)
        startActivityForResult(intent, REQUEST_NEW_ALARM)
    }

    private fun addAlarmRow(alarm: Alarm, onRemove: View.OnClickListener): View {
        val alertItem = requireActivity().layoutInflater.inflate(R.layout.alarm_edit_row, null)
        alertContainer.addView(alertItem)
        addAlarmRow(alertItem, alarm, onRemove)
        return alertItem
    }

    private fun addAlarmRow(alertItem: View, alarm: Alarm, onRemove: View.OnClickListener?) {
        val display = alertItem.findViewById<TextView>(R.id.alarm_string)
        viewModel.selectedAlarms?.add(alarm)
        display.text = alarmToString.toString(alarm)
        alertItem
                .findViewById<View>(R.id.clear)
                .setOnClickListener { v: View? ->
                    alertContainer.removeView(alertItem)
                    onRemove?.onClick(v)
                }
    }

    private val options: List<String>
        get() {
            val options: MutableList<String> = ArrayList()
            if (viewModel.selectedAlarms?.find { it.type == TYPE_REL_START && it.time == 0L } == null) {
                options.add(getString(R.string.when_started))
            }
            if (viewModel.selectedAlarms?.find { it.type == TYPE_REL_END && it.time == 0L } == null) {
                options.add(getString(R.string.when_due))
            }
            if (viewModel.selectedAlarms?.find { it.type == TYPE_REL_END && it.time == TimeUnit.HOURS.toMillis(24) } == null) {
                options.add(getString(R.string.when_overdue))
            }
            if (randomControlSet == null) {
                options.add(getString(R.string.randomly))
            }
            options.add(getString(R.string.pick_a_date_and_time))
            return options
        }

    private fun addRandomReminder(reminderPeriod: Long) {
        val alarmRow = addAlarmRow(Alarm(viewModel.task?.id ?: 0, 0, TYPE_RANDOM)) {
            viewModel.reminderPeriod = 0
            randomControlSet = null
        }
        randomControlSet = RandomReminderControlSet(activity, alarmRow, reminderPeriod, viewModel)
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_reminders_pref
        private const val REQUEST_NEW_ALARM = 12152
    }
}