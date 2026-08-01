package org.tasks.repeats

import kotlinx.coroutines.runBlocking
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.Recur.Frequency
import net.fortuna.ical4j.model.Recur.Frequency.DAILY
import net.fortuna.ical4j.model.Recur.Frequency.HOURLY
import net.fortuna.ical4j.model.Recur.Frequency.MINUTELY
import net.fortuna.ical4j.model.Recur.Frequency.MONTHLY
import net.fortuna.ical4j.model.Recur.Frequency.WEEKLY
import net.fortuna.ical4j.model.Recur.Frequency.YEARLY
import net.fortuna.ical4j.model.WeekDay.Day
import net.fortuna.ical4j.model.WeekDay.Day.FR
import net.fortuna.ical4j.model.WeekDay.Day.MO
import net.fortuna.ical4j.model.WeekDay.Day.SA
import net.fortuna.ical4j.model.WeekDay.Day.SU
import net.fortuna.ical4j.model.WeekDay.Day.TH
import net.fortuna.ical4j.model.WeekDay.Day.TU
import net.fortuna.ical4j.model.WeekDay.Day.WE
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.tasks.analytics.CrashReporting
import org.tasks.extensions.formatNumber
import org.tasks.kmp.org.tasks.time.DateFormatter
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTime
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.list_separator_with_space
import tasks.kmp.generated.resources.repeat_monthly_every_day_of_nth_week
import tasks.kmp.generated.resources.repeat_monthly_fifth_week
import tasks.kmp.generated.resources.repeat_monthly_first_week
import tasks.kmp.generated.resources.repeat_monthly_fourth_week
import tasks.kmp.generated.resources.repeat_monthly_last_week
import tasks.kmp.generated.resources.repeat_monthly_second_week
import tasks.kmp.generated.resources.repeat_monthly_third_week
import tasks.kmp.generated.resources.repeat_n_days
import tasks.kmp.generated.resources.repeat_n_hours
import tasks.kmp.generated.resources.repeat_n_minutes
import tasks.kmp.generated.resources.repeat_n_months
import tasks.kmp.generated.resources.repeat_n_weeks
import tasks.kmp.generated.resources.repeat_n_years
import tasks.kmp.generated.resources.repeat_times
import tasks.kmp.generated.resources.repeats_daily
import tasks.kmp.generated.resources.repeats_hourly
import tasks.kmp.generated.resources.repeats_minutely
import tasks.kmp.generated.resources.repeats_monthly
import tasks.kmp.generated.resources.repeats_plural
import tasks.kmp.generated.resources.repeats_plural_number_of_times
import tasks.kmp.generated.resources.repeats_plural_on
import tasks.kmp.generated.resources.repeats_plural_on_number_of_times
import tasks.kmp.generated.resources.repeats_plural_on_until
import tasks.kmp.generated.resources.repeats_plural_until
import tasks.kmp.generated.resources.repeats_single
import tasks.kmp.generated.resources.repeats_single_number_of_times
import tasks.kmp.generated.resources.repeats_single_on
import tasks.kmp.generated.resources.repeats_single_on_number_of_times
import tasks.kmp.generated.resources.repeats_single_on_until
import tasks.kmp.generated.resources.repeats_single_until
import tasks.kmp.generated.resources.repeats_weekly
import tasks.kmp.generated.resources.repeats_yearly

class RepeatRuleToString(
    private val locale: Locale,
    private val crashReporting: CrashReporting,
) {
    private val weekdays = listOf(*Day.values())

    suspend fun toString(rrule: String?): String? =
        rrule?.takeIf { it.isNotBlank() }?.let { toString(newRecur(it)) }

    fun toStringBlocking(rrule: String?): String? = runBlocking { toString(rrule) }

    private suspend fun toString(rrule: Recur): String = try {
        val dateFormatter = DateFormatter.create(is24HourFormat = false)
        val interval = rrule.interval
        val frequency = rrule.frequency
        val repeatUntil = if (rrule.until == null) null else DateTime.from(rrule.until)
        val count = rrule.count
        val countString = if (count > 0) getPluralString(Res.plurals.repeat_times, count) else ""
        val countNumber = if (count > 0) locale.formatNumber(count) else ""
        if (interval <= 1) {
            val frequencyString = getString(getSingleFrequencyResource(frequency))
            if ((frequency == WEEKLY || frequency == MONTHLY) && !rrule.dayList.isEmpty()) {
                val dayString = getDayString(rrule)
                when {
                    count > 0 -> getString(
                        Res.string.repeats_single_on_number_of_times,
                        frequencyString,
                        dayString,
                        countNumber,
                        countString
                    )
                    repeatUntil == null ->
                        getString(Res.string.repeats_single_on, frequencyString, dayString)
                    else -> getString(
                        Res.string.repeats_single_on_until,
                        frequencyString,
                        dayString,
                        dateFormatter.fullDate(repeatUntil.millis)
                    )
                }
            } else if (count > 0) {
                getString(
                    Res.string.repeats_single_number_of_times,
                    frequencyString,
                    countNumber,
                    countString
                )
            } else if (repeatUntil == null) {
                getString(Res.string.repeats_single, frequencyString)
            } else {
                getString(
                    Res.string.repeats_single_until,
                    frequencyString,
                    dateFormatter.fullDate(repeatUntil.millis)
                )
            }
        } else {
            val frequencyPlural = getPluralString(
                getFrequencyPlural(frequency),
                interval,
                locale.formatNumber(interval)
            )
            if ((frequency == WEEKLY || frequency == MONTHLY) && !rrule.dayList.isEmpty()) {
                val dayString = getDayString(rrule)
                when {
                    count > 0 -> getString(
                        Res.string.repeats_plural_on_number_of_times,
                        frequencyPlural,
                        dayString,
                        countNumber,
                        countString
                    )
                    repeatUntil == null ->
                        getString(Res.string.repeats_plural_on, frequencyPlural, dayString)
                    else -> getString(
                        Res.string.repeats_plural_on_until,
                        frequencyPlural,
                        dayString,
                        dateFormatter.fullDate(repeatUntil.millis)
                    )
                }
            } else if (count > 0) {
                getString(
                    Res.string.repeats_plural_number_of_times,
                    frequencyPlural,
                    countNumber,
                    countString
                )
            } else if (repeatUntil == null) {
                getString(Res.string.repeats_plural, frequencyPlural)
            } else {
                getString(
                    Res.string.repeats_plural_until,
                    frequencyPlural,
                    dateFormatter.fullDate(repeatUntil.millis)
                )
            }
        }
    } catch (e: Exception) {
        crashReporting.reportException(IllegalStateException("$rrule caused $e"))
        rrule.toString()
    }

    private suspend fun getDayString(rrule: Recur): String {
        val dfs = DateFormatSymbols(locale)
        return if (rrule.frequency == WEEKLY) {
            val shortWeekdays = dfs.shortWeekdays
            val days: MutableList<String?> = ArrayList()
            for (weekday in rrule.dayList) {
                days.add(shortWeekdays[weekdays.indexOf(weekday.day) + 1])
            }
            days.joinToString(getString(Res.string.list_separator_with_space))
        } else if (rrule.frequency == MONTHLY) {
            val longWeekdays = dfs.weekdays
            val weekdayNum = rrule.dayList[0]
            val dayOfWeekCalendar = Calendar.getInstance(locale)
            dayOfWeekCalendar[Calendar.DAY_OF_WEEK] = weekdayToCalendarDay(weekdayNum.day)
            val weekday = longWeekdays[dayOfWeekCalendar[Calendar.DAY_OF_WEEK]]
            val nthWeek = getString(
                if (weekdayNum.offset == -1) {
                    Res.string.repeat_monthly_last_week
                } else {
                    NTH_WEEK[weekdayNum.offset - 1]
                }
            )
            String.format(
                locale,
                getString(Res.string.repeat_monthly_every_day_of_nth_week),
                nthWeek,
                weekday
            )
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

    private fun getSingleFrequencyResource(frequency: Frequency): StringResource {
        return when (frequency) {
            MINUTELY -> Res.string.repeats_minutely
            HOURLY -> Res.string.repeats_hourly
            DAILY -> Res.string.repeats_daily
            WEEKLY -> Res.string.repeats_weekly
            MONTHLY -> Res.string.repeats_monthly
            YEARLY -> Res.string.repeats_yearly
            else -> throw RuntimeException("Invalid frequency: $frequency")
        }
    }

    private fun getFrequencyPlural(frequency: Frequency): PluralStringResource {
        return when (frequency) {
            MINUTELY -> Res.plurals.repeat_n_minutes
            HOURLY -> Res.plurals.repeat_n_hours
            DAILY -> Res.plurals.repeat_n_days
            WEEKLY -> Res.plurals.repeat_n_weeks
            MONTHLY -> Res.plurals.repeat_n_months
            YEARLY -> Res.plurals.repeat_n_years
            else -> throw RuntimeException("Invalid frequency: $frequency")
        }
    }

    companion object {
        private val NTH_WEEK = listOf(
            Res.string.repeat_monthly_first_week,
            Res.string.repeat_monthly_second_week,
            Res.string.repeat_monthly_third_week,
            Res.string.repeat_monthly_fourth_week,
            Res.string.repeat_monthly_fifth_week,
        )
    }
}
