package org.tasks.analytics

import androidx.datastore.preferences.core.longPreferencesKey
import org.tasks.preferences.TasksPreferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay

interface Analytics {
    val tasksPreferences: TasksPreferences

    fun logEvent(event: String, vararg params: Pair<String, Any>)
    fun addTask(source: String)
    fun completeTask(source: String)
    fun identify(distinctId: String)

    suspend fun logEventOncePerDay(event: String, vararg params: Pair<String, Any>) {
        val prefKey = longPreferencesKey("last_logged_$event")
        val today = currentTimeMillis().startOfDay()
        if (tasksPreferences.get(prefKey, 0L) < today) {
            tasksPreferences.set(prefKey, today)
            logEvent(event, *params)
        }
    }
}
