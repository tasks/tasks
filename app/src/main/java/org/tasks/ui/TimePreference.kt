package org.tasks.ui

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.preference.Preference
import org.tasks.R
import org.tasks.dialogs.MyTimePickerDialog
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.kmp.org.tasks.time.getTimeString
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.withMillisOfDay

class TimePreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    private val summaryTemplate: String?
    var millisOfDay: Int = 0
        private set

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TimePreference)
        summaryTemplate = a.getString(R.styleable.TimePreference_time_summary)
        a.recycle()
    }

    override fun onGetDefaultValue(a: android.content.res.TypedArray, index: Int): Any {
        return a.getInteger(index, -1)
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        millisOfDay = if (restoreValue) {
            val noon = DateTime().startOfDay().withHourOfDay(12).millisOfDay
            getPersistedInt(noon)
        } else {
            defaultValue as Int
        }
        updateMillisOfDay(millisOfDay)
    }

    private fun updateMillisOfDay(millis: Int) {
        millisOfDay = millis
        val setting = getTimeString(
            currentTimeMillis().withMillisOfDay(millis),
            context.is24HourFormat
        )
        summary = if (summaryTemplate == null) setting else String.format(summaryTemplate, setting)
    }

    fun handleTimePickerActivityIntent(data: Intent) {
        val timestamp = data.getLongExtra(MyTimePickerDialog.EXTRA_TIMESTAMP, 0L)
        val millis = DateTime(timestamp).millisOfDay
        if (callChangeListener(millis)) {
            persistInt(millis)
            updateMillisOfDay(millis)
        }
    }
}
