package org.tasks.time

import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.toKotlinTimeZone
import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.WeekDay
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime

fun DateTime.toDateTime(): net.fortuna.ical4j.model.DateTime {
    return if (millis == 0L) throw IllegalStateException() else net.fortuna.ical4j.model.DateTime(millis)
}

fun DateTime.toDate(): Date {
    return if (millis == 0L) throw IllegalStateException() else Date(millis + offset)
}

fun DateTime.toLocalDate(): LocalDate? {
    return if (millis == 0L) null else LocalDate.of(year, monthOfYear, dayOfMonth)
}

fun DateTime.toLocalDateTime(): LocalDateTime? {
    return if (millis == 0L) null else LocalDateTime.of(
        year,
        monthOfYear,
        dayOfMonth,
        hourOfDay,
        minuteOfHour
    )
}

val DateTime.weekDay: WeekDay
    get() = when (dayOfWeek) {
        1 -> WeekDay.SU
        2 -> WeekDay.MO
        3 -> WeekDay.TU
        4 -> WeekDay.WE
        5 -> WeekDay.TH
        6 -> WeekDay.FR
        7 -> WeekDay.SA
        else -> throw RuntimeException()
    }

fun DateTime.Companion.from(date: java.util.Date?): DateTime {
    if (date == null) {
        return DateTime(0)
    }
    val dateTime = DateTime(date.time)
    return dateTime.minusMillis(dateTime.offset)
}

fun DateTime.Companion.from(date: Date?): DateTime {
    if (date is net.fortuna.ical4j.model.DateTime) {
        val tz: java.util.TimeZone? = date.timeZone
        return DateTime(
            date.time,
            tz?.toKotlinTimeZone(date.time) ?: if (date.isUtc) DateTime.UTC else TimeZone.currentSystemDefault()
        )
    } else {
        return from(date as java.util.Date?)
    }
}

private fun java.util.TimeZone.toKotlinTimeZone(millis: Long): TimeZone =
    try {
        toZoneId().toKotlinTimeZone()
    } catch (e: DateTimeException) {
        fixedOffsetAt(millis)
    }

private fun java.util.TimeZone.fixedOffsetAt(millis: Long): TimeZone =
    UtcOffset(seconds = getOffset(millis) / 1000).asTimeZone()
