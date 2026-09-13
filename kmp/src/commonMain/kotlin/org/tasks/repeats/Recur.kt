package org.tasks.repeats

import org.tasks.data.entity.Task.Companion.sanitizeRecur
import org.tasks.kmp.formatDayOfWeek
import org.tasks.kmp.org.tasks.time.TextStyle
import org.tasks.time.DateTime

enum class Frequency {
    SECONDLY, MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY
}

enum class Weekday {
    SU, MO, TU, WE, TH, FR, SA;

    val calendarDay: Int
        get() = ordinal + 1

    companion object {
        fun fromCalendarDay(calendarDay: Int): Weekday = entries[calendarDay - 1]
    }
}

data class ByDay(val day: Weekday, val offset: Int = 0)

sealed interface Until {
    data class Date(val year: Int, val month: Int, val day: Int) : Until

    data class DateTime(val millis: Long, val utc: Boolean) : Until
}

data class Recur(
    val frequency: Frequency,
    val interval: Int? = null,
    val count: Int? = null,
    val until: Until? = null,
    val byDay: List<ByDay> = emptyList(),
    val byMonthDay: List<Int> = emptyList(),
    val byMonth: List<Int> = emptyList(),
    val bySecond: List<Int> = emptyList(),
    val byMinute: List<Int> = emptyList(),
    val byHour: List<Int> = emptyList(),
    val byYearDay: List<Int> = emptyList(),
    val byWeekNo: List<Int> = emptyList(),
    val bySetPos: List<Int> = emptyList(),
    val weekStart: Weekday? = null,
) {
    override fun toString(): String = serializeRecur(this)

    companion object {
        private val LEGACY_RRULE_PREFIX = "^RRULE:".toRegex()

        fun parse(rrule: String): Recur =
            parseRecur(rrule.replace(LEGACY_RRULE_PREFIX, "").sanitizeRecur().orEmpty())
    }
}

internal expect fun parseRecur(rrule: String): Recur

internal expect fun serializeRecur(recur: Recur): String

expect fun Recur.nextOccurrence(start: DateTime, hasTime: Boolean): DateTime?

val DateTime.weekday: Weekday
    get() = Weekday.fromCalendarDay(dayOfWeek)

fun Weekday.displayName(style: TextStyle): String =
    formatDayOfWeek(DateTime(2024, 12, 21 + calendarDay).millis, style)

fun Until.toDateTime(): DateTime = when (this) {
    is Until.Date -> DateTime(year, month, day)
    is Until.DateTime -> DateTime(millis, if (utc) DateTime.UTC else kotlinx.datetime.TimeZone.currentSystemDefault())
}
