package org.tasks.jobs

import android.net.Uri
import com.todoroo.astrid.data.Task
import org.tasks.BuildConfig
import org.tasks.data.CaldavAccount
import org.tasks.data.Place

interface WorkManager {

    fun scheduleRepeat(task: Task)

    fun updateCalendar(task: Task)

    fun cleanup(ids: Iterable<Long>)

    fun migrateLocalTasks(caldavAccount: CaldavAccount)

    suspend fun googleTaskSync(immediate: Boolean)

    suspend fun caldavSync(immediate: Boolean)

    @Deprecated("use etebase")
    suspend fun eteSync(immediate: Boolean)

    suspend fun eteBaseSync(immediate: Boolean)

    suspend fun openTaskSync(immediate: Boolean)

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
        const val MAX_CLEANUP_LENGTH = 500
        const val TAG_BACKUP = "tag_backup"
        const val TAG_REFRESH = "tag_refresh"
        const val TAG_MIDNIGHT_REFRESH = "tag_midnight_refresh"
        const val TAG_SYNC_GOOGLE_TASKS = "tag_sync_google_tasks"
        const val TAG_SYNC_CALDAV = "tag_sync_caldav"
        @Deprecated("use etebase") const val TAG_SYNC_ETESYNC = "tag_sync_etesync"
        const val TAG_SYNC_ETEBASE = "tag_sync_etebase"
        const val TAG_SYNC_OPENTASK = "tag_sync_opentask"
        const val TAG_BACKGROUND_SYNC_GOOGLE_TASKS = "tag_background_sync_google_tasks"
        const val TAG_BACKGROUND_SYNC_CALDAV = "tag_background_sync_caldav"
        const val TAG_BACKGROUND_SYNC_ETEBASE = "tag_background_sync_etebase"
        const val TAG_BACKGROUND_SYNC_OPENTASKS = "tag_background_sync_opentasks"
        const val TAG_REMOTE_CONFIG = "tag_remote_config"
        const val TAG_MIGRATE_LOCAL = "tag_migrate_local"
        const val TAG_UPDATE_PURCHASES = "tag_update_purchases"
    }
}