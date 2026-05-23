package org.tasks.kmp

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import co.touchlab.kermit.Logger
import okio.Path.Companion.toPath
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.TextStyle

expect fun formatNumber(number: Int): String

expect val PROD_ID: String

expect val DEV_URL: String

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler {
            Logger.e { "Preferences datastore corrupted, resetting to defaults" }
            emptyPreferences()
        },
        produceFile = { producePath().toPath() }
    )

const val dataStoreFileName = "tasks.preferences_pb"

expect fun formatDate(timestamp: Long, style: DateStyle): String

expect fun formatTime(timestamp: Long, is24HourFormat: Boolean): String

expect fun formatFullDateTime(
    timestamp: Long,
    is24HourFormat: Boolean,
    dateStyle: DateStyle,
): String

expect fun formatDayOfWeek(timestamp: Long, style: TextStyle): String
