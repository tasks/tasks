package org.tasks.analytics

import androidx.datastore.preferences.core.longPreferencesKey
import co.touchlab.kermit.Logger
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.tasks.preferences.TasksPreferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis

interface Analytics {
    val tasksPreferences: TasksPreferences

    val analyticsEnabled: Boolean

    fun logEvent(event: String, vararg params: Pair<String, Any>)
    fun addTask(source: String)
    fun completeTask(source: String)
    fun setSku(sku: String?)

    suspend fun logEventOncePerDay(event: String, vararg params: Pair<String, Any>) =
        logEventOncePerDay(event, dedupeBy = event, params = params)

    suspend fun logEventOncePerDay(
        event: String,
        dedupeBy: String,
        vararg params: Pair<String, Any>,
    ) {
        if (!analyticsEnabled) {
            return
        }
        val prefKey = longPreferencesKey(utcDayKey(event, dedupeBy))
        val claimed = withContext(NonCancellable) {
            try {
                tasksPreferences.setIfNewUtcDay(prefKey, currentTimeMillis())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                recordFailure(e, "Failed to claim $event")
                false
            }
        }
        if (!claimed) {
            return
        }
        try {
            logEvent(event, *params)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordFailure(e, "Failed to log $event")
        }
    }
}

private const val TAG = "Analytics"

private fun Analytics.recordFailure(e: Exception, message: String) {
    Logger.e(e, tag = TAG) { message }
    try {
        (this as? CrashReporting)?.reportException(e, fatal = false)
    } catch (_: Exception) {
        Logger.e(tag = TAG) { "Crash reporting threw while reporting a failed event" }
    }
}
