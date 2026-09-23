package org.tasks.kmp

import org.tasks.kmp.org.tasks.time.TextStyle
import org.tasks.kmp.org.tasks.time.templateFormatter
import org.tasks.kmp.org.tasks.time.timeFormatter
import org.tasks.kmp.org.tasks.time.toNSDate
import org.tasks.kmp.org.tasks.time.toWeekdayPattern

actual fun formatTime(timestamp: Long, is24HourFormat: Boolean): String =
    timeFormatter(is24HourFormat).stringFromDate(timestamp.toNSDate())

actual fun formatDayOfWeek(timestamp: Long, style: TextStyle, languageTag: String?): String =
    templateFormatter(style.toWeekdayPattern(), languageTag).stringFromDate(timestamp.toNSDate())
