package org.tasks.icalendar

import kotlinx.datetime.TimeZone
import org.tasks.time.DateTime

private val DATE_TIME = Regex("(\\d{4})(\\d{2})(\\d{2})T(\\d{2})(\\d{2})(\\d{2})(Z?)")

fun Long.toICalDate(): ICalDate.Date = DateTime(this).let { ICalDate.Date(it.year, it.monthOfYear, it.dayOfMonth) }

fun Long.toICalDateTime(): ICalDate.DateTime = ICalDate.DateTime(this, TimeZone.currentSystemDefault().id)

fun ICalDate.localMillis(): Long = when (this) {
    is ICalDate.Date -> DateTime(year, month, day).millis
    is ICalDate.DateTime -> millis
}

fun parseICalDateTime(text: String): Long? = DATE_TIME.matchEntire(text.trim())?.let { match ->
    val (year, month, day, hour, minute, second, utc) = match.destructured
    val zone = if (utc.isEmpty()) TimeZone.currentSystemDefault() else DateTime.UTC
    DateTime(year.toInt(), month.toInt(), day.toInt(), hour.toInt(), minute.toInt(), second.toInt(), timeZone = zone).millis
}

fun formatICalUtc(millis: Long): String = DateTime(millis, DateTime.UTC).let {
    "${it.year.pad(4)}${it.monthOfYear.pad(2)}${it.dayOfMonth.pad(2)}T${it.hourOfDay.pad(2)}${it.minuteOfHour.pad(2)}${it.secondOfMinute.pad(2)}Z"
}

private fun Int.pad(width: Int) = toString().padStart(width, '0')
