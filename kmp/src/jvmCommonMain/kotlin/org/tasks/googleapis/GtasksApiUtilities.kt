package org.tasks.googleapis

import co.touchlab.kermit.Logger
import com.google.api.client.util.DateTime
import java.util.Date
import java.util.TimeZone

object GtasksApiUtilities {

    fun unixTimeToGtasksCompletionTime(time: Long): DateTime? {
        if (time < 0) {
            return null
        }
        return DateTime(Date(time), TimeZone.getDefault())
    }

    fun gtasksCompletedTimeToUnixTime(gtasksCompletedTime: DateTime?): Long {
        return gtasksCompletedTime?.value ?: 0
    }

    /**
     * Google deals only in dates for due times, so on the server side they normalize to utc time and
     * then truncate h:m:s to 0. This can lead to a loss of date information for us, so we adjust here
     * by doing the normalizing/truncating ourselves and then correcting the date we get back in a
     * similar way.
     */
    @Suppress("DEPRECATION")
    fun unixTimeToGtasksDueDate(time: Long): DateTime? {
        if (time < 0) {
            return null
        }
        val date = Date(time / 1000 * 1000)
        date.hours = 0
        date.minutes = 0
        date.seconds = 0
        date.time = date.time - date.timezoneOffset * 60000
        return DateTime(date, TimeZone.getTimeZone("GMT"))
    }

    @Suppress("DEPRECATION")
    fun gtasksDueTimeToUnixTime(gtasksDueTime: DateTime?): Long {
        if (gtasksDueTime == null) {
            return 0
        }
        return try {
            val utcTime = gtasksDueTime.value
            val date = Date(utcTime)
            val returnDate = Date(date.time + date.timezoneOffset * 60000)
            returnDate.time
        } catch (e: NumberFormatException) {
            Logger.e("GtasksApiUtilities", e) { e.message.orEmpty() }
            0
        }
    }
}
