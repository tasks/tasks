/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.todoroo.andlib.utility.DateUtilities
import org.tasks.R
import org.tasks.data.Alarm
import org.tasks.data.Alarm.Companion.TYPE_RANDOM
import org.tasks.ui.TaskEditViewModel

/**
 * Control set dealing with random reminder settings
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
internal class RandomReminderControlSet(context: Context, parentView: View, reminderPeriod: Long, vm: TaskEditViewModel) {
    init {
        val periodSpinner = parentView.findViewById<Spinner>(R.id.reminder_random_interval)
        periodSpinner.visibility = View.VISIBLE
        // create hour array
        val hourStrings = context.resources.getStringArray(R.array.TEA_reminder_random_hours)
        val hours = IntArray(hourStrings.size)
        for (i in hours.indices) {
            hours[i] = hourStrings[i].toInt()
        }
        // create adapter
        val adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                context.resources.getStringArray(R.array.TEA_reminder_random))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodSpinner.adapter = adapter
        periodSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newAlarm =
                    Alarm(vm.task?.id ?: 0, hours[position] * DateUtilities.ONE_HOUR, TYPE_RANDOM)
                vm.selectedAlarms?.apply {
                    find { it.type == TYPE_RANDOM }?.let {
                        newAlarm.id = it.id
                        remove(it)
                    }
                    add(newAlarm)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        var i = 0
        while (i < hours.size - 1) {
            if (hours[i] * DateUtilities.ONE_HOUR >= reminderPeriod) {
                break
            }
            i++
        }
        periodSpinner.setSelection(i)
    }
}