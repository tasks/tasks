package org.tasks.kmp

import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.TextStyle
import org.tasks.kmp.org.tasks.time.formatFullDateTimeString
import org.tasks.kmp.org.tasks.time.formatTimeString
import org.tasks.kmp.org.tasks.time.toFormatStyle
import org.tasks.kmp.org.tasks.time.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

actual fun formatNumber(number: Int): String =
    java.text.NumberFormat.getIntegerInstance(Locale.getDefault()).format(number)

actual val PROD_ID = "+//IDN tasks.org//desktop-${JvmBuildConfig.VERSION_CODE}//EN"

actual val DEV_URL: String = JvmBuildConfig.DEV_URL

actual fun formatDate(timestamp: Long, style: DateStyle): String =
    DateTimeFormatter
        .ofLocalizedDate(style.toFormatStyle())
        .withLocale(Locale.getDefault())
        .format(timestamp.toLocalDateTime().toLocalDate())

actual fun formatTime(timestamp: Long, is24HourFormat: Boolean): String =
    formatTimeString(timestamp.toLocalDateTime(), is24HourFormat)

actual fun formatFullDateTime(
    timestamp: Long,
    is24HourFormat: Boolean,
    dateStyle: DateStyle,
): String =
    formatFullDateTimeString(
        timestamp.toLocalDateTime(),
        is24HourFormat,
        dateStyle.toFormatStyle(),
    )

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
