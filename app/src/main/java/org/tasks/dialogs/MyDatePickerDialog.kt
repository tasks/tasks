package org.tasks.dialogs

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialDatePicker.INPUT_MODE_CALENDAR
import com.google.android.material.datepicker.MaterialDatePicker.INPUT_MODE_TEXT
import com.todoroo.andlib.utility.DateUtilities
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils.currentTimeMillis
import org.tasks.time.DateTimeUtils.startOfDay
import javax.inject.Inject

@AndroidEntryPoint
class MyDatePickerDialog : DialogFragment() {

    @Inject lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragment =
            (childFragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER) as? MaterialDatePicker<Long>)
                ?: newDatePicker(initial, preferences.dateInputMode)
                    .let {
                        childFragmentManager
                            .beginTransaction()
                            .add(it, FRAG_TAG_DATE_PICKER)
                            .commit()
                        it
                    }
        with(fragment) {
            addOnPositiveButtonClickListener {
                val dt = DateTime(it, DateTime.UTC)
                selected(dt.year, dt.monthOfYear, dt.dayOfMonth)
            }
            addOnCancelListener { cancel() }
            addOnNegativeButtonClickListener { cancel() }
        }
    }

    private val initial: Long
        get() = arguments?.getLong(MyTimePickerDialog.EXTRA_TIMESTAMP) ?: DateUtilities.now().startOfDay()

    private fun selected(year: Int, month: Int, day: Int) {
        targetFragment?.onActivityResult(
            targetRequestCode,
            RESULT_OK,
            Intent().putExtra(EXTRA_TIMESTAMP, DateTime(year, month, day).millis)
        )
        dismiss()
    }

    private fun cancel() {
        targetFragment?.onActivityResult(targetRequestCode, RESULT_CANCELED, null)
        dismiss()
    }

    companion object {
        const val FRAG_TAG_DATE_PICKER = "frag_date_picker"
        const val EXTRA_TIMESTAMP = "extra_timestamp"

        @JvmStatic
        fun newDatePicker(initial: Long, inputMode: Int) =
            MaterialDatePicker.Builder.datePicker()
                // TODO: figure out hack for first day of week
                .setInputMode(inputMode)
                .setSelection(if (initial > 0) initial else currentTimeMillis())
                .build()

        val Preferences.dateInputMode: Int
            get() = when (getIntegerFromString(R.string.p_picker_mode_date, 0)) {
                1 -> INPUT_MODE_TEXT
                else -> INPUT_MODE_CALENDAR
            }
    }
}