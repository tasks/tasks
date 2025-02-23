/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui

import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import com.todoroo.astrid.ui.NNumberPickerDialog.OnNNumberPickedListener
import org.tasks.R

class TimeDurationControlSet(
    private val context: Context,
    view: View,
    timeButtonId: Int,
) : OnNNumberPickedListener,
    View.OnClickListener {
    private val timeButton: TextView = view.findViewById(timeButtonId)
    var timeDurationInSeconds = 0
        private set
    private var initialValues: IntArray? = null
    private var dialog: NNumberPickerDialog? = null

    fun setTimeDuration(timeDurationInSeconds: Int) {
        this.timeDurationInSeconds = timeDurationInSeconds
        if (timeDurationInSeconds == 0) {
            timeButton.text = context.getString(R.string.WID_dateButtonUnset)
            return
        }
        timeButton.text = DateUtils.formatElapsedTime(timeDurationInSeconds.toLong())
        val hours = this.timeDurationInSeconds / 3600
        val minutes = this.timeDurationInSeconds / 60 - 60 * hours
        initialValues = intArrayOf(hours, minutes)
    }

    /** Called when NumberPicker activity is completed  */
    override fun onNumbersPicked(values: IntArray) {
        setTimeDuration(values[0] * 3600 + values[1] * 60)
    }

    /** Called when time button is clicked  */
    override fun onClick(v: View) {
        if (dialog == null) {
            dialog = NNumberPickerDialog(
                    context,
                    this,
                    context.getString(R.string.DLG_hour_minutes), intArrayOf(0, 0), intArrayOf(1, 5), intArrayOf(0, 0), intArrayOf(999, 59), arrayOf(":", null))
            val hourPicker = dialog!!.getPicker(0)
            val minutePicker = dialog!!.getPicker(1)
            minutePicker.setFormatter { value: Int -> String.format("%02d", value) }
            minutePicker.setOnChangeListener { newVal: Int ->
                if (newVal < 0) {
                    if (hourPicker.current == 0) {
                        return@setOnChangeListener 0
                    }
                    hourPicker.current = hourPicker.current - 1
                    return@setOnChangeListener 60 + newVal
                } else if (newVal > 59) {
                    hourPicker.current = hourPicker.current + 1
                    return@setOnChangeListener newVal % 60
                }
                newVal
            }
        }
        if (initialValues != null) {
            dialog!!.setInitialValues(initialValues)
        }
        dialog!!.show()
    }

    init {
        (timeButton.parent as View).setOnClickListener(this)
    }
}