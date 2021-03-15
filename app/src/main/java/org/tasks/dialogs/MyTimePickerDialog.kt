package org.tasks.dialogs

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat.CLOCK_12H
import com.google.android.material.timepicker.TimeFormat.CLOCK_24H
import com.todoroo.andlib.utility.DateUtilities.now
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils.startOfDay

class MyTimePickerDialog : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragment =
            (childFragmentManager.findFragmentByTag(FRAG_TAG_TIME_PICKER) as? MaterialTimePicker)
                ?: newTimePicker(requireContext(), initial)
                    .let {
                        childFragmentManager
                            .beginTransaction()
                            .add(it, FRAG_TAG_TIME_PICKER)
                            .commit()
                        it
                    }
        with(fragment) {
            addOnPositiveButtonClickListener { selected(hour, minute) }
            addOnNegativeButtonClickListener { cancel() }
            addOnCancelListener { cancel() }
        }
    }

    private val initial: Long
        get() = arguments?.getLong(EXTRA_TIMESTAMP) ?: now().startOfDay()

    private fun selected(hour: Int, minute: Int) {
        targetFragment?.onActivityResult(
            targetRequestCode,
            Activity.RESULT_OK,
            Intent().putExtra(
                EXTRA_TIMESTAMP,
                initial
                    .toDateTime()
                    .startOfDay()
                    .withHourOfDay(hour)
                    .withMinuteOfHour(minute)
                    .millis
            )
        )
        dismiss()
    }

    private fun cancel() {
        targetFragment?.onActivityResult(targetRequestCode, RESULT_CANCELED, null)
        dismiss()
    }

    companion object {
        const val FRAG_TAG_TIME_PICKER = "frag_time_picker"
        const val EXTRA_TIMESTAMP = "extra_timestamp"

        fun newTimePicker(target: Fragment, rc: Int, initial: Long) =
            MyTimePickerDialog().apply {
                arguments = Bundle().apply {
                    putLong(EXTRA_TIMESTAMP, initial)
                }
                setTargetFragment(target, rc)
            }

        @JvmStatic
        fun newTimePicker(context: Context?, initial: Long) = DateTime(initial).let {
            MaterialTimePicker.Builder()
                .setTimeFormat(if (is24HourFormat(context)) CLOCK_24H else CLOCK_12H)
                .setHour(it.hourOfDay)
                .setMinute(it.minuteOfHour)
                .build()
        }
    }
}