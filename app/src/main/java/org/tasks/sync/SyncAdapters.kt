package org.tasks.sync

import com.todoroo.astrid.data.SyncFlags
import com.todoroo.astrid.data.Task
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskListDao
import org.tasks.jobs.WorkManager
import javax.inject.Inject

class SyncAdapters @Inject constructor(
        private val workManager: WorkManager,
        private val caldavDao: CaldavDao,
        private val googleTaskListDao: GoogleTaskListDao) {

    suspend fun sync(task: Task, original: Task?) {
        val suppress = task.checkTransitory(SyncFlags.SUPPRESS_SYNC)
        val forceCaldav = task.checkTransitory(SyncFlags.FORCE_CALDAV_SYNC)
        val pushGtasks = !suppress
                && !task.googleTaskUpToDate(original)
                && isGoogleTaskSyncEnabled()
        val pushCaldav = !suppress
                && (forceCaldav || !task.caldavUpToDate(original))
                && isCaldavSyncEnabled()
        if (pushGtasks || pushCaldav) {
            sync()
        }
    }

    suspend fun sync() {
        sync(false)
    }

    suspend fun sync(immediate: Boolean): Boolean = withContext(NonCancellable) {
        if (isSyncEnabled()) {
            workManager.sync(immediate)
            true
        } else {
            false
        }
    }

    suspend fun isSyncEnabled(): Boolean = isGoogleTaskSyncEnabled() || isCaldavSyncEnabled()

    suspend fun isGoogleTaskSyncEnabled(): Boolean = googleTaskListDao.getAccounts().isNotEmpty()

    suspend fun isCaldavSyncEnabled(): Boolean = caldavDao.accountCount() > 0
}