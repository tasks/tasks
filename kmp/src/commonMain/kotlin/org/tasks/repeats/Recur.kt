package org.tasks.repeats

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
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
    val unknownParts: List<Pair<String, String>> = emptyList(),
) {
    override fun toString(): String = serializeRecur(this)

    companion object {
        private val LEGACY_RRULE_PREFIX = "^RRULE:".toRegex()

        private val KNOWN_PARTS = setOf(
            "FREQ", "UNTIL", "COUNT", "INTERVAL", "BYSECOND", "BYMINUTE", "BYHOUR", "BYDAY",
            "BYMONTHDAY", "BYYEARDAY", "BYWEEKNO", "BYMONTH", "BYSETPOS", "WKST",
        )

        fun parse(rrule: String): Recur {
            val value = rrule.replace(LEGACY_RRULE_PREFIX, "").sanitizeRecur().orEmpty()
            return parseRecur(value).copy(unknownParts = value.unknownParts())
        }

        private fun String.unknownParts(): List<Pair<String, String>> = split(';').mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) {
                null
            } else {
                part.substring(0, separator).uppercase()
                    .takeUnless { it in KNOWN_PARTS }
                    ?.let { it to part.substring(separator + 1) }
            }
        }
    }
}

internal expect fun parseRecur(rrule: String): Recur

internal const val RSCALE = "RSCALE"

internal fun serializeRecur(recur: Recur): String = buildString {
    recur.unknownParts
        .filter { (name, _) -> name == RSCALE }
        .forEach { (name, value) -> append(name).append("=").append(value).append(";") }
    append("FREQ=").append(recur.frequency.name)
    recur.weekStart?.let { append(";WKST=").append(it.name) }
    recur.until?.let { append(";UNTIL=").append(it.serialize()) }
    recur.count?.let { append(";COUNT=").append(it) }
    recur.interval?.let { append(";INTERVAL=").append(it) }
    appendList("BYMONTH", recur.byMonth)
    appendList("BYWEEKNO", recur.byWeekNo)
    appendList("BYYEARDAY", recur.byYearDay)
    appendList("BYMONTHDAY", recur.byMonthDay)
    appendList("BYDAY", recur.byDay.map { if (it.offset == 0) it.day.name else "${it.offset}${it.day.name}" })
    appendList("BYHOUR", recur.byHour)
    appendList("BYMINUTE", recur.byMinute)
    appendList("BYSECOND", recur.bySecond)
    appendList("BYSETPOS", recur.bySetPos)
    recur.unknownParts
        .filterNot { (name, _) -> name == RSCALE }
        .forEach { (name, value) -> append(";").append(name).append("=").append(value) }
}

private fun StringBuilder.appendList(name: String, values: List<Any>) {
    if (values.isNotEmpty()) append(";").append(name).append("=").append(values.joinToString(","))
}

private fun Until.serialize(): String = when (this) {
    is Until.Date -> "${year.pad(4)}${month.pad(2)}${day.pad(2)}"
    is Until.DateTime -> toDateTime().let {
        "${it.year.pad(4)}${it.monthOfYear.pad(2)}${it.dayOfMonth.pad(2)}T${it.hourOfDay.pad(2)}${it.minuteOfHour.pad(2)}${it.secondOfMinute.pad(2)}${if (utc) "Z" else ""}"
    }
}

private fun Int.pad(width: Int) = toString().padStart(width, '0')

expect fun Recur.nextOccurrence(start: DateTime, hasTime: Boolean): DateTime?

val DateTime.weekday: Weekday
    get() = Weekday.fromCalendarDay(dayOfWeek)

fun Weekday.displayName(style: TextStyle, languageTag: String? = null): String =
    formatDayOfWeek(DateTime(2024, 12, 21 + calendarDay).millis, style, languageTag)

val Weekday.dayOfWeek: DayOfWeek
    get() = DayOfWeek(if (this == Weekday.SU) 7 else ordinal)

fun DayOfWeek.toWeekday(): Weekday = Weekday.entries[isoDayNumber % 7]

fun DayOfWeek.displayName(style: TextStyle, languageTag: String? = null): String =
    toWeekday().displayName(style, languageTag)

fun Until.toDateTime(): DateTime = when (this) {
    is Until.Date -> DateTime(year, month, day)
    is Until.DateTime -> DateTime(millis, if (utc) DateTime.UTC else kotlinx.datetime.TimeZone.currentSystemDefault())
}
