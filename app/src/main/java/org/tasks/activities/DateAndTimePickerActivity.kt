package org.tasks.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.dialogs.MyDatePickerDialog.Companion.dateInputMode
import org.tasks.dialogs.MyDatePickerDialog.Companion.newDatePicker
import org.tasks.dialogs.MyTimePickerDialog
import org.tasks.dialogs.MyTimePickerDialog.Companion.newTimePicker
import org.tasks.dialogs.MyTimePickerDialog.Companion.timeInputMode
import org.tasks.preferences.Preferences
import org.tasks.themes.ThemeAccent
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils.currentTimeMillis
import javax.inject.Inject

@AndroidEntryPoint
class DateAndTimePickerActivity : AppCompatActivity() {
    @Inject lateinit var themeAccent: ThemeAccent
    @Inject lateinit var preferences: Preferences

    private var initial: DateTime? = null
    private var dateSelected: DateTime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initial = DateTime(intent.getLongExtra(EXTRA_TIMESTAMP, currentTimeMillis()))
        dateSelected =
            savedInstanceState
                ?.getLong(EXTRA_DATE_SELECTED)
                ?.takeIf { it > 0 }
                ?.let { DateTime(it, DateTime.UTC) }
        themeAccent.applyStyle(theme)
        if (dateSelected != null) {
            showTimePicker()
        } else {
            showDatePicker(initial ?: DateTime())
        }
    }

    private fun showDatePicker(date: DateTime) {
        dateSelected = null
        val picker =
            supportFragmentManager
                .findFragmentByTag(FRAG_TAG_DATE_PICKER) as? MaterialDatePicker<Long>
                ?: newDatePicker(date.millis, preferences.dateInputMode).apply {
                    show(supportFragmentManager, FRAG_TAG_DATE_PICKER)
                }
        picker.apply {
            addOnPositiveButtonClickListener {
                dateSelected = DateTime(selection!!, DateTime.UTC)
                showTimePicker()
            }
            addOnCancelListener { finish() }
            addOnNegativeButtonClickListener { finish() }
        }
    }

    private fun showTimePicker() {
        val fragmentManager = supportFragmentManager
        val picker =
            fragmentManager
                .findFragmentByTag(FRAG_TAG_TIME_PICKER) as? MaterialTimePicker
                ?: newTimePicker(
                    this,
                    DateTime(dateSelected!!.year, dateSelected!!.monthOfYear, dateSelected!!.dayOfMonth)
                        .withMillisOfDay(initial!!.millisOfDay).millis,
                    preferences.timeInputMode
                ).apply { show(fragmentManager, FRAG_TAG_TIME_PICKER) }
        picker.apply {
            addOnCancelListener {
                dateSelected?.let {  showDatePicker(it) } ?: finish()
            }
            addOnNegativeButtonClickListener {
                dateSelected?.let { showDatePicker(it) } ?: finish()
            }
            addOnPositiveButtonClickListener {
                val data = Intent()
                data.putExtras(intent)
                data.putExtra(
                    MyTimePickerDialog.EXTRA_TIMESTAMP,
                    DateTime(
                        dateSelected!!.year,
                        dateSelected!!.monthOfYear,
                        dateSelected!!.dayOfMonth,
                        hour,
                        minute
                    ).millis
                )
                setResult(RESULT_OK, data)
                finish()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        dateSelected?.let { outState.putLong(EXTRA_DATE_SELECTED, it.millis) }
    }

    companion object {
        const val EXTRA_TIMESTAMP = "extra_timestamp"
        private const val FRAG_TAG_DATE_PICKER = "frag_tag_date_picker"
        private const val FRAG_TAG_TIME_PICKER = "frag_tag_time_picker"
        private const val EXTRA_DATE_SELECTED = "extra_date_selected"
    }
}