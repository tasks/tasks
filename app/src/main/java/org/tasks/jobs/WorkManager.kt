package org.tasks.jobs

import android.net.Uri
import org.tasks.data.entity.Task
import org.tasks.BuildConfig
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.Place
import org.tasks.time.DateTimeUtils2.currentTimeMillis

interface WorkManager {

    fun updateCalendar(task: Task)

    fun migrateLocalTasks(caldavAccount: CaldavAccount)

    suspend fun sync(immediate: Boolean)

    suspend fun startEnqueuedSync()

    fun reverseGeocode(place: Place)

    fun updateBackgroundSync()

    suspend fun scheduleRefresh(timestamp: Long = currentTimeMillis() + 5_000)

    fun triggerNotifications(expedited: Boolean = false)

    fun scheduleNotification(scheduledTime: Long)

    fun scheduleBackup()

    fun scheduleConfigRefresh()

    fun scheduleDriveUpload(uri: Uri, purge: Boolean)

    fun updatePurchases()

    companion object {
        val REMOTE_CONFIG_INTERVAL_HOURS = if (BuildConfig.DEBUG) 1 else 12.toLong()
        const val TAG_BACKUP = "tag_backup"
        const val TAG_REFRESH = "tag_refresh"
        const val TAG_SYNC = "tag_sync"
        const val TAG_BACKGROUND_SYNC = "tag_background_sync"
        const val TAG_REMOTE_CONFIG = "tag_remote_config"
        const val TAG_MIGRATE_LOCAL = "tag_migrate_local"
        const val TAG_UPDATE_PURCHASES = "tag_update_purchases"
        const val TAG_NOTIFICATIONS = "tag_notifications"
    }
}