package org.tasks.googleapis

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

object GtasksDates {

    fun parse(value: String): Long {
        val dateOnly = DATE_ONLY.matchEntire(value)
        if (dateOnly != null) {
            return LocalDate.parse(value).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        }
        if (!HAS_OFFSET.containsMatchIn(value)) {
            return LocalDateTime.parse(value).toInstant(TimeZone.UTC).toEpochMilliseconds()
        }
        return Instant.parse(value).toEpochMilliseconds()
    }

    fun unixTimeToGtasksCompletionTime(time: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String? {
        if (time < 0) {
            return null
        }
        val offsetMinutes = timeZone.offsetAt(Instant.fromEpochMilliseconds(time)).totalSeconds / 60
        return format(time, offsetMinutes)
    }

    fun gtasksCompletedTimeToUnixTime(completed: String?): Long =
        completed?.let(::parse) ?: 0

    fun unixTimeToGtasksDueDate(time: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String? {
        if (time < 0) {
            return null
        }
        val truncated = Instant.fromEpochMilliseconds(time / 1000 * 1000)
        val midnight = truncated.toLocalDateTime(timeZone).date.atStartOfDayIn(timeZone)
        val offsetMillis = timeZone.offsetAt(midnight).totalSeconds / 60 * 60_000L
        return format(midnight.toEpochMilliseconds() + offsetMillis, 0)
    }

    fun gtasksDueTimeToUnixTime(due: String?, timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
        if (due == null) {
            return 0
        }
        val utc = parse(due)
        val offsetMillis = timeZone.offsetAt(Instant.fromEpochMilliseconds(utc)).totalSeconds / 60 * 60_000L
        return utc - offsetMillis
    }

    internal fun format(epochMillis: Long, offsetMinutes: Int): String {
        val local = Instant
            .fromEpochMilliseconds(epochMillis + offsetMinutes * 60_000L)
            .toLocalDateTime(TimeZone.UTC)
        return buildString {
            append(local.year.pad(4))
            append('-')
            append(local.monthNumber.pad(2))
            append('-')
            append(local.dayOfMonth.pad(2))
            append('T')
            append(local.hour.pad(2))
            append(':')
            append(local.minute.pad(2))
            append(':')
            append(local.second.pad(2))
            append('.')
            append((local.nanosecond / 1_000_000).pad(3))
            if (offsetMinutes == 0) {
                append('Z')
            } else {
                append(if (offsetMinutes > 0) '+' else '-')
                val absolute = abs(offsetMinutes)
                append((absolute / 60).pad(2))
                append(':')
                append((absolute % 60).pad(2))
            }
        }
    }

    private fun Int.pad(length: Int) = toString().padStart(length, '0')

    private val DATE_ONLY = Regex("""\d{4}-\d{2}-\d{2}""")
    private val HAS_OFFSET = Regex("""([Zz]|[+-]\d{2}:\d{2})$""")
}
