package org.tasks.time

import android.annotation.SuppressLint
import org.tasks.BuildConfig
import org.tasks.date.DateTimeUtils.toDateTime

object DateTimeUtils {
    @SuppressLint("DefaultLocale")
    fun printDuration(millis: Long): String = if (BuildConfig.DEBUG) {
        val seconds = millis / 1000
        String.format(
                "%dh %dm %ds", seconds / 3600L, (seconds % 3600L / 60L).toInt(), (seconds % 60L).toInt())
    } else millis.toString()

    fun Long.toDate(): net.fortuna.ical4j.model.Date? = this.toDateTime().toDate()
}
