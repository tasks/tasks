package org.tasks.compose.pickers

import org.tasks.kmp.formatTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.withMillisOfDay

fun labelWithTime(label: String, time: Int, is24Hour: Boolean): String =
    if (time == NO_TIME) {
        label
    } else {
        "$label ${formatTime(currentTimeMillis().withMillisOfDay(time), is24Hour)}"
    }
