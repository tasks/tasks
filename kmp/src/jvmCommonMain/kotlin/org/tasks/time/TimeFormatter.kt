package org.tasks.kmp.org.tasks.time

import java.time.LocalDateTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.temporal.TemporalAccessor
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

// Languages where AM/PM is conventionally placed before the time
private val AM_PM_BEFORE_TIME = setOf("zh", "ja", "ko", "vi", "as", "brx", "ee", "ta", "yue")

private val APPENDED_FIELD = Regex("[\\u251C\\u2524]|\\([^)]*:\\s")

private val PROBE = LocalDateTime.of(2026, 3, 9, 13, 47, 5)

private class TimeFormat(val pattern: String, val formatter: DateTimeFormatter)

private val timeFormats = ConcurrentHashMap<Triple<Locale, Boolean, Boolean>, TimeFormat>()

private val dateTimePatterns = ConcurrentHashMap<Triple<Locale, FormatStyle, String>, String>()

internal fun fallbackTimePattern(locale: Locale): String =
    if (locale.language in AM_PM_BEFORE_TIME) "ah:mm" else "h:mm a"

internal fun sanitizeTimePattern(pattern: String, locale: Locale): String =
    if (APPENDED_FIELD.containsMatchIn(pattern)) fallbackTimePattern(locale) else pattern

private fun formatter(
    pattern: String,
    locale: Locale,
    probe: TemporalAccessor,
): DateTimeFormatter? = try {
    DateTimeFormatter.ofPattern(pattern, locale).also { it.format(probe) }
} catch (e: Exception) {
    null
}

internal fun validTimePattern(pattern: String, locale: Locale): String =
    if (formatter(pattern, locale, PROBE.toLocalTime()) != null) {
        pattern
    } else {
        fallbackTimePattern(locale)
    }

internal fun adjustTimePattern(
    pattern: String,
    locale: Locale,
    is24HourFormat: Boolean,
    onTheHour: Boolean,
): String {
    var adjusted = pattern
    if (is24HourFormat && !adjusted.contains("H")) {
        adjusted = adjusted
            .replace("hh", "HH").replace("h", "HH")
            .replace(Regex("[\\s\\u202F]*a[\\s\\u202F]*"), "")
            .trim()
    } else if (!is24HourFormat && adjusted.contains("H")) {
        adjusted = adjusted.replace("HH", "h").replace("H", "h")
        adjusted = if (locale.language in AM_PM_BEFORE_TIME) {
            "a$adjusted"
        } else {
            "$adjusted a"
        }
    }
    if (!is24HourFormat && onTheHour) {
        adjusted = adjusted.replace(Regex("[:.]mm"), "")
    }
    return adjusted
}

private fun rawTimePattern(locale: Locale): String = DateTimeFormatterBuilder
    .getLocalizedDateTimePattern(null, FormatStyle.SHORT, IsoChronology.INSTANCE, locale)

internal fun resolveTimePattern(locale: Locale, raw: () -> String): String = try {
    validTimePattern(sanitizeTimePattern(raw(), locale), locale)
} catch (e: Exception) {
    fallbackTimePattern(locale)
}

private fun localizedTimePattern(locale: Locale): String =
    resolveTimePattern(locale) { rawTimePattern(locale) }

fun timePatternDiagnostics(): String = try {
    val locale = Locale.getDefault()
    val raw = rawTimePattern(locale)
    val resolved = localizedTimePattern(locale)
    if (resolved == raw) raw else "$raw -> $resolved"
} catch (e: Exception) {
    e.toString()
}

private fun timeFormat(
    locale: Locale,
    is24HourFormat: Boolean,
    onTheHour: Boolean,
): TimeFormat = timeFormats.getOrPut(Triple(locale, is24HourFormat, onTheHour)) {
    val pattern =
        adjustTimePattern(localizedTimePattern(locale), locale, is24HourFormat, onTheHour)
    TimeFormat(pattern, DateTimeFormatter.ofPattern(pattern, locale))
}

fun formatTimeString(dateTime: LocalDateTime, is24HourFormat: Boolean): String {
    val time = dateTime.toLocalTime()
    val onTheHour = !is24HourFormat && time.minute == 0
    return time.format(timeFormat(Locale.getDefault(), is24HourFormat, onTheHour).formatter)
}

private fun rawDateTimePattern(dateStyle: FormatStyle, locale: Locale): String =
    DateTimeFormatterBuilder.getLocalizedDateTimePattern(
        dateStyle,
        FormatStyle.SHORT,
        IsoChronology.INSTANCE,
        locale,
    )

internal fun fullDateTimePattern(
    dateTimePattern: String,
    rawTime: String,
    timePattern: String,
): String? = dateTimePattern
    .takeIf { rawTime.isNotBlank() && it.contains(rawTime) }
    ?.replace(rawTime, timePattern)

private fun localizedDateTimePattern(
    locale: Locale,
    dateStyle: FormatStyle,
    timePattern: String,
): String = dateTimePatterns.getOrPut(Triple(locale, dateStyle, timePattern)) {
    try {
        fullDateTimePattern(
            dateTimePattern = rawDateTimePattern(dateStyle, locale),
            rawTime = rawTimePattern(locale),
            timePattern = timePattern,
        )
            ?.takeIf { formatter(it, locale, PROBE) != null }
            .orEmpty()
    } catch (e: Exception) {
        ""
    }
}

fun formatFullDateTimeString(
    dateTime: LocalDateTime,
    is24HourFormat: Boolean,
    dateStyle: FormatStyle,
): String {
    val locale = Locale.getDefault()
    val onTheHour = !is24HourFormat && dateTime.minute == 0
    val time = timeFormat(locale, is24HourFormat, onTheHour)
    val fullPattern = localizedDateTimePattern(locale, dateStyle, time.pattern)
    if (fullPattern.isNotEmpty()) {
        return dateTime.format(DateTimeFormatter.ofPattern(fullPattern, locale))
    }
    val date = DateTimeFormatter.ofLocalizedDate(dateStyle).withLocale(locale).format(dateTime)
    return "$date ${dateTime.format(time.formatter)}"
}
