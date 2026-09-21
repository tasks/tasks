package org.tasks.time

import androidx.compose.runtime.Composable
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

private const val PREFERRED_HOUR_SKELETON = "j"

private val is24Hour: Boolean by lazy {
    val hourFormat = NSDateFormatter.dateFormatFromTemplate(PREFERRED_HOUR_SKELETON, 0u, NSLocale.currentLocale) ?: "h"
    !hourFormat.contains('a')
}

@Composable
actual fun is24HourFormat(): Boolean = is24Hour
