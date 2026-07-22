package org.tasks.sync

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.DirtyDao
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.jobs.BackgroundWork
import org.tasks.preferences.TasksPreferences
import kotlin.coroutines.CoroutineContext

class SyncAdapters(
    private val backgroundWork: BackgroundWork,
    private val caldavDao: CaldavDao,
    private val dirtyDao: DirtyDao,
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

    init {
        scope.launch {
            dirtyDao
                .hasDirtyTasks()
                .onEach { log.d { "dirty table changed: hasDirty=$it" } }
                .filter { it }
                .conflate()
                .collect { sync(SyncSource.TASK_CHANGE).join() }
        }
        scope.launch {
            var previousCount = -1
            caldavDao
                .watchAccounts()
                .map { it.size }
                .distinctUntilChanged()
                .collect { count ->
                    backgroundWork.updateBackgroundSync()
                    if (previousCount in 0..<count) {
                        log.d { "account added ($previousCount -> $count), syncing" }
                        refreshBroadcaster.broadcastRefresh()
                        sync(SyncSource.ACCOUNT_ADDED)
                    }
                    previousCount = count
                }
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
        private val log = Logger.withTag("SyncAdapters")
        const val TAG_SYNC = "tag_sync"
    }
}
