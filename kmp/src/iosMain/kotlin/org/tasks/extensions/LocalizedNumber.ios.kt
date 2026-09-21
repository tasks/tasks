package org.tasks.extensions

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.currentLocale

actual fun localizedNumber(number: Int): String =
    NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        locale = NSLocale.currentLocale
        usesGroupingSeparator = false
    }.stringFromNumber(NSNumber(int = number)) ?: number.toString()
