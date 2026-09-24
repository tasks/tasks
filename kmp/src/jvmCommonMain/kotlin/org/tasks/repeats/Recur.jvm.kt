package org.tasks.repeats

import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.Month
import net.fortuna.ical4j.model.MonthList
import net.fortuna.ical4j.model.NumberList
import net.fortuna.ical4j.model.WeekDay
import net.fortuna.ical4j.model.WeekDayList
import org.tasks.time.DateTime
import org.tasks.time.toDate
import org.tasks.time.toDateTime
import java.text.ParseException

internal actual fun parseRecur(rrule: String): Recur =
    try {
        net.fortuna.ical4j.model.Recur(rrule).toRecur()
    } catch (e: ParseException) {
        throw IllegalArgumentException(e)
    }

actual fun Recur.nextOccurrence(start: DateTime, hasTime: Boolean): DateTime? {
    val seed: Date = if (hasTime) start.toDateTime() else start.toDate()
    val next = toIcal4j().getNextDate(seed, seed) ?: return null
    return if (next is net.fortuna.ical4j.model.DateTime) {
        // time may be inaccurate due to DST, force time to be same
        DateTime(next.time, start.timeZone).withHourOfDay(start.hourOfDay).withMinuteOfHour(start.minuteOfHour)
    } else {
        val value = next.toString()
        DateTime(value.substring(0, 4).toInt(), value.substring(4, 6).toInt(), value.substring(6, 8).toInt(), timeZone = start.timeZone)
    }
}

fun net.fortuna.ical4j.model.Recur.toRecur(): Recur = Recur(
    frequency = Frequency.valueOf(frequency.name),
    interval = interval.takeIf { it >= 1 },
    count = count.takeIf { it >= 1 },
    until = until?.toUntil(),
    byDay = dayList.map { ByDay(Weekday.valueOf(it.day.name), it.offset) },
    byMonthDay = monthDayList.toList(),
    byMonth = monthList.map { it.monthOfYear },
    bySecond = secondList.toList(),
    byMinute = minuteList.toList(),
    byHour = hourList.toList(),
    byYearDay = yearDayList.toList(),
    byWeekNo = weekNoList.toList(),
    bySetPos = setPosList.toList(),
    weekStart = weekStartDay?.let { Weekday.valueOf(it.name) },
)

fun Recur.toIcal4j(): net.fortuna.ical4j.model.Recur =
    net.fortuna.ical4j.model.Recur.Builder()
        .frequency(net.fortuna.ical4j.model.Recur.Frequency.valueOf(frequency.name))
        .interval(interval)
        .count(count)
        .until(until?.toIcal4j())
        .apply {
            if (byDay.isNotEmpty()) dayList(WeekDayList(*this@toIcal4j.byDay.map { it.toIcal4j() }.toTypedArray()))
            if (byMonthDay.isNotEmpty()) monthDayList(byMonthDay.toNumberList())
            if (byMonth.isNotEmpty()) monthList(MonthList().apply { byMonth.forEach { add(Month(it)) } })
            if (bySecond.isNotEmpty()) secondList(bySecond.toNumberList())
            if (byMinute.isNotEmpty()) minuteList(byMinute.toNumberList())
            if (byHour.isNotEmpty()) hourList(byHour.toNumberList())
            if (byYearDay.isNotEmpty()) yearDayList(byYearDay.toNumberList())
            if (byWeekNo.isNotEmpty()) weekNoList(byWeekNo.toNumberList())
            if (bySetPos.isNotEmpty()) setPosList(bySetPos.toNumberList())
            weekStart?.let { weekStartDay(WeekDay.Day.valueOf(it.name)) }
        }
        .build()

fun ByDay.toIcal4j(): WeekDay = WeekDay(WeekDay.getWeekDay(WeekDay.Day.valueOf(day.name)), offset)

private fun Date.toUntil(): Until =
    if (this is net.fortuna.ical4j.model.DateTime) {
        Until.DateTime(time, isUtc)
    } else {
        val value = toString()
        Until.Date(value.substring(0, 4).toInt(), value.substring(4, 6).toInt(), value.substring(6, 8).toInt())
    }

private fun Until.toIcal4j(): Date = when (this) {
    is Until.Date -> Date(String.format("%04d%02d%02d", year, month, day))
    is Until.DateTime -> net.fortuna.ical4j.model.DateTime(millis).also { it.isUtc = utc }
}

private fun List<Int>.toNumberList(): NumberList = NumberList().apply { addAll(this@toNumberList) }
