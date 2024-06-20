package org.tasks.time

import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.WeekDay
import org.tasks.data.dao.CaldavDao.Companion.toAppleEpoch
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.Objects
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class DateTime {
    private val timeZone: TimeZone
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
        timeZone: TimeZone = TimeZone.getDefault()
    ) {
        val gregorianCalendar = GregorianCalendar(timeZone)
        gregorianCalendar[year, month - 1, day, hour, minute] = second
        gregorianCalendar[Calendar.MILLISECOND] = millisecond
        millis = gregorianCalendar.timeInMillis
        this.timeZone = timeZone
    }

    @JvmOverloads
    constructor(timestamp: Long = currentTimeMillis(), timeZone: TimeZone = TimeZone.getDefault()) {
        this.millis = timestamp
        this.timeZone = timeZone
    }

    private constructor(calendar: Calendar) : this(calendar.timeInMillis, calendar.timeZone)

    fun startOfDay(): DateTime = DateTime(millis.startOfDay())

    fun startOfMinute(): DateTime = DateTime(millis.startOfMinute())

    fun startOfSecond(): DateTime = DateTime(millis.startOfSecond())

    fun endOfMinute(): DateTime = DateTime(millis.endOfMinute())

    fun noon(): DateTime = DateTime(millis.noon())

    fun endOfDay(): DateTime = DateTime(millis.endOfDay())

    fun withMillisOfDay(millisOfDay: Int): DateTime = DateTime(millis.withMillisOfDay(millisOfDay))

    val offset: Long
        get() = timeZone.getOffset(millis).toLong()

    val millisOfDay: Int
        get() = millis.millisOfDay

    val year: Int
        get() = calendar[Calendar.YEAR]

    val monthOfYear: Int
        get() = calendar[Calendar.MONTH] + 1

    val dayOfMonth: Int
        get() = calendar[Calendar.DATE]

    val dayOfWeek: Int
        get() = calendar[Calendar.DAY_OF_WEEK]

    val hourOfDay: Int
        get() = calendar[Calendar.HOUR_OF_DAY]

    val minuteOfHour: Int
        get() = calendar[Calendar.MINUTE]

    val secondOfMinute: Int
        get() = calendar[Calendar.SECOND]

    fun withYear(year: Int): DateTime {
        return with(Calendar.YEAR, year)
    }

    fun withMonthOfYear(monthOfYear: Int): DateTime {
        return with(Calendar.MONTH, monthOfYear - 1)
    }

    fun withDayOfMonth(dayOfMonth: Int): DateTime {
        return with(Calendar.DAY_OF_MONTH, dayOfMonth)
    }

    fun withHourOfDay(hourOfDay: Int): DateTime {
        return with(Calendar.HOUR_OF_DAY, hourOfDay)
    }

    fun withMinuteOfHour(minuteOfHour: Int): DateTime {
        return with(Calendar.MINUTE, minuteOfHour)
    }

    fun withSecondOfMinute(secondOfMinute: Int): DateTime {
        return with(Calendar.SECOND, secondOfMinute)
    }

    fun withMillisOfSecond(millisOfSecond: Int): DateTime {
        return with(Calendar.MILLISECOND, millisOfSecond)
    }

    fun plusMonths(interval: Int): DateTime {
        return add(Calendar.MONTH, interval)
    }

    fun plusWeeks(weeks: Int): DateTime {
        return add(Calendar.WEEK_OF_MONTH, weeks)
    }

    fun plusDays(interval: Int): DateTime {
        return add(Calendar.DATE, interval)
    }

    fun plusHours(hours: Int): DateTime {
        return add(Calendar.HOUR_OF_DAY, hours)
    }

    fun plusMinutes(minutes: Int): DateTime {
        return add(Calendar.MINUTE, minutes)
    }

    fun plusSeconds(seconds: Int): DateTime {
        return add(Calendar.SECOND, seconds)
    }

    fun plusMillis(millis: Int): DateTime {
        return add(Calendar.MILLISECOND, millis)
    }

    fun minusSeconds(seconds: Int): DateTime {
        return subtract(Calendar.SECOND, seconds)
    }

    fun minusDays(days: Int): DateTime = DateTime(millis.minusDays(days))

    fun minusMinutes(minutes: Int): DateTime = DateTime(millis.minusMinutes(minutes))

    fun minusMillis(millis: Long): DateTime = DateTime(this.millis.minusMillis(millis))

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
        return toTimeZone(TimeZone.getDefault())
    }

    val isLastDayOfMonth: Boolean
        get() = dayOfMonth == numberOfDaysInMonth

    val numberOfDaysInMonth: Int
        get() = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    private fun toTimeZone(timeZone: TimeZone): DateTime {
        if (timeZone === this.timeZone) {
            return this
        }
        val current = calendar
        val target: Calendar = GregorianCalendar(timeZone)
        target.timeInMillis = current.timeInMillis
        return DateTime(target)
    }

    private fun with(field: Int, value: Int): DateTime {
        val calendar = calendar
        calendar[field] = value
        return DateTime(calendar)
    }

    private fun subtract(field: Int, value: Int): DateTime {
        return add(field, -value)
    }

    private fun add(field: Int, value: Int): DateTime {
        val calendar = calendar
        calendar.add(field, value)
        return DateTime(calendar)
    }

    private val calendar: Calendar
        get() {
            val calendar: Calendar = GregorianCalendar(timeZone)
            calendar.timeInMillis = millis
            return calendar
        }

    fun toDateTime(): net.fortuna.ical4j.model.DateTime {
        return if (millis == 0L) throw IllegalStateException() else net.fortuna.ical4j.model.DateTime(millis)
    }

    fun toDate(): Date {
        return if (millis == 0L) throw IllegalStateException() else Date(millis + offset)
    }

    fun toLocalDate(): LocalDate? {
        return if (millis == 0L) null else LocalDate.of(year, monthOfYear, dayOfMonth)
    }

    fun toLocalDateTime(): LocalDateTime? {
        return if (millis == 0L) null else LocalDateTime.of(
            year,
            monthOfYear,
            dayOfMonth,
            hourOfDay,
            minuteOfHour
        )
    }

    fun toAppleEpoch(): Long {
        return millis.toAppleEpoch()
    }

    val dayOfWeekInMonth: Int
        get() = calendar[Calendar.DAY_OF_WEEK_IN_MONTH]

    val maxDayOfWeekInMonth: Int
        get() = calendar.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH)

    val weekDay: WeekDay
        get() {
            when (calendar[Calendar.DAY_OF_WEEK]) {
                Calendar.SUNDAY -> return WeekDay.SU
                Calendar.MONDAY -> return WeekDay.MO
                Calendar.TUESDAY -> return WeekDay.TU
                Calendar.WEDNESDAY -> return WeekDay.WE
                Calendar.THURSDAY -> return WeekDay.TH
                Calendar.FRIDAY -> return WeekDay.FR
                Calendar.SATURDAY -> return WeekDay.SA
            }
            throw RuntimeException()
        }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is DateTime) {
            return false
        }
        val dateTime = o
        return millis == dateTime.millis && timeZone == dateTime.timeZone
    }

    override fun hashCode(): Int {
        return Objects.hash(timeZone, millis)
    }

    fun toString(format: String?): String {
        val calendar = calendar
        val simpleDateFormat =
            SimpleDateFormat(format, Locale.getDefault())
        simpleDateFormat.calendar = calendar
        return simpleDateFormat.format(calendar.time)
    }

    override fun toString(): String {
        return toString("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    }

    companion object {
        val MAX_MILLIS_PER_DAY: Int = TimeUnit.DAYS.toMillis(1).toInt() - 1
        val UTC: TimeZone = TimeZone.getTimeZone("GMT")

        fun from(date: java.util.Date?): DateTime {
            if (date == null) {
                return DateTime(0)
            }
            val dateTime = DateTime(date.time)
            return dateTime.minusMillis(dateTime.offset)
        }

        fun from(date: Date?): DateTime {
            if (date is net.fortuna.ical4j.model.DateTime) {
                val dt = date
                val tz: TimeZone? = dt.timeZone
                return DateTime(
                    dt.time,
                    tz ?: if (dt.isUtc) UTC else TimeZone.getDefault()
                )
            } else {
                return from(date as java.util.Date?)
            }
        }
    }
}
