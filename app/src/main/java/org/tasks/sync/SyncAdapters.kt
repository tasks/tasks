package org.tasks.sync

import com.todoroo.astrid.data.SyncFlags
import com.todoroo.astrid.data.Task
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.tasks.data.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.CaldavAccount.Companion.TYPE_ETESYNC
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao
import org.tasks.data.GoogleTaskListDao
import org.tasks.data.OpenTaskDao
import org.tasks.jobs.WorkManager
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_CALDAV
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_ETESYNC
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_GOOGLE_TASKS
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncAdapters @Inject constructor(
        workManager: WorkManager,
        private val caldavDao: CaldavDao,
        private val googleTaskDao: GoogleTaskDao,
        private val googleTaskListDao: GoogleTaskListDao) {
    private val googleTasks = Debouncer(TAG_SYNC_GOOGLE_TASKS) { workManager.googleTaskSync(it) }
    private val caldav = Debouncer(TAG_SYNC_CALDAV) { workManager.caldavSync(it) }
    private val eteSync = Debouncer(TAG_SYNC_ETESYNC) { workManager.eteSync(it) }

    suspend fun sync(task: Task, original: Task?) {
        if (task.checkTransitory(SyncFlags.SUPPRESS_SYNC)) {
            return
        }
        if (!task.googleTaskUpToDate(original)
                && googleTaskDao.getAllByTaskId(task.id).isNotEmpty()) {
            googleTasks.sync(false)
        }
        if (task.checkTransitory(SyncFlags.FORCE_CALDAV_SYNC) || !task.caldavUpToDate(original)) {
            if (caldavDao.isAccountType(task.id, TYPE_CALDAV)) {
                caldav.sync(false)
            }
            if (caldavDao.isAccountType(task.id, TYPE_ETESYNC)) {
                eteSync.sync(false)
            }
        }
    }

    suspend fun sync() {
        sync(false)
    }

    suspend fun sync(immediate: Boolean): Boolean = withContext(NonCancellable) {
        val googleTasksEnabled = isGoogleTaskSyncEnabled()
        if (googleTasksEnabled) {
            googleTasks.sync(immediate)
        }

        val caldavEnabled = isCaldavSyncEnabled()
        if (caldavEnabled) {
            caldav.sync(immediate)
        }

        val eteSyncEnabled = isEteSyncEnabled()
        if (eteSyncEnabled) {
            eteSync.sync(immediate)
        }

        return@withContext googleTasksEnabled || caldavEnabled || eteSyncEnabled
    }

    suspend fun isGoogleTaskSyncEnabled() = googleTaskListDao.getAccounts().isNotEmpty()

    suspend fun isCaldavSyncEnabled() = caldavDao.getAccounts(TYPE_CALDAV).isNotEmpty()

    suspend fun isEteSyncEnabled() = caldavDao.getAccounts(TYPE_ETESYNC).isNotEmpty()
}