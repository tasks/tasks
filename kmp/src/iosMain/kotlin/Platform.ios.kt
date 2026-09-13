package org.tasks.kmp

import org.tasks.TasksBuildConfig
import platform.Foundation.NSLocale
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
