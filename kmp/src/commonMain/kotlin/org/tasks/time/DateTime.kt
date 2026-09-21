package org.tasks.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.atTime
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.format.char
import kotlinx.datetime.format.format
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.offsetAt
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.tasks.data.dao.CaldavDao.Companion.toAppleEpoch
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import kotlin.jvm.JvmOverloads

class DateTime {
    val timeZone: TimeZone
    val millis: Long

    @JvmOverloads
    constructor(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        millisecond: Int = 0,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ) {
        millis = toMillis(year, month, day, hour, minute, second, millisecond, timeZone)
        this.timeZone = timeZone
    }

    @JvmOverloads
    constructor(timestamp: Long = currentTimeMillis(), timeZone: TimeZone = TimeZone.currentSystemDefault()) {
        this.millis = timestamp
        this.timeZone = timeZone
    }

    private constructor(local: LocalDateTime, timeZone: TimeZone) :
        this(local.toInstant(timeZone).toEpochMilliseconds(), timeZone)

    @JvmOverloads
    fun startOfDay(timeZone: TimeZone = this.timeZone): DateTime = DateTime(
        year = year,
        month = monthOfYear,
        day = dayOfMonth,
        timeZone = timeZone
    )

    fun startOfMinute(): DateTime = DateTime(millis.startOfMinute(), timeZone)

    fun startOfSecond(): DateTime = DateTime(millis.startOfSecond(), timeZone)

    fun endOfMinute(): DateTime = DateTime(millis.endOfMinute(), timeZone)

    fun noon(): DateTime = DateTime(
        year = year,
        month = monthOfYear,
        day = dayOfMonth,
        hour = 12,
        timeZone = timeZone
    )

    fun endOfDay(): DateTime = DateTime(
        year = year,
        month = monthOfYear,
        day = dayOfMonth,
        hour = 23,
        minute = 59,
        second = 59,
        timeZone = timeZone
    )

    fun withMillisOfDay(millisOfDay: Int): DateTime {
        val hours = millisOfDay / 3600000
        val minutes = (millisOfDay % 3600000) / 60000
        val seconds = (millisOfDay % 60000) / 1000
        val millis = millisOfDay % 1000
        return DateTime(
            year = year,
            month = monthOfYear,
            day = dayOfMonth,
            hour = hours,
            minute = minutes,
            second = seconds,
            millisecond = millis,
            timeZone = timeZone
        )
    }

    val offset: Long
        get() = timeZone.offsetAt(instant).totalSeconds * 1000L

    val millisOfDay: Int
        get() = local.time.toMillisecondOfDay()

    val year: Int
        get() = local.year

    val monthOfYear: Int
        get() = local.monthNumber

    val dayOfMonth: Int
        get() = local.dayOfMonth

    val dayOfWeek: Int
        get() = if (local.dayOfWeek == DayOfWeek.SUNDAY) 1 else local.dayOfWeek.isoDayNumber + 1

    val hourOfDay: Int
        get() = local.hour

    val minuteOfHour: Int
        get() = local.minute

    val secondOfMinute: Int
        get() = local.second

    private val millisOfSecond: Int
        get() = local.nanosecond / 1_000_000

    fun withYear(year: Int): DateTime = with(year = year)

    fun withMonthOfYear(monthOfYear: Int): DateTime = with(month = monthOfYear)

    fun withDayOfMonth(dayOfMonth: Int): DateTime = with(day = dayOfMonth)

    fun withHourOfDay(hourOfDay: Int): DateTime = with(hour = hourOfDay)

    fun withMinuteOfHour(minuteOfHour: Int): DateTime = with(minute = minuteOfHour)

    fun withSecondOfMinute(secondOfMinute: Int): DateTime = with(second = secondOfMinute)

    fun withMillisOfSecond(millisOfSecond: Int): DateTime = with(millisecond = millisOfSecond)

    fun plusMonths(interval: Int): DateTime = plusDateUnits(interval, DateTimeUnit.MONTH)

    fun plusWeeks(weeks: Int): DateTime = plusDateUnits(weeks, DateTimeUnit.WEEK)

    fun plusDays(interval: Int): DateTime = plusDateUnits(interval, DateTimeUnit.DAY)

    fun plusHours(hours: Int): DateTime = plusTimeUnits(hours, DateTimeUnit.HOUR)

    fun plusMinutes(minutes: Int): DateTime = plusTimeUnits(minutes, DateTimeUnit.MINUTE)

    fun plusSeconds(seconds: Int): DateTime = plusTimeUnits(seconds, DateTimeUnit.SECOND)

    fun plusMillis(millis: Int): DateTime = plusTimeUnits(millis, DateTimeUnit.MILLISECOND)

    fun minusSeconds(seconds: Int): DateTime = plusTimeUnits(-seconds, DateTimeUnit.SECOND)

    fun minusDays(days: Int): DateTime = plusDateUnits(-days, DateTimeUnit.DAY)

    fun minusMinutes(minutes: Int): DateTime = plusTimeUnits(-minutes, DateTimeUnit.MINUTE)

    fun minusMillis(millis: Long): DateTime = DateTime(this.millis - millis, timeZone)

    val isAfterNow: Boolean
        get() = isAfter(currentTimeMillis())

    fun isAfter(dateTime: DateTime): Boolean {
        return isAfter(dateTime.millis)
    }

    private fun isAfter(timestamp: Long): Boolean {
        return this.millis > timestamp
    }

    val isBeforeNow: Boolean
        get() = millis < currentTimeMillis()

    fun isBefore(dateTime: DateTime): Boolean {
        return millis < dateTime.millis
    }

    fun toUTC(): DateTime {
        return toTimeZone(UTC)
    }

    fun toLocal(): DateTime {
        return toTimeZone(TimeZone.currentSystemDefault())
    }

    val isLastDayOfMonth: Boolean
        get() = dayOfMonth == numberOfDaysInMonth

    val numberOfDaysInMonth: Int
        get() = firstOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth

    private fun toTimeZone(timeZone: TimeZone): DateTime {
        if (timeZone == this.timeZone) {
            return this
        }
        return DateTime(millis, timeZone)
    }

    private fun with(
        year: Int = this.year,
        month: Int = this.monthOfYear,
        day: Int = this.dayOfMonth,
        hour: Int = this.hourOfDay,
        minute: Int = this.minuteOfHour,
        second: Int = this.secondOfMinute,
        millisecond: Int = this.millisOfSecond,
    ): DateTime = DateTime(year, month, day, hour, minute, second, millisecond, timeZone)

    private fun plusDateUnits(value: Int, unit: DateTimeUnit.DateBased): DateTime =
        local.let { DateTime(it.date.plus(value, unit).atTime(it.time), timeZone) }

    private fun plusTimeUnits(value: Int, unit: DateTimeUnit.TimeBased): DateTime =
        DateTime(instant.plus(value, unit).toEpochMilliseconds(), timeZone)

    private val instant: Instant
        get() = Instant.fromEpochMilliseconds(millis)

    private val local: LocalDateTime
        get() = instant.toLocalDateTime(timeZone)

    private val firstOfMonth: LocalDate
        get() = local.let { LocalDate(it.year, it.month, 1) }

    fun toAppleEpoch(): Long {
        return millis.toAppleEpoch()
    }

    val dayOfWeekInMonth: Int
        get() = (dayOfMonth - 1) / 7 + 1

    val maxDayOfWeekInMonth: Int
        get() {
            val firstOccurrence = 1 + (local.dayOfWeek.isoDayNumber - firstOfMonth.dayOfWeek.isoDayNumber).mod(7)
            return (numberOfDaysInMonth - firstOccurrence) / 7 + 1
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is DateTime) {
            return false
        }
        return millis == other.millis && timeZone == other.timeZone
    }

    override fun hashCode(): Int {
        return 31 * timeZone.hashCode() + millis.hashCode()
    }

    @OptIn(FormatStringsInDatetimeFormats::class)
    fun toString(format: String): String =
        LocalDateTime.Format { byUnicodePattern(format) }.format(local)

    override fun toString(): String = DEFAULT_FORMAT.format {
        setDateTime(local)
        setOffset(timeZone.offsetAt(instant))
    }

    companion object {
        val MAX_MILLIS_PER_DAY: Int = ONE_DAY.toInt() - 1
        val UTC: TimeZone = TimeZone.UTC

        private val DEFAULT_FORMAT = DateTimeComponents.Format {
            year()
            char('-')
            monthNumber()
            char('-')
            dayOfMonth()
            char('T')
            hour()
            char(':')
            minute()
            char(':')
            second()
            char('.')
            secondFraction(3)
            offset(UtcOffset.Formats.FOUR_DIGITS)
        }

        private fun toMillis(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            second: Int,
            millisecond: Int,
            timeZone: TimeZone,
        ): Long {
            val timeOfDay = hour * ONE_HOUR + minute * ONE_MINUTE + second * ONE_SECOND + millisecond
            val date = LocalDate(year, 1, 1)
                .plus(month - 1, DateTimeUnit.MONTH)
                .plus(day - 1, DateTimeUnit.DAY)
                .plus(timeOfDay.floorDiv(ONE_DAY).toInt(), DateTimeUnit.DAY)
            val time = LocalTime.fromMillisecondOfDay(timeOfDay.mod(ONE_DAY).toInt())
            return date.atTime(time).toInstant(timeZone).toEpochMilliseconds()
        }
    }
}
