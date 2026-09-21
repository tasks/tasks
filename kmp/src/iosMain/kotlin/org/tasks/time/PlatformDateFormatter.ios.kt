package org.tasks.kmp.org.tasks.time

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterFullStyle
import platform.Foundation.NSDateFormatterLongStyle
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSDateFormatterStyle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localeIdentifier
import platform.Foundation.preferredLanguages

actual fun currentLocaleTag(): String =
    NSLocale.preferredLanguages.firstOrNull() as? String ?: NSLocale.currentLocale.localeIdentifier

internal fun Long.toNSDate(): NSDate = NSDate.dateWithTimeIntervalSince1970(this / 1000.0)

internal fun DateStyle.toNSStyle(): NSDateFormatterStyle = when (this) {
    DateStyle.FULL -> NSDateFormatterFullStyle
    DateStyle.LONG -> NSDateFormatterLongStyle
    DateStyle.MEDIUM -> NSDateFormatterMediumStyle
    DateStyle.SHORT -> NSDateFormatterShortStyle
}

internal fun TextStyle.toWeekdayPattern(): String = when (this) {
    TextStyle.FULL -> "EEEE"
    TextStyle.SHORT -> "EEE"
    TextStyle.NARROW -> "EEEEE"
}

internal fun templateFormatter(template: String): NSDateFormatter =
    NSDateFormatter().apply {
        locale = NSLocale.currentLocale
        setLocalizedDateFormatFromTemplate(template)
    }

internal fun timeFormatter(is24HourFormat: Boolean): NSDateFormatter =
    templateFormatter(if (is24HourFormat) "HH:mm" else "h:mm a")

actual class PlatformDateFormatter actual constructor() {
    private val dateFormatters: Map<DateStyle, NSDateFormatter> =
        DateStyle.entries.associateWith { style ->
            NSDateFormatter().apply {
                locale = NSLocale.currentLocale
                dateStyle = style.toNSStyle()
                timeStyle = NSDateFormatterNoStyle
            }
        }

    actual fun date(timestamp: Long, style: DateStyle): String =
        dateFormatters.getValue(style).stringFromDate(timestamp.toNSDate())

    actual fun time(timestamp: Long, is24HourFormat: Boolean): String =
        timeFormatter(is24HourFormat).stringFromDate(timestamp.toNSDate())

    actual fun fullDateTime(
        timestamp: Long,
        is24HourFormat: Boolean,
        dateStyle: DateStyle,
    ): String =
        NSDateFormatter().apply {
            locale = NSLocale.currentLocale
            this.dateStyle = dateStyle.toNSStyle()
            timeStyle = NSDateFormatterShortStyle
        }.stringFromDate(timestamp.toNSDate())

    actual fun dayOfWeek(timestamp: Long, style: TextStyle): String =
        templateFormatter(style.toWeekdayPattern()).stringFromDate(timestamp.toNSDate())
}
