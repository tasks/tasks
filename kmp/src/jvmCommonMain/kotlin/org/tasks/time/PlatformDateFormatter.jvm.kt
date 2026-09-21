package org.tasks.kmp.org.tasks.time

import java.time.format.DateTimeFormatter
import java.util.Locale

actual fun currentLocaleTag(): String = Locale.getDefault().toLanguageTag()

actual class PlatformDateFormatter actual constructor() {
    private val locale: Locale = Locale.getDefault()

    private val dateFormatters: Map<DateStyle, DateTimeFormatter> =
        DateStyle.entries.associateWith {
            DateTimeFormatter.ofLocalizedDate(it.toFormatStyle()).withLocale(locale)
        }

    actual fun date(timestamp: Long, style: DateStyle): String =
        dateFormatters.getValue(style).format(timestamp.toLocalDateTime().toLocalDate())

    actual fun time(timestamp: Long, is24HourFormat: Boolean): String =
        formatTimeString(timestamp.toLocalDateTime(), is24HourFormat)

    actual fun fullDateTime(
        timestamp: Long,
        is24HourFormat: Boolean,
        dateStyle: DateStyle,
    ): String =
        formatFullDateTimeString(
            timestamp.toLocalDateTime(),
            is24HourFormat,
            dateStyle.toFormatStyle(),
        )

    actual fun dayOfWeek(timestamp: Long, style: TextStyle): String =
        timestamp.toLocalDateTime().dayOfWeek.getDisplayName(style.toJavaTextStyle(), locale)
}

internal fun TextStyle.toJavaTextStyle(): java.time.format.TextStyle = when (this) {
    TextStyle.FULL -> java.time.format.TextStyle.FULL
    TextStyle.SHORT -> java.time.format.TextStyle.SHORT
    TextStyle.NARROW -> java.time.format.TextStyle.NARROW
}
