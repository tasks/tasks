package org.tasks.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
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
    } else {
        0
    }

fun Long.endOfDay(): Long =
    if (this > 0) {
        toLocalDateTime()
            .let { LocalDateTime(it.year, it.month, it.dayOfMonth, 23, 59, 59, 0) }
            .toEpochMilliseconds()
    } else {
        0
    }

fun Long.withMillisOfDay(millisOfDay: Int): Long =
    if (this > 0) {
        LocalDateTime(
            date = toLocalDateTime().date,
            time = LocalTime.fromMillisecondOfDay(millisOfDay)
        )
            .toEpochMilliseconds()
    } else {
        0
    }

fun Long.plusDays(days: Int): Long =
    if (this > 0) {
        with (toLocalDateTime()) {
            date
                .plus(days, DateTimeUnit.DAY)
                .atTime(time)
                .toEpochMilliseconds()
        }
    } else {
        0
    }

fun Long.minusDays(days: Int): Long =
    if (this > 0) {
        with (toLocalDateTime()) {
            date
                .minus(days, DateTimeUnit.DAY)
                .atTime(time)
                .toEpochMilliseconds()
        }
    } else {
        0
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
