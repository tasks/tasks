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

/**
 * Control set dealing with random reminder settings
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
internal class RandomReminderControlSet(context: Context, parentView: View, reminderPeriod: Long) {
    private val hours: IntArray
    private var selectedIndex = 0
    val reminderPeriod: Long
        get() {
            val hourValue = hours[selectedIndex]
            return hourValue * DateUtilities.ONE_HOUR
        }

    init {
        val periodSpinner = parentView.findViewById<Spinner>(R.id.reminder_random_interval)
        periodSpinner.visibility = View.VISIBLE
        // create adapter
        val adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                context.resources.getStringArray(R.array.TEA_reminder_random))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodSpinner.adapter = adapter
        periodSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                selectedIndex = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // create hour array
        val hourStrings = context.resources.getStringArray(R.array.TEA_reminder_random_hours)
        hours = IntArray(hourStrings.size)
        for (i in hours.indices) {
            hours[i] = hourStrings[i].toInt()
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