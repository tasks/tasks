package org.tasks.date

import org.tasks.time.DateTime
import org.tasks.time.DateTime.Companion.UTC
import java.util.TimeZone

object DateTimeUtils {
    @JvmStatic
    fun newDate(year: Int, month: Int, day: Int): DateTime = DateTime(year, month, day, 0, 0, 0)

    @JvmStatic
    fun newDateUtc(
            year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): DateTime =
            DateTime(year, month, day, hour, minute, second, 0, TimeZone.getTimeZone("GMT"))

    @JvmStatic
    fun newDateTime(): DateTime = DateTime()

    @JvmStatic
    fun midnight(): Long = newDateTime().plusDays(1).startOfDay().millis

    @JvmStatic
    fun newDateTime(timestamp: Long): DateTime = DateTime(timestamp)

    @JvmStatic
    fun newDateTime(timestamp: Long, timeZone: TimeZone): DateTime = DateTime(timestamp, timeZone)

    fun Long.toDateTime(): DateTime = DateTime(this)

    fun Long.isAfterNow(): Boolean = DateTime(this).isAfterNow

    fun Long.toLocal(): Long = DateTime(this, UTC).toLocal().millis
}