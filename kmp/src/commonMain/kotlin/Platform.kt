package org.tasks.kmp

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.TextStyle

expect fun formatNumber(number: Int): String

expect val IS_DEBUG: Boolean

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

const val dataStoreFileName = "tasks.preferences_pb"

expect fun formatDate(timestamp: Long, style: DateStyle): String

expect fun formatDateTime(timestamp: Long, style: DateStyle): String

expect fun formatDayOfWeek(timestamp: Long, style: TextStyle): String

expect fun formatDateTime(timestamp: Long, format: String): String
