package org.tasks.kmp

import org.tasks.TasksBuildConfig
import kotlinx.datetime.DayOfWeek
import platform.Foundation.NSCalendar
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleIdentifier
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

import platform.Foundation.currentLocale
import platform.UIKit.UIDevice

actual fun formatNumber(number: Int): String =
    NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        locale = NSLocale.currentLocale
    }.stringFromNumber(NSNumber(int = number)) ?: number.toString()

actual val PROD_ID = "+//IDN tasks.org//ios-${TasksBuildConfig.VERSION_CODE}//EN"

actual val DEV_URL: String = IosBuildConfig.DEV_URL

actual fun osDescription(): String =
    UIDevice.currentDevice.let { "${it.systemName} ${it.systemVersion} (${it.model})" }

actual fun languageDisplayName(languageTag: String): String? =
    languageTag
        .takeIf { it.isNotBlank() }
        ?.let { NSLocale(localeIdentifier = it).displayNameForKey(NSLocaleIdentifier, value = it) }
        ?.takeIf { it.isNotBlank() }

actual fun firstDayOfWeek(): DayOfWeek =
    DayOfWeek(((NSCalendar.currentCalendar().firstWeekday.toInt() + 5) % 7) + 1)

actual fun parseLocalizedInteger(text: String?): Int? =
    text?.let {
        NSNumberFormatter().apply {
            numberStyle = NSNumberFormatterDecimalStyle
            locale = NSLocale.currentLocale
        }.numberFromString(it)?.intValue
    }
