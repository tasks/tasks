/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import butterknife.BindView
import butterknife.OnClick
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.activities.DateAndTimePickerActivity
import org.tasks.data.Alarm
import org.tasks.date.DateTimeUtils
import org.tasks.dialogs.DialogBuilder
import org.tasks.dialogs.MyTimePickerDialog
import org.tasks.injection.ActivityContext
import org.tasks.injection.FragmentComponent
import org.tasks.locale.Locale
import org.tasks.ui.TaskEditControlFragment
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Control set dealing with reminder settings
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
class ReminderControlSet : TaskEditControlFragment() {
    private val alarms: MutableSet<Long> = LinkedHashSet()

    @Inject @ActivityContext lateinit var activity: Context
    @Inject lateinit var alarmService: AlarmService
    @Inject lateinit var locale: Locale
    @Inject lateinit var dialogBuilder: DialogBuilder
    
    @BindView(R.id.alert_container)
    lateinit var alertContainer: LinearLayout

    @BindView(R.id.reminder_alarm)
    lateinit var mode: TextView
    
    private var taskId: Long = 0
    private var flags = 0
    private var randomReminder: Long = 0
    private var ringMode = 0
    private var randomControlSet: RandomReminderControlSet? = null
    private var whenDue = false
    private var whenOverdue = false
    
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        mode.paintFlags = mode.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        taskId = task.id
        if (savedInstanceState == null) {
            flags = task.reminderFlags
            randomReminder = task.reminderPeriod
            setup(currentAlarms())
        } else {
            flags = savedInstanceState.getInt(EXTRA_FLAGS)
            randomReminder = savedInstanceState.getLong(EXTRA_RANDOM_REMINDER)
            setup(savedInstanceState.getLongArray(EXTRA_ALARMS)!!.toList())
        }
        return view
    }

    private fun currentAlarms(): List<Long> {
        return if (taskId == Task.NO_ID) {
            emptyList()
        } else {
            alarmService.getAlarms(taskId).map(Alarm::time)
        }
    }

    @OnClick(R.id.reminder_alarm)
    fun onClickRingType() {
        val modes = resources.getStringArray(R.array.reminder_ring_modes)
        dialogBuilder
                .newDialog()
                .setSingleChoiceItems(modes, ringMode) { dialog: DialogInterface, which: Int ->
                    setRingMode(which)
                    dialog.dismiss()
                }
                .show()
    }

    private fun setRingMode(ringMode: Int) {
        this.ringMode = ringMode
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
            getString(R.string.when_due) -> addDue()
            getString(R.string.when_overdue) -> addOverdue()
            getString(R.string.randomly) -> addRandomReminder(TimeUnit.DAYS.toMillis(14))
            getString(R.string.pick_a_date_and_time) -> addNewAlarm()
        }
    }

    @OnClick(R.id.alarms_add)
    fun addAlarm() {
        val options = options
        if (options.size == 1) {
            addNewAlarm()
        } else {
            dialogBuilder
                    .newDialog()
                    .setItems(
                            options
                    ) { dialog: DialogInterface, which: Int ->
                        addAlarm(options[which])
                        dialog.dismiss()
                    }
                    .show()
        }
    }

    override val layout: Int
        get() = R.layout.control_set_reminders

    override val icon: Int
        get() = R.drawable.ic_outline_notifications_24px

    override fun controlId(): Int {
        return TAG
    }

    private fun setup(alarms: List<Long>) {
        setValue(flags)
        alertContainer.removeAllViews()
        if (whenDue) {
            addDue()
        }
        if (whenOverdue) {
            addOverdue()
        }
        if (randomReminder > 0) {
            addRandomReminder(randomReminder)
        }
        for (timestamp in alarms) {
            addAlarmRow(timestamp)
        }
    }

    override fun hasChanges(original: Task): Boolean {
        return getFlags() != original.reminderFlags || randomReminderPeriod != original.reminderPeriod || HashSet(currentAlarms()) != alarms
    }

    override fun requiresId() = true

    override fun apply(task: Task) {
        task.reminderFlags = getFlags()
        task.reminderPeriod = randomReminderPeriod
        if (alarmService.synchronizeAlarms(task.id, alarms)) {
            task.modificationDate = DateUtilities.now()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_FLAGS, getFlags())
        outState.putLong(EXTRA_RANDOM_REMINDER, randomReminderPeriod)
        outState.putLongArray(EXTRA_ALARMS, alarms.toLongArray())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_NEW_ALARM) {
            if (resultCode == Activity.RESULT_OK) {
                addAlarmRow(data!!.getLongExtra(MyTimePickerDialog.EXTRA_TIMESTAMP, 0L))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun addAlarmRow(timestamp: Long) {
        if (alarms.add(timestamp)) {
            addAlarmRow(DateUtilities.getLongDateStringWithTime(timestamp, locale.locale), View.OnClickListener { alarms.remove(timestamp) })
        }
    }

    private fun getFlags(): Int {
        var value = 0
        if (whenDue) {
            value = value or Task.NOTIFY_AT_DEADLINE
        }
        if (whenOverdue) {
            value = value or Task.NOTIFY_AFTER_DEADLINE
        }
        value = value and (Task.NOTIFY_MODE_FIVE or Task.NOTIFY_MODE_NONSTOP).inv()
        if (ringMode == 2) {
            value = value or Task.NOTIFY_MODE_NONSTOP
        } else if (ringMode == 1) {
            value = value or Task.NOTIFY_MODE_FIVE
        }
        return value
    }

    private val randomReminderPeriod: Long
        get() = if (randomControlSet == null) 0L else randomControlSet!!.reminderPeriod

    private fun addNewAlarm() {
        val intent = Intent(activity, DateAndTimePickerActivity::class.java)
        intent.putExtra(
                DateAndTimePickerActivity.EXTRA_TIMESTAMP, DateTimeUtils.newDateTime().noon().millis)
        startActivityForResult(intent, REQUEST_NEW_ALARM)
    }

    private fun addAlarmRow(text: String, onRemove: View.OnClickListener): View {
        val alertItem = requireActivity().layoutInflater.inflate(R.layout.alarm_edit_row, null)
        alertContainer.addView(alertItem)
        addAlarmRow(alertItem, text, onRemove)
        return alertItem
    }

    private fun addAlarmRow(alertItem: View, text: String, onRemove: View.OnClickListener?) {
        val display = alertItem.findViewById<TextView>(R.id.alarm_string)
        display.text = text
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
            if (!whenDue) {
                options.add(getString(R.string.when_due))
            }
            if (!whenOverdue) {
                options.add(getString(R.string.when_overdue))
            }
            if (randomControlSet == null) {
                options.add(getString(R.string.randomly))
            }
            options.add(getString(R.string.pick_a_date_and_time))
            return options
        }

    private fun addDue() {
        whenDue = true
        addAlarmRow(getString(R.string.when_due), View.OnClickListener { whenDue = false })
    }

    private fun addOverdue() {
        whenOverdue = true
        addAlarmRow(getString(R.string.when_overdue), View.OnClickListener { whenOverdue = false })
    }

    private fun addRandomReminder(reminderPeriod: Long) {
        val alarmRow = addAlarmRow(getString(R.string.randomly_once) + " ", View.OnClickListener { randomControlSet = null })
        randomControlSet = RandomReminderControlSet(activity, alarmRow, reminderPeriod)
    }

    private fun setValue(flags: Int) {
        whenDue = flags and Task.NOTIFY_AT_DEADLINE > 0
        whenOverdue = flags and Task.NOTIFY_AFTER_DEADLINE > 0
        when {
            flags and Task.NOTIFY_MODE_NONSTOP > 0 -> setRingMode(2)
            flags and Task.NOTIFY_MODE_FIVE > 0 -> setRingMode(1)
            else -> setRingMode(0)
        }
    }

    override fun inject(component: FragmentComponent) = component.inject(this)

    companion object {
        const val TAG = R.string.TEA_ctrl_reminders_pref
        private const val REQUEST_NEW_ALARM = 12152
        private const val EXTRA_FLAGS = "extra_flags"
        private const val EXTRA_RANDOM_REMINDER = "extra_random_reminder"
        private const val EXTRA_ALARMS = "extra_alarms"
    }
}