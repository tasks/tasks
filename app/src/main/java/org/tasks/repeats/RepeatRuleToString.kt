package org.tasks.repeats

import android.content.Context
import com.todoroo.andlib.utility.DateUtilities
import dagger.hilt.android.qualifiers.ApplicationContext
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.Recur.Frequency
import net.fortuna.ical4j.model.Recur.Frequency.*
import net.fortuna.ical4j.model.WeekDay.Day
import net.fortuna.ical4j.model.WeekDay.Day.*
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTime
import java.text.DateFormatSymbols
import java.util.*
import javax.inject.Inject

class RepeatRuleToString @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val locale: Locale,
        private val firebase: Firebase
) {
    private val weekdays = listOf(*Day.values())

    fun toString(rrule: String?): String? = rrule?.let { toString(newRecur(it)) }

    private fun toString(rrule: Recur): String = try {
        val interval = rrule.interval
        val frequency = rrule.frequency
        val repeatUntil = if (rrule.until == null) null else DateTime.from(rrule.until)
        val count = rrule.count
        val countString = if (count > 0) context.resources.getQuantityString(R.plurals.repeat_times, count) else ""
        if (interval <= 1) {
            val frequencyString = context.getString(getSingleFrequencyResource(frequency))
            if ((frequency == WEEKLY || frequency == MONTHLY) && !rrule.dayList.isEmpty()) {
                val dayString = getDayString(rrule)
                when {
                    count > 0 -> context.getString(
                            R.string.repeats_single_on_number_of_times,
                            frequencyString,
                            dayString,
                            count,
                            countString
                    )
                    repeatUntil == null ->
                        context.getString(R.string.repeats_single_on, frequencyString, dayString)
                    else -> context.getString(
                            R.string.repeats_single_on_until,
                            frequencyString,
                            dayString,
                            DateUtilities.getLongDateString(repeatUntil, locale)
                    )
                }
            } else if (count > 0) {
                context.getString(
                        R.string.repeats_single_number_of_times, frequencyString, count, countString)
            } else if (repeatUntil == null) {
                context.getString(R.string.repeats_single, frequencyString)
            } else {
                context.getString(
                        R.string.repeats_single_until,
                        frequencyString,
                        DateUtilities.getLongDateString(repeatUntil, locale))
            }
        } else {
            val plural = getFrequencyPlural(frequency)
            val frequencyPlural = context.resources.getQuantityString(plural, interval, interval)
            if ((frequency == WEEKLY || frequency == MONTHLY) && !rrule.dayList.isEmpty()) {
                val dayString = getDayString(rrule)
                when {
                    count > 0 -> context.getString(
                            R.string.repeats_plural_on_number_of_times,
                            frequencyPlural,
                            dayString,
                            count,
                            countString
                    )
                    repeatUntil == null ->
                        context.getString(R.string.repeats_plural_on, frequencyPlural, dayString)
                    else -> context.getString(
                            R.string.repeats_plural_on_until,
                            frequencyPlural,
                            dayString,
                            DateUtilities.getLongDateString(repeatUntil, locale)
                    )
                }
            } else if (count > 0) {
                context.getString(
                        R.string.repeats_plural_number_of_times, frequencyPlural, count, countString)
            } else if (repeatUntil == null) {
                context.getString(R.string.repeats_plural, frequencyPlural)
            } else {
                context.getString(
                        R.string.repeats_plural_until,
                        frequencyPlural,
                        DateUtilities.getLongDateString(repeatUntil, locale))
            }
        }
    } catch (e: Exception) {
        firebase.reportException(IllegalStateException("$rrule caused $e"))
        rrule.toString()
    }

    private fun getDayString(rrule: Recur): String {
        val dfs = DateFormatSymbols(locale)
        return if (rrule.frequency == WEEKLY) {
            val shortWeekdays = dfs.shortWeekdays
            val days: MutableList<String?> = ArrayList()
            for (weekday in rrule.dayList) {
                days.add(shortWeekdays[weekdays.indexOf(weekday.day) + 1])
            }
            days.joinToString(context.getString(R.string.list_separator_with_space))
        } else if (rrule.frequency == MONTHLY) {
            val longWeekdays = dfs.weekdays
            val weekdayNum = rrule.dayList[0]
            val weekday: String
            val dayOfWeekCalendar = Calendar.getInstance(locale)
            dayOfWeekCalendar[Calendar.DAY_OF_WEEK] = weekdayToCalendarDay(weekdayNum.day)
            weekday = longWeekdays[dayOfWeekCalendar[Calendar.DAY_OF_WEEK]]
            if (weekdayNum.offset == -1) {
                context.getString(
                        R.string.repeat_monthly_every_day_of_nth_week,
                        context.getString(R.string.repeat_monthly_last_week),
                        weekday)
            } else {
                context.getString(
                        R.string.repeat_monthly_every_day_of_nth_week,
                        context.getString(NTH_WEEK[weekdayNum.offset - 1]),
                        weekday)
            }
        } else {
            throw RuntimeException()
        }
    }

    private fun weekdayToCalendarDay(weekday: Day): Int {
        return when (weekday) {
            SU -> Calendar.SUNDAY
            MO -> Calendar.MONDAY
            TU -> Calendar.TUESDAY
            WE -> Calendar.WEDNESDAY
            TH -> Calendar.THURSDAY
            FR -> Calendar.FRIDAY
            SA -> Calendar.SATURDAY
            else -> throw RuntimeException("Invalid weekday: $weekday")
        }
    }

    private fun getSingleFrequencyResource(frequency: Frequency): Int {
        return when (frequency) {
            MINUTELY -> R.string.repeats_minutely
            HOURLY -> R.string.repeats_hourly
            DAILY -> R.string.repeats_daily
            WEEKLY -> R.string.repeats_weekly
            MONTHLY -> R.string.repeats_monthly
            YEARLY -> R.string.repeats_yearly
            else -> throw RuntimeException("Invalid frequency: $frequency")
        }
    }

    private fun getFrequencyPlural(frequency: Frequency): Int {
        return when (frequency) {
            MINUTELY -> R.plurals.repeat_n_minutes
            HOURLY -> R.plurals.repeat_n_hours
            DAILY -> R.plurals.repeat_n_days
            WEEKLY -> R.plurals.repeat_n_weeks
            MONTHLY -> R.plurals.repeat_n_months
            YEARLY -> R.plurals.repeat_n_years
            else -> throw RuntimeException("Invalid frequency: $frequency")
        }
    }

    companion object {
        private val NTH_WEEK = intArrayOf(
                R.string.repeat_monthly_first_week,
                R.string.repeat_monthly_second_week,
                R.string.repeat_monthly_third_week,
                R.string.repeat_monthly_fourth_week,
                R.string.repeat_monthly_fifth_week
        )
    }
}