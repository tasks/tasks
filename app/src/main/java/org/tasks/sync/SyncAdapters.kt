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
import org.tasks.jobs.WorkManager
import javax.inject.Inject

class SyncAdapters @Inject constructor(
        private val workManager: WorkManager,
        private val caldavDao: CaldavDao,
        private val googleTaskDao: GoogleTaskDao,
        private val googleTaskListDao: GoogleTaskListDao) {

    suspend fun sync(task: Task, original: Task?) {
        if (task.checkTransitory(SyncFlags.SUPPRESS_SYNC)) {
            return
        }
        if (!task.googleTaskUpToDate(original)
                && googleTaskDao.getAllByTaskId(task.id).isNotEmpty()) {
            workManager.googleTaskSync(false)
        }
        if (task.checkTransitory(SyncFlags.FORCE_CALDAV_SYNC) || !task.caldavUpToDate(original)) {
            if (caldavDao.isAccountType(task.id, TYPE_CALDAV)) {
                workManager.caldavSync(false)
            }
            if (caldavDao.isAccountType(task.id, TYPE_ETESYNC)) {
                workManager.eteSync(false)
            }
        }
    }

    suspend fun sync() {
        sync(false)
    }

    suspend fun sync(immediate: Boolean): Boolean = withContext(NonCancellable) {
        val googleTasks = isGoogleTaskSyncEnabled()
        if (googleTasks) {
            workManager.googleTaskSync(immediate)
        }

        val caldav = isCaldavSyncEnabled()
        if (caldav) {
            workManager.caldavSync(immediate)
        }

        val eteSync = isEteSyncEnabled()
        if (eteSync) {
            workManager.eteSync(immediate)
        }

        return@withContext googleTasks || caldav || eteSync
    }

    suspend fun isGoogleTaskSyncEnabled() = googleTaskListDao.getAccounts().isNotEmpty()

    suspend fun isCaldavSyncEnabled() = caldavDao.getAccounts(TYPE_CALDAV).isNotEmpty()

    suspend fun isEteSyncEnabled() = caldavDao.getAccounts(TYPE_ETESYNC).isNotEmpty()
}