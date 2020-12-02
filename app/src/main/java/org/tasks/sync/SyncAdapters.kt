package org.tasks.sync

import com.todoroo.astrid.data.SyncFlags
import com.todoroo.astrid.data.Task
import kotlinx.coroutines.*
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.data.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.CaldavAccount.Companion.TYPE_ETESYNC
import org.tasks.data.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao
import org.tasks.data.GoogleTaskListDao
import org.tasks.data.OpenTaskDao
import org.tasks.jobs.WorkManager
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_CALDAV
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_ETEBASE
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_ETESYNC
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_GOOGLE_TASKS
import org.tasks.jobs.WorkManager.Companion.TAG_SYNC_OPENTASK
import org.tasks.preferences.Preferences
import java.util.concurrent.Executors.newSingleThreadExecutor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncAdapters @Inject constructor(
        workManager: WorkManager,
        private val caldavDao: CaldavDao,
        private val googleTaskDao: GoogleTaskDao,
        private val googleTaskListDao: GoogleTaskListDao,
        private val openTaskDao: OpenTaskDao,
        private val preferences: Preferences,
        private val localBroadcastManager: LocalBroadcastManager
) {
    private val scope = CoroutineScope(newSingleThreadExecutor().asCoroutineDispatcher() + SupervisorJob())
    private val googleTasks = Debouncer(TAG_SYNC_GOOGLE_TASKS) { workManager.googleTaskSync(it) }
    private val caldav = Debouncer(TAG_SYNC_CALDAV) { workManager.caldavSync(it) }
    @Deprecated("use etebase") private val eteSync = Debouncer(TAG_SYNC_ETESYNC) { workManager.eteSync(it) }
    private val eteBaseSync = Debouncer(TAG_SYNC_ETEBASE) { workManager.eteBaseSync(it) }
    private val opentasks = Debouncer(TAG_SYNC_OPENTASK) { workManager.openTaskSync(it) }
    private val syncStatus = Debouncer("sync_status") {
        if (preferences.getBoolean(R.string.p_sync_ongoing_android, false) != it
                && isOpenTaskSyncEnabled()) {
            preferences.setBoolean(R.string.p_sync_ongoing_android, it)
            localBroadcastManager.broadcastRefresh()
        }
    }

    fun sync(task: Task, original: Task?) = scope.launch {
        if (task.checkTransitory(SyncFlags.SUPPRESS_SYNC)) {
            return@launch
        }
        if (!task.googleTaskUpToDate(original)
                && googleTaskDao.getAllByTaskId(task.id).isNotEmpty()) {
            googleTasks.sync(false)
        }
        if (task.checkTransitory(SyncFlags.FORCE_CALDAV_SYNC) || !task.caldavUpToDate(original)) {
            if (caldavDao.isAccountType(task.id, TYPE_CALDAV)
                    || caldavDao.isAccountType(task.id, TYPE_TASKS)) {
                caldav.sync(false)
            }
            if (caldavDao.isAccountType(task.id, TYPE_ETESYNC)) {
                eteSync.sync(false)
            }
            if (caldavDao.isAccountType(task.id, TYPE_ETEBASE)) {
                eteBaseSync.sync(false)
            }
            if (caldavDao.isAccountType(task.id, TYPE_OPENTASKS)) {
                opentasks.sync(false)
            }
        }
    }

    fun setOpenTaskSyncActive(active: Boolean) = scope.launch {
        syncStatus.sync(active)
    }

    fun syncOpenTasks() = scope.launch {
        opentasks.sync(true)
    }

    fun sync() {
        sync(false)
    }

    fun sync(immediate: Boolean) = scope.launch {
        val googleTasksEnabled = async { isGoogleTaskSyncEnabled() }
        val caldavEnabled = async { isCaldavSyncEnabled() }
        val eteSyncEnabled = async { isEteSyncEnabled() }
        val eteBaseEnabled = async { isEtebaseEnabled() }
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

        if (eteBaseEnabled.await()) {
            eteBaseSync.sync(immediate)
        }

        if (opentasksEnabled.await()) {
            opentasks.sync(immediate)
        }
    }

    private suspend fun isGoogleTaskSyncEnabled() = googleTaskListDao.getAccounts().isNotEmpty()

    private suspend fun isCaldavSyncEnabled() =
            caldavDao.getAccounts(TYPE_CALDAV, TYPE_TASKS).isNotEmpty()

    @Deprecated("use etebase")
    private suspend fun isEteSyncEnabled() = caldavDao.getAccounts(TYPE_ETESYNC).isNotEmpty()

    private suspend fun isEtebaseEnabled() = caldavDao.getAccounts(TYPE_ETEBASE).isNotEmpty()

    private suspend fun isOpenTaskSyncEnabled() =
            caldavDao.getAccounts(TYPE_OPENTASKS).isNotEmpty()
                    || openTaskDao.newAccounts().isNotEmpty()
}