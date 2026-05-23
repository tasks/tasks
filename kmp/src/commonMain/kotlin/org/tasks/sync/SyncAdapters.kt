package org.tasks.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.CaldavDao
import org.tasks.jobs.BackgroundWork
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.FORCE_CALDAV_SYNC
import org.tasks.data.entity.FORCE_SYNC
import org.tasks.data.entity.SUPPRESS_SYNC
import org.tasks.data.entity.Task
import org.tasks.preferences.TasksPreferences
import kotlin.coroutines.CoroutineContext

class SyncAdapters(
    private val backgroundWork: BackgroundWork,
    private val caldavDao: CaldavDao,
    private val openTaskSyncCheck: suspend () -> Boolean,
    private val tasksPreferences: TasksPreferences,
    private val refreshBroadcaster: RefreshBroadcaster,
    coroutineContext: CoroutineContext,
) {
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())
    private val sync = Debouncer(
        tag = TAG_SYNC,
        default = SyncSource.NONE,
        merge = { current, new -> current.upgrade(new) }
    ) { backgroundWork.sync(it) }
    private val syncStatus = Debouncer(
        tag = "sync_status",
        default = false
    ) { newState ->
        val currentState = tasksPreferences.get(TasksPreferences.syncOngoingAndroid, false)
        if (currentState != newState && isOpenTaskSyncEnabled()) {
            tasksPreferences.set(TasksPreferences.syncOngoingAndroid, newState)
            refreshBroadcaster.broadcastRefresh()
        }
    }

    fun sync(task: Task, original: Task?) = scope.launch {
        if (task.checkTransitory(SUPPRESS_SYNC)) {
            return@launch
        }
        if (task.checkTransitory(FORCE_SYNC)) {
            sync.sync(SyncSource.TASK_CHANGE)
            return@launch
        }
        val accountType = caldavDao.getAccountType(task.id) ?: return@launch
        val needsSync = when (accountType) {
            TYPE_GOOGLE_TASKS -> !task.googleTaskUpToDate(original)
            TYPE_MICROSOFT -> !task.microsoftUpToDate(original)
            TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE, TYPE_OPENTASKS ->
                task.checkTransitory(FORCE_CALDAV_SYNC) || !task.caldavUpToDate(original)
            else -> false
        }
        if (needsSync) {
            sync.sync(SyncSource.TASK_CHANGE)
        }
    }

    fun setOpenTaskSyncActive(active: Boolean) = scope.launch {
        syncStatus.sync(active)
    }

    fun sync(source: SyncSource) = scope.launch {
        val caldavEnabled = async { isSyncEnabled() }
        val opentasksEnabled = async { isOpenTaskSyncEnabled() }

        if (caldavEnabled.await() || opentasksEnabled.await()) {
            sync.sync(source)
        }
    }

    private suspend fun isSyncEnabled() =
            caldavDao
                .getAccounts(
                    TYPE_GOOGLE_TASKS,
                    TYPE_CALDAV,
                    TYPE_TASKS,
                    TYPE_ETEBASE,
                    TYPE_MICROSOFT
                )
                .isNotEmpty()

    private suspend fun isOpenTaskSyncEnabled() = openTaskSyncCheck()

    companion object {
        const val TAG_SYNC = "tag_sync"
    }
}
