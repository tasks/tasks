package org.tasks.kmp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.tasks.kmp.BuildConfig
import org.tasks.extensions.formatNumber
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.TextStyle
import org.tasks.kmp.org.tasks.time.formatFullDateTimeString
import org.tasks.kmp.org.tasks.time.formatTimeString
import org.tasks.kmp.org.tasks.time.toFormatStyle
import org.tasks.kmp.org.tasks.time.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

actual fun formatNumber(number: Int) = Locale.getDefault().formatNumber(number)

fun createDataStore(context: Context): DataStore<Preferences> = createDataStore(
    producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath }
)

actual val PROD_ID = "+//IDN tasks.org//android-${BuildConfig.VERSION_CODE}//EN"

actual val DEV_URL: String = BuildConfig.DEV_URL

private val dateFormatters = ConcurrentHashMap<String, DateTimeFormatter>()

actual fun formatDate(timestamp: Long, style: DateStyle): String {
    val locale = Locale.getDefault()
    val formatter = dateFormatters.getOrPut("$locale|$style") {
        DateTimeFormatter.ofLocalizedDate(style.toFormatStyle()).withLocale(locale)
    }
    return formatter.format(timestamp.toLocalDateTime().toLocalDate())
}

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
