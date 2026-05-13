package org.tasks.kmp.org.tasks.time

import java.time.LocalDateTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

// Languages where AM/PM is conventionally placed before the time
private val AM_PM_BEFORE_TIME = setOf("zh", "ja", "ko", "vi", "as", "brx", "ee", "ta", "yue")

fun formatTimeString(dateTime: LocalDateTime, is24HourFormat: Boolean): String {
    val locale = Locale.getDefault()
    var pattern = DateTimeFormatterBuilder
        .getLocalizedDateTimePattern(null, FormatStyle.SHORT, IsoChronology.INSTANCE, locale)
    if (is24HourFormat && !pattern.contains("H")) {
        pattern = pattern
            .replace("hh", "HH").replace("h", "HH")
            .replace(Regex("[\\s\\u202F]*a[\\s\\u202F]*"), "")
            .trim()
    } else if (!is24HourFormat && pattern.contains("H")) {
        pattern = pattern.replace("HH", "h").replace("H", "h")
        pattern = if (locale.language in AM_PM_BEFORE_TIME) {
            "a$pattern"
        } else {
            "$pattern a"
        }
    }
    if (!is24HourFormat && dateTime.minute == 0) {
        pattern = pattern.replace(Regex("[:.]mm"), "")
    }
    return dateTime.toLocalTime()
        .format(DateTimeFormatter.ofPattern(pattern, locale))
}

fun formatFullDateTimeString(
    dateTime: LocalDateTime,
    is24HourFormat: Boolean,
    dateStyle: FormatStyle,
): String {
    val locale = Locale.getDefault()
    val fullFormatted = DateTimeFormatter
        .ofLocalizedDateTime(dateStyle, FormatStyle.SHORT)
        .withLocale(locale)
        .format(dateTime)
    val localeTime = dateTime.toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    val correctTime = formatTimeString(dateTime, is24HourFormat)
    return fullFormatted.replace(localeTime, correctTime)
}
