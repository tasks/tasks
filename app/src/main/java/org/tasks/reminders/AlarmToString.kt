package org.tasks.reminders

import android.content.Context
import com.todoroo.andlib.utility.DateUtilities
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.data.Alarm
import org.tasks.locale.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.absoluteValue

class AlarmToString @Inject constructor(
    @ApplicationContext context: Context,
    var locale: Locale,
) {
    private val resources = context.resources

    fun toString(alarm: Alarm): String {
        val reminder = when (alarm.type) {
            Alarm.TYPE_REL_START ->
                if (alarm.time == 0L) {
                    resources.getString(R.string.when_started)
                } else {
                    val res = if (alarm.time < 0) {
                        R.string.alarm_before_start
                    } else {
                        R.string.alarm_after_start
                    }
                    resources.getString(res, getDurationString(alarm.time))
                }
            Alarm.TYPE_REL_END ->
                if (alarm.time == 0L) {
                    resources.getString(R.string.when_due)
                } else {
                    val res = if (alarm.time < 0) {
                        R.string.alarm_before_due
                    } else {
                        R.string.alarm_after_due
                    }
                    resources.getString(res, getDurationString(alarm.time))
                }
            Alarm.TYPE_RANDOM ->
                resources.getString(R.string.randomly_once) + " "
            Alarm.TYPE_SNOOZE ->
                resources.getString(
                    R.string.snoozed_until,
                    DateUtilities.getLongDateStringWithTime(alarm.time, locale.locale)
                )
            else ->
                DateUtilities.getLongDateStringWithTime(alarm.time, locale.locale)
        }
        return if (alarm.repeat > 0) {
            val frequencyPlural = getDurationString(alarm.interval)
            val count = alarm.repeat
            val countString = resources.getQuantityString(R.plurals.repeat_times, count)
            reminder + "\n" + resources.getString(R.string.repeats_plural_number_of_times, frequencyPlural, count, countString)
        } else {
            reminder
        }
    }

    private fun getDurationString(duration: Long): String {
        val seconds = duration.absoluteValue
        val days = TimeUnit.MILLISECONDS.toDays(seconds)
        val weeks = days / 7
        val hours = TimeUnit.MILLISECONDS.toHours(seconds) - days * 24
        val minute =
            TimeUnit.MILLISECONDS.toMinutes(seconds) - TimeUnit.MILLISECONDS.toHours(seconds) * 60
        val result = ArrayList<String>()
        if (weeks > 0) {
            result.add(resources.getQuantityString(R.plurals.repeat_n_weeks, weeks.toInt(), weeks.toInt()))
        }
        val leftoverDays = days - weeks * 7
        if (leftoverDays > 0) {
            result.add(
                resources.getQuantityString(
                    R.plurals.repeat_n_days,
                    leftoverDays.toInt(),
                    leftoverDays.toInt()
                )
            )
        }
        if (hours > 0) {
            result.add(resources.getQuantityString(R.plurals.repeat_n_hours, hours.toInt(), hours.toInt()))
        }
        if (minute > 0) {
            result.add(resources.getQuantityString(R.plurals.repeat_n_minutes, minute.toInt(), minute.toInt()))
        }
        return result.joinToString(" ")
    }
}