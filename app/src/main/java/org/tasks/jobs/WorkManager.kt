package org.tasks.jobs

import android.net.Uri
import com.todoroo.astrid.data.Task
import org.tasks.BuildConfig
import org.tasks.data.Place

interface WorkManager {

    fun afterSave(current: Task, original: Task?)

    fun cleanup(ids: Iterable<Long>)

    fun sync(immediate: Boolean)

    fun reverseGeocode(place: Place)

    fun updateBackgroundSync()

    suspend fun updateBackgroundSync(
            forceAccountPresent: Boolean?,
            forceBackgroundEnabled: Boolean?,
            forceOnlyOnUnmetered: Boolean?)

    fun scheduleRefresh(time: Long)

    fun scheduleMidnightRefresh()

    fun scheduleNotification(scheduledTime: Long)

    fun scheduleBackup()

    fun scheduleConfigRefresh()

    fun scheduleDriveUpload(uri: Uri, purge: Boolean)

    fun cancelNotifications()

    companion object {
        val REMOTE_CONFIG_INTERVAL_HOURS = if (BuildConfig.DEBUG) 1 else 12.toLong()
        const val MAX_CLEANUP_LENGTH = 500
        const val TAG_BACKUP = "tag_backup"
        const val TAG_REFRESH = "tag_refresh"
        const val TAG_MIDNIGHT_REFRESH = "tag_midnight_refresh"
        const val TAG_SYNC = "tag_sync"
        const val TAG_BACKGROUND_SYNC = "tag_background_sync"
        const val TAG_REMOTE_CONFIG = "tag_remote_config"
    }
}