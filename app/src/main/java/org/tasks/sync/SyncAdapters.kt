package org.tasks.sync

import com.todoroo.astrid.data.SyncFlags
import com.todoroo.astrid.data.Task
import kotlinx.coroutines.*
import org.tasks.data.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.CaldavAccount.Companion.TYPE_ETESYNC
import org.tasks.data.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao
import org.tasks.data.GoogleTaskListDao
import org.tasks.jobs.WorkManager
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_CALDAV
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_ETESYNC
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_GOOGLE_TASKS
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_OPENTASK
import java.util.concurrent.Executors.newSingleThreadExecutor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncAdapters @Inject constructor(
        workManager: WorkManager,
        private val caldavDao: CaldavDao,
        private val googleTaskDao: GoogleTaskDao,
        private val googleTaskListDao: GoogleTaskListDao) {
    private val scope = CoroutineScope(newSingleThreadExecutor().asCoroutineDispatcher() + SupervisorJob())
    private val googleTasks = Debouncer(TAG_SYNC_GOOGLE_TASKS) { workManager.googleTaskSync(it) }
    private val caldav = Debouncer(TAG_SYNC_CALDAV) { workManager.caldavSync(it) }
    private val eteSync = Debouncer(TAG_SYNC_ETESYNC) { workManager.eteSync(it) }
    private val opentasks = Debouncer(TAG_SYNC_OPENTASK) { workManager.openTaskSync() }

    fun sync(task: Task, original: Task?) = scope.launch {
        if (task.checkTransitory(SyncFlags.SUPPRESS_SYNC)) {
            return@launch
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
            if (caldavDao.isAccountType(task.id, TYPE_OPENTASKS)) {
                opentasks.sync(false)
            }
        }
    }

    fun syncOpenTasks() = scope.launch {
        opentasks.sync(false)
    }

    fun sync() {
        sync(false)
    }

    fun sync(immediate: Boolean) = scope.launch {
        val googleTasksEnabled = async { isGoogleTaskSyncEnabled() }
        val caldavEnabled = async { isCaldavSyncEnabled() }
        val eteSyncEnabled = async { isEteSyncEnabled() }
        val opentasksEnabled = async { isOpenTaskSyncEnabled() }

        if (googleTasksEnabled.await()) {
            googleTasks.sync(immediate)
        }

        if (caldavEnabled.await()) {
            caldav.sync(immediate)
        }

        if (eteSyncEnabled.await()) {
            eteSync.sync(immediate)
        }

        if (opentasksEnabled.await()) {
            opentasks.sync(immediate)
        }
    }

    private suspend fun isGoogleTaskSyncEnabled() = googleTaskListDao.getAccounts().isNotEmpty()

    private suspend fun isCaldavSyncEnabled() = caldavDao.getAccounts(TYPE_CALDAV).isNotEmpty()

    private suspend fun isEteSyncEnabled() = caldavDao.getAccounts(TYPE_ETESYNC).isNotEmpty()

    private suspend fun isOpenTaskSyncEnabled() = caldavDao.getAccounts(TYPE_OPENTASKS).isNotEmpty()
}