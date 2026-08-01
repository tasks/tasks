package org.tasks.kmp.org.tasks.time

import java.time.LocalDateTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

// Languages where AM/PM is conventionally placed before the time
private val AM_PM_BEFORE_TIME = setOf("zh", "ja", "ko", "vi", "as", "brx", "ee", "ta", "yue")

private val AM_PM_MARKER = Regex("[\\s\\u202F]*a[\\s\\u202F]*")
private val MINUTES = Regex("[:.]mm")

private val timeFormatters = ConcurrentHashMap<String, DateTimeFormatter>()

private fun timeFormatter(
    locale: Locale,
    is24HourFormat: Boolean,
    omitMinutes: Boolean,
): DateTimeFormatter =
    // Building the pattern and compiling a DateTimeFormatter costs more than the formatting
    // itself, and the task list formats a time for every visible row. The result depends only on
    // the locale and the two booleans, so formatters are cached.
    timeFormatters.getOrPut("$locale|$is24HourFormat|$omitMinutes") {
    var pattern = DateTimeFormatterBuilder
        .getLocalizedDateTimePattern(null, FormatStyle.SHORT, IsoChronology.INSTANCE, locale)
    if (is24HourFormat && !pattern.contains("H")) {
        pattern = pattern
            .replace("hh", "HH").replace("h", "HH")
            .replace(AM_PM_MARKER, "")
            .trim()
    } else if (!is24HourFormat && pattern.contains("H")) {
        pattern = pattern.replace("HH", "h").replace("H", "h")
        pattern = if (locale.language in AM_PM_BEFORE_TIME) {
            "a$pattern"
        } else {
            "$pattern a"
        }
    }
    if (omitMinutes) {
        pattern = pattern.replace(MINUTES, "")
    }
    DateTimeFormatter.ofPattern(pattern, locale)
}

fun formatTimeString(dateTime: LocalDateTime, is24HourFormat: Boolean): String {
    val locale = Locale.getDefault()
    val omitMinutes = !is24HourFormat && dateTime.minute == 0
    return dateTime.toLocalTime().format(timeFormatter(locale, is24HourFormat, omitMinutes))
}

private val localizedFormatters = ConcurrentHashMap<String, DateTimeFormatter>()

private fun localizedDateTimeFormatter(locale: Locale, dateStyle: FormatStyle): DateTimeFormatter =
    localizedFormatters.getOrPut("datetime|$locale|$dateStyle") {
        DateTimeFormatter.ofLocalizedDateTime(dateStyle, FormatStyle.SHORT).withLocale(locale)
    }

private fun localizedTimeFormatter(locale: Locale): DateTimeFormatter =
    localizedFormatters.getOrPut("time|$locale") {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }

fun formatFullDateTimeString(
    dateTime: LocalDateTime,
    is24HourFormat: Boolean,
    dateStyle: FormatStyle,
): String {
    val locale = Locale.getDefault()
    val fullFormatted = localizedDateTimeFormatter(locale, dateStyle).format(dateTime)
    val localeTime = dateTime.toLocalTime().format(localizedTimeFormatter(locale))
    val correctTime = formatTimeString(dateTime, is24HourFormat)
    return fullFormatted.replace(localeTime, correctTime)
}
