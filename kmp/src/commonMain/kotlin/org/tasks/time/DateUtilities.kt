package org.tasks.kmp.org.tasks.time

import org.jetbrains.compose.resources.getString
import org.tasks.data.entity.Task.Companion.hasDueTime
import org.tasks.kmp.formatDate
import org.tasks.kmp.formatDateTime
import org.tasks.kmp.formatDayOfWeek
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_DAY
import org.tasks.time.minuteOfHour
import org.tasks.time.plusDays
import org.tasks.time.startOfDay
import org.tasks.time.year
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.tmrw
import tasks.kmp.generated.resources.today
import tasks.kmp.generated.resources.today_lowercase
import tasks.kmp.generated.resources.tomorrow
import tasks.kmp.generated.resources.tomorrow_abbrev_lowercase
import tasks.kmp.generated.resources.tomorrow_lowercase
import tasks.kmp.generated.resources.yest
import tasks.kmp.generated.resources.yesterday
import tasks.kmp.generated.resources.yesterday_abbrev_lowercase
import tasks.kmp.generated.resources.yesterday_lowercase
import kotlin.math.abs

fun getTimeString(date: Long, is24HourFormat: Boolean): String =
    formatDateTime(
        timestamp = date,
        format = when {
            is24HourFormat -> "HH:mm"
            date.minuteOfHour == 0 -> "h a"
            else -> "h:mm a"
        }
    )

suspend fun getRelativeDateTime(
    date: Long,
    is24HourFormat: Boolean,
    style: DateStyle = DateStyle.MEDIUM,
    alwaysDisplayFullDate: Boolean = false,
    lowercase: Boolean = false
): String {
    if (alwaysDisplayFullDate || !isWithinSixDays(date)) {
        return if (hasDueTime(date))
            getFullDateTime(date, style)
        else
            getFullDate(date, style)
    }

    val day = getRelativeDay(date, isAbbreviated(style), lowercase)
    return if (hasDueTime(date)) {
        val time = getTimeString(date, is24HourFormat)
        if (currentTimeMillis().startOfDay() == date.startOfDay())
            time
        else
            String.format("%s %s", day, time)
    } else {
        day
    }
}

suspend fun getRelativeDay(
    date: Long,
    style: DateStyle = DateStyle.MEDIUM,
    alwaysDisplayFullDate: Boolean = false,
    lowercase: Boolean = false,
): String =
    if (alwaysDisplayFullDate || !isWithinSixDays(date)) {
        getFullDate(date, style)
    } else {
        getRelativeDay(date, isAbbreviated(style), lowercase)
    }

fun getFullDate(
    date: Long,
    style: DateStyle = DateStyle.LONG,
): String = stripYear(formatDate(date, style), currentTimeMillis().year)

fun getFullDateTime(
    date: Long,
    style: DateStyle = DateStyle.LONG,
): String = stripYear(formatDateTime(date, style), currentTimeMillis().year)

private fun isAbbreviated(style: DateStyle): Boolean =
    style == DateStyle.SHORT || style == DateStyle.MEDIUM

private fun stripYear(date: String, year: Int): String =
    date.replace("(?: de |, |/| )?$year(?:年|년 | г\\.)?".toRegex(), "")

private suspend fun getRelativeDay(
    date: Long,
    abbreviated: Boolean,
    lowercase: Boolean
): String {
    val startOfToday = currentTimeMillis().startOfDay()
    val startOfDate = date.startOfDay()

    if (startOfToday == startOfDate) {
        return getString(if (lowercase) Res.string.today_lowercase else Res.string.today)
    }

    if (startOfToday.plusDays(1) == startOfDate) {
        return getString(
            if (abbreviated) {
                if (lowercase) Res.string.tomorrow_abbrev_lowercase else Res.string.tmrw
            } else {
                if (lowercase) Res.string.tomorrow_lowercase else Res.string.tomorrow
            }
        )
    }

    if (startOfDate.plusDays(1) == startOfToday) {
        return getString(
            when {
                abbreviated ->
                    if (lowercase) Res.string.yesterday_abbrev_lowercase else Res.string.yest

                lowercase ->
                    Res.string.yesterday_lowercase

                else ->
                    Res.string.yesterday
            }
        )
    }

    return formatDayOfWeek(
        timestamp = date,
        style = if (abbreviated) TextStyle.SHORT else TextStyle.FULL
    )
}

private fun isWithinSixDays(date: Long): Boolean {
    val startOfToday = currentTimeMillis().startOfDay()
    val startOfDate = date.startOfDay()
    return abs((startOfToday - startOfDate).toDouble()) <= ONE_DAY * 6
}