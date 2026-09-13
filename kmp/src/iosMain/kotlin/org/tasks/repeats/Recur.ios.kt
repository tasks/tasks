package org.tasks.repeats

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import libical.ICAL_BY_DAY
import libical.ICAL_BY_HOUR
import libical.ICAL_BY_MINUTE
import libical.ICAL_BY_MONTH
import libical.ICAL_BY_MONTH_DAY
import libical.ICAL_BY_SECOND
import libical.ICAL_BY_SET_POS
import libical.ICAL_BY_WEEK_NO
import libical.ICAL_BY_YEAR_DAY
import libical.icalrecur_iterator_free
import libical.icalrecur_iterator_new
import libical.icalrecur_iterator_next
import libical.icalrecurrence_by_data
import libical.icalrecurrencetype
import libical.icalrecurrencetype_day_day_of_week
import libical.icalrecurrencetype_day_position
import libical.icalrecurrencetype_month_month
import libical.icalrecurrencetype_new_from_string
import libical.icalrecurrencetype_unref
import libical.icaltime_from_string
import libical.icaltime_is_null_time
import libical.icaltime_is_utc
import libical.icaltimetype
import org.tasks.time.DateTime

private val CLAUSE_INTERVAL = Regex("(^|;)INTERVAL=", RegexOption.IGNORE_CASE)
private val CLAUSE_WKST = Regex("(^|;)WKST=", RegexOption.IGNORE_CASE)

@OptIn(ExperimentalForeignApi::class)
internal actual fun parseRecur(rrule: String): Recur = withRecur(rrule) { r ->
    Recur(
        frequency = Frequency.entries[r.freq.toInt()],
        interval = r.interval.toInt().takeIf { CLAUSE_INTERVAL.containsMatchIn(rrule) },
        count = r.count.takeIf { it > 0 },
        until = r.until.toUntil(),
        byDay = r.by[ICAL_BY_DAY].values().map {
            ByDay(Weekday.fromCalendarDay(icalrecurrencetype_day_day_of_week(it).toInt()), icalrecurrencetype_day_position(it))
        },
        byMonthDay = r.by[ICAL_BY_MONTH_DAY].values().map { it.toInt() },
        byMonth = r.by[ICAL_BY_MONTH].values().map { icalrecurrencetype_month_month(it) },
        bySecond = r.by[ICAL_BY_SECOND].values().map { it.toInt() },
        byMinute = r.by[ICAL_BY_MINUTE].values().map { it.toInt() },
        byHour = r.by[ICAL_BY_HOUR].values().map { it.toInt() },
        byYearDay = r.by[ICAL_BY_YEAR_DAY].values().map { it.toInt() },
        byWeekNo = r.by[ICAL_BY_WEEK_NO].values().map { it.toInt() },
        bySetPos = r.by[ICAL_BY_SET_POS].values().map { it.toInt() },
        weekStart = Weekday.fromCalendarDay(r.week_start.toInt()).takeIf { CLAUSE_WKST.containsMatchIn(rrule) },
    )
}

@OptIn(ExperimentalForeignApi::class)
actual fun Recur.nextOccurrence(start: DateTime, hasTime: Boolean): DateTime? =
    withRecur(serializeRecur(copy(until = null))) { rule ->
        val untilMillis = until?.toDateTime()?.millis
        val dtstart = icaltime_from_string(if (hasTime) start.floatingDateTime() else start.floatingDate())
        val iterator = icalrecur_iterator_new(rule.ptr, dtstart) ?: return@withRecur null
        try {
            while (true) {
                val next = icalrecur_iterator_next(iterator)
                if (icaltime_is_null_time(next)) return@withRecur null
                val candidate = next.useContents {
                    if (hasTime) {
                        DateTime(year, month, day, start.hourOfDay, start.minuteOfHour, start.secondOfMinute, timeZone = start.timeZone)
                    } else {
                        DateTime(year, month, day, timeZone = start.timeZone)
                    }
                }
                if (untilMillis != null && candidate.millis > untilMillis) return@withRecur null
                if (candidate.millis > start.millis) return@withRecur candidate
            }
            @Suppress("UNREACHABLE_CODE")
            null
        } finally {
            icalrecur_iterator_free(iterator)
        }
    }

@OptIn(ExperimentalForeignApi::class)
private inline fun <T> withRecur(rrule: String, block: (icalrecurrencetype) -> T): T {
    val recur: CPointer<icalrecurrencetype> = icalrecurrencetype_new_from_string(rrule)
        ?: throw IllegalArgumentException("Invalid recurrence rule: $rrule")
    try {
        return block(recur.pointed)
    } finally {
        icalrecurrencetype_unref(recur)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun icalrecurrence_by_data.values(): List<Short> {
    val data = data ?: return emptyList()
    return List(size.toInt()) { data[it] }
}

@OptIn(ExperimentalForeignApi::class)
private fun icaltimetype.toUntil(): Until? = when {
    year == 0 -> null
    is_date != 0 -> Until.Date(year, month, day)
    icaltime_is_utc(readValue()) ->
        Until.DateTime(DateTime(year, month, day, hour, minute, second, timeZone = DateTime.UTC).millis, utc = true)
    else -> Until.DateTime(DateTime(year, month, day, hour, minute, second).millis, utc = false)
}

private fun DateTime.floatingDate(): String = "${year.pad(4)}${monthOfYear.pad(2)}${dayOfMonth.pad(2)}"

private fun DateTime.floatingDateTime(): String =
    "${floatingDate()}T${hourOfDay.pad(2)}${minuteOfHour.pad(2)}${secondOfMinute.pad(2)}"

private fun Int.pad(width: Int) = toString().padStart(width, '0')
