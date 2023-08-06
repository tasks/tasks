package org.tasks.reminders

import android.content.Context
import android.content.res.Resources
import com.todoroo.andlib.utility.DateUtilities
import org.tasks.R
import org.tasks.data.Alarm
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class AlarmToString(context: Context, var locale: Locale) {
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
                    resources.getString(res, resources.getDurationString(alarm.time))
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
                    resources.getString(res, resources.getDurationString(alarm.time))
                }
            Alarm.TYPE_RANDOM ->
                resources.getString(R.string.randomly_every, resources.getDurationString(alarm.time))
            Alarm.TYPE_SNOOZE ->
                resources.getString(
                    R.string.snoozed_until,
                    DateUtilities.getLongDateStringWithTime(alarm.time, locale)
                )
            else ->
                DateUtilities.getLongDateStringWithTime(alarm.time, locale)
        }
        return if (alarm.repeat > 0) {
            reminder + "\n" + resources.getRepeatString(alarm.repeat, alarm.interval)
        } else {
            reminder
        }
    }

    companion object {
        fun Resources.getRepeatString(repeat: Int, interval: Long): String =
            getString(
                R.string.repeats_plural_number_of_times,
                getDurationString(interval),
                repeat,
                getQuantityString(R.plurals.repeat_times, repeat)
            )

        fun Resources.getDurationString(duration: Long): String {
            val seconds = duration.absoluteValue
            val days = TimeUnit.MILLISECONDS.toDays(seconds)
            val weeks = days / 7
            val hours = TimeUnit.MILLISECONDS.toHours(seconds) - days * 24
            val minute =
                TimeUnit.MILLISECONDS.toMinutes(seconds) - TimeUnit.MILLISECONDS.toHours(seconds) * 60
            val result = ArrayList<String>()
            if (weeks > 0) {
                result.add(getQuantityString(R.plurals.repeat_n_weeks, weeks.toInt(), weeks.toInt()))
            }
            val leftoverDays = days - weeks * 7
            if (leftoverDays > 0) {
                result.add(
                    getQuantityString(
                        R.plurals.repeat_n_days,
                        leftoverDays.toInt(),
                        leftoverDays.toInt()
                    )
                )
            }
            if (hours > 0) {
                result.add(getQuantityString(R.plurals.repeat_n_hours, hours.toInt(), hours.toInt()))
            }
            if (minute > 0) {
                result.add(getQuantityString(R.plurals.repeat_n_minutes, minute.toInt(), minute.toInt()))
            }
            return result.joinToString(" ")
        }
    }
}