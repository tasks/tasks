package org.tasks.time

import org.tasks.date.DateTimeUtils.toDateTime

object DateTimeUtils {
    fun Long.toDate(): net.fortuna.ical4j.model.Date? = this.toDateTime().toDate()
}
