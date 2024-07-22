package org.tasks.kmp

import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.TextStyle
import org.tasks.kmp.org.tasks.time.toFormatStyle
import org.tasks.kmp.org.tasks.time.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

actual fun formatNumber(number: Int) = number.toString()

actual val IS_DEBUG = true

actual fun formatDate(timestamp: Long, style: DateStyle): String =
    DateTimeFormatter
        .ofLocalizedDate(style.toFormatStyle())
        .withLocale(Locale.getDefault())
        .format(timestamp.toLocalDateTime().toLocalDate())

actual fun formatDateTime(timestamp: Long, style: DateStyle): String =
    DateTimeFormatter
        .ofLocalizedDateTime(style.toFormatStyle(), FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .format(timestamp.toLocalDateTime())

actual fun formatDayOfWeek(timestamp: Long, style: TextStyle): String =
    timestamp
        .toLocalDateTime()
        .dayOfWeek
        .getDisplayName(
            when (style) {
                TextStyle.FULL -> java.time.format.TextStyle.FULL
                TextStyle.SHORT -> java.time.format.TextStyle.SHORT
                TextStyle.NARROW -> java.time.format.TextStyle.NARROW
            },
            Locale.getDefault()
        )

actual fun formatDateTime(timestamp: Long, format: String): String =
    timestamp.toLocalDateTime().format(DateTimeFormatter.ofPattern(format))
