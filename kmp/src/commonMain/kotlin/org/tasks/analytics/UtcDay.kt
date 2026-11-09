package org.tasks.analytics

import androidx.datastore.preferences.core.Preferences
import org.tasks.preferences.TasksPreferences
import org.tasks.time.ONE_DAY
import org.tasks.time.startOfDayUtc

private const val UTC_DAY_PREFIX = "last_utc_day_"

internal fun utcDayKey(name: String): String = "$UTC_DAY_PREFIX$name"

internal fun utcDayKey(event: String, dedupeBy: String): String =
    if (dedupeBy == event) utcDayKey(event) else utcDayKey("$event:$dedupeBy")

private const val FUTURE_DAY_TOLERANCE = ONE_DAY

private fun isNewUtcDay(stored: Long, today: Long): Boolean =
    stored < today || stored > today + FUTURE_DAY_TOLERANCE

suspend fun TasksPreferences.setIfNewUtcDay(
    key: Preferences.Key<Long>,
    instant: Long,
): Boolean {
    val today = instant.startOfDayUtc()
    return setLongIf(key, today) { stored -> isNewUtcDay(stored = stored ?: 0L, today = today) }
}
