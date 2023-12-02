package org.tasks.jobs

import android.net.Uri
import com.todoroo.astrid.data.Task
import org.tasks.BuildConfig
import org.tasks.data.CaldavAccount
import org.tasks.data.Place

interface WorkManager {

    fun updateCalendar(task: Task)

    fun migrateLocalTasks(caldavAccount: CaldavAccount)

    suspend fun sync(immediate: Boolean)

    suspend fun startEnqueuedSync()

    fun reverseGeocode(place: Place)

    fun updateBackgroundSync()

    fun scheduleRefresh(time: Long)

    fun scheduleMidnightRefresh()

    fun scheduleNotification(scheduledTime: Long)

    fun scheduleBackup()

    fun scheduleConfigRefresh()

    fun scheduleDriveUpload(uri: Uri, purge: Boolean)

    fun cancelNotifications()

    fun updatePurchases()

    companion object {
        val REMOTE_CONFIG_INTERVAL_HOURS = if (BuildConfig.DEBUG) 1 else 12.toLong()
        const val TAG_BACKUP = "tag_backup"
        const val TAG_REFRESH = "tag_refresh"
        const val TAG_MIDNIGHT_REFRESH = "tag_midnight_refresh"
        const val TAG_SYNC = "tag_sync"
        const val TAG_BACKGROUND_SYNC = "tag_background_sync"
        const val TAG_REMOTE_CONFIG = "tag_remote_config"
        const val TAG_MIGRATE_LOCAL = "tag_migrate_local"
        const val TAG_UPDATE_PURCHASES = "tag_update_purchases"
    }
}