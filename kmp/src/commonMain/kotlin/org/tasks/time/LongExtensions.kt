package org.tasks.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun Long.noon(): Long =
    if (this > 0) {
        toLocalDateTime()
            .let { LocalDateTime(it.year, it.month, it.dayOfMonth, 12, 0, 0, 0) }
            .toEpochMilliseconds()
    } else {
        0
    }

fun Long.startOfDay(): Long =
    if (this > 0) {
        toLocalDateTime()
            .let { LocalDateTime(it.year, it.month, it.dayOfMonth, 0, 0, 0, 0) }
            .toEpochMilliseconds()
    } else {
        0
    }

fun Long.startOfDayUtc(): Long =
    if (this > 0) this - this % ONE_DAY else 0

fun Long.startOfMinute(): Long =
    if (this > 0) {
        this - this % ONE_MINUTE
    } else {
        0
    }

fun Long.startOfSecond(): Long =
    if (this > 0) {
        toLocalDateTime()
            .let {
                LocalDateTime(
                    it.year,
                    it.month,
                    it.dayOfMonth,
                    it.hour,
                    it.minute,
                    it.second,
                    0
                )
            }
            .toEpochMilliseconds()
    } else {
        0
    }

fun Long.endOfMinute(): Long =
    if (this > 0) {
        toLocalDateTime()
            .let {
                LocalDateTime(
                    it.year,
                    it.month,
                    it.dayOfMonth,
                    it.hour,
                    it.minute,
                    59,
                    999_000_000
                )
            }
            .toEpochMilliseconds()
            .coerceAtLeast(this)
    } else {
        0
    }

fun Long.endOfDay(): Long =
    if (this > 0) {
        val date = toLocalDateTime().date
        val candidate = date.atTimeOnThisDate(LocalTime(23, 59, 59))
        val lastMillis = date.lastMillisOfDate()
        if (lastMillis - candidate < ONE_SECOND) candidate else lastMillis
    } else {
        0
    }

fun Long.withMillisOfDay(millisOfDay: Int): Long =
    if (this > 0) {
        toLocalDateTime()
            .date
            .atTimeOnThisDate(LocalTime.fromMillisecondOfDay(millisOfDay))
    } else {
        0
    }

fun Long.plusDays(days: Int): Long = when {
    this <= 0 -> 0
    days == 0 -> this
    else -> with (toLocalDateTime()) {
        date
            .plus(days, DateTimeUnit.DAY)
            .atTimeOnThisDate(time)
    }
}

fun Long.daysUntil(other: Long): Int =
    if (this > 0 && other > 0) {
        toLocalDateTime().date.daysUntil(other.toLocalDateTime().date)
    } else {
        0
    }

fun Long.minusDays(days: Int): Long = plusDays(-days)

fun Long.shiftFollowingDueDate(oldDueDate: Long, newDueDate: Long): Long {
    val days = oldDueDate.daysUntil(newDueDate)
    return if (days != 0 && oldDueDate.millisOfDay == newDueDate.millisOfDay) {
        plusDays(days)
    } else {
        this + newDueDate - oldDueDate
    }
}

fun Long.minusMinutes(minutes: Int): Long = minus(minutes, DateTimeUnit.MINUTE)

fun Long.minusMillis(millis: Long): Long = minus(millis.toInt(), DateTimeUnit.MILLISECOND)

private fun Long.minus(value: Int, units: DateTimeUnit.TimeBased): Long =
    if (this > 0) {
        Instant
            .fromEpochMilliseconds(this)
            .minus(value, units)
            .toEpochMilliseconds()
    } else {
        0
    }

val Long.millisOfDay: Int
    get() = if (this > 0) toLocalDateTime().time.toMillisecondOfDay() else 0

val Long.hourOfDay: Int
    get() = if (this > 0) toLocalDateTime().hour else 0

val Long.minuteOfHour: Int
    get() = if (this > 0) toLocalDateTime().minute else 0

val Long.year: Int
    get() = if (this > 0) toLocalDateTime().year else 0

fun Long.toUtcDateMillis(): Long =
    toLocalDateTime()
        .date
        .atStartOfDayIn(TimeZone.UTC)
        .toEpochMilliseconds()

fun Long.toLocalDateMillis(): Long =
    Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.UTC)
        .date
        .atStartOfDayIn(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()

private fun Long.toLocalDateTime(): LocalDateTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())

private fun LocalDateTime.toEpochMilliseconds(): Long =
    toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

private fun LocalDate.atTimeOnThisDate(time: LocalTime): Long {
    val millis = atTime(time).toEpochMilliseconds()
    if (millis.toLocalDateTime().date == this) {
        return millis
    }
    val lastMillis = lastMillisOfDate()
    return if (lastMillis.toLocalDateTime().date == this) lastMillis else millis
}

private fun LocalDate.lastMillisOfDate(): Long =
    plus(1, DateTimeUnit.DAY)
        .atStartOfDayIn(TimeZone.currentSystemDefault())
        .toEpochMilliseconds() - 1
