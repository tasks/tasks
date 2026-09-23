package org.tasks.kmp

import org.tasks.kmp.org.tasks.time.TextStyle
import org.tasks.kmp.org.tasks.time.formatTimeString
import org.tasks.kmp.org.tasks.time.toJavaTextStyle
import org.tasks.kmp.org.tasks.time.toLocalDateTime
import org.tasks.extensions.toLocale

actual fun formatTime(timestamp: Long, is24HourFormat: Boolean): String =
    formatTimeString(timestamp.toLocalDateTime(), is24HourFormat)

actual fun formatDayOfWeek(timestamp: Long, style: TextStyle, languageTag: String?): String =
    timestamp
        .toLocalDateTime()
        .dayOfWeek
        .getDisplayName(style.toJavaTextStyle(), languageTag.toLocale())
