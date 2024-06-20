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
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_KEYBOARD
import com.google.android.material.timepicker.TimeFormat.CLOCK_12H
import com.google.android.material.timepicker.TimeFormat.CLOCK_24H
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay
import javax.inject.Inject

@AndroidEntryPoint
class MyTimePickerDialog : DialogFragment() {

    @Inject lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragment =
            (childFragmentManager.findFragmentByTag(FRAG_TAG_TIME_PICKER) as? MaterialTimePicker)
                ?: newTimePicker(requireContext(), initial, preferences.timeInputMode)
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
        get() = arguments?.getLong(EXTRA_TIMESTAMP) ?: currentTimeMillis().startOfDay()

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

        fun newTimePicker(
            target: Fragment,
            rc: Int,
            initial: Long,
        ) =
            MyTimePickerDialog().apply {
                arguments = Bundle().apply {
                    putLong(EXTRA_TIMESTAMP, initial)
                }
                setTargetFragment(target, rc)
            }

        @JvmStatic
        fun newTimePicker(context: Context?, initial: Long, inputMode: Int) =
            DateTime(initial).let {
                MaterialTimePicker.Builder()
                    .setInputMode(inputMode)
                    .setTimeFormat(if (is24HourFormat(context)) CLOCK_24H else CLOCK_12H)
                    .setHour(it.hourOfDay)
                    .setMinute(it.minuteOfHour)
                    .build()
            }

        val Preferences.timeInputMode: Int
            get() = when (getIntegerFromString(R.string.p_picker_mode_time, 0)) {
                1 -> INPUT_MODE_KEYBOARD
                else -> INPUT_MODE_CLOCK
            }
    }
}