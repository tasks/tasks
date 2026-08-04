package org.tasks.sync

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.DirtyDao
import org.tasks.data.dao.DirtyTaskVersion
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.jobs.BackgroundWork
import org.tasks.preferences.TasksPreferences
import kotlin.coroutines.CoroutineContext

class SyncAdapters(
    private val backgroundWork: BackgroundWork,
    private val caldavDao: CaldavDao,
    private val dirtyDao: DirtyDao,
    private val openTaskListsActive: suspend () -> Boolean,
    private val tasksPreferences: TasksPreferences,
    private val refreshBroadcaster: RefreshBroadcaster,
    coroutineContext: CoroutineContext,
    private val debounceMs: Long = SYNC_DEBOUNCE_MS,
) {
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())

    private val syncRequests = Channel<SyncSource>(Channel.UNLIMITED)
    private val openTaskSyncActive = Channel<Boolean>(Channel.CONFLATED)

    private data class AccountPresence(val syncable: Boolean, val openTasks: Boolean)

    private data class PendingSync(
        val source: SyncSource = SyncSource.NONE,
        val requestedDirectly: Boolean = false,
    ) {
        fun upgrade(next: SyncSource) = PendingSync(
            source = source.upgrade(next),
            requestedDirectly = requestedDirectly || next != SyncSource.TASK_CHANGE,
        )
    }

    private val accountPresence: StateFlow<AccountPresence?> = caldavDao.watchAccounts()
        .map { accounts ->
            AccountPresence(
                syncable = accounts.any { it.accountType in SYNCABLE_TYPES },
                openTasks = accounts.any { it.accountType == TYPE_OPENTASKS },
            )
        }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    private var lastHandedOff: List<DirtyTaskVersion>? = null

    init {
        scope.launch {
            val pending = MutableStateFlow(PendingSync())
            merge(
                dirtyDao.hasDirtyTasks().filter { it }.map { SyncSource.TASK_CHANGE },
                syncRequests.receiveAsFlow(),
            )
                .onEach { source -> pending.update { it.upgrade(source) } }
                .debounce(debounceMs)
                .collect {
                    val (source, direct) = pending.getAndUpdate { PendingSync() }
                    runSync(source, skipDirtyCheck = direct)
                }
        }
        scope.launch {
            openTaskSyncActive
                .receiveAsFlow()
                .debounce(debounceMs)
                .collect { newState ->
                    val currentState =
                        tasksPreferences.get(TasksPreferences.syncOngoingAndroid, false)
                    if (currentState != newState && isOpenTaskSyncEnabled()) {
                        tasksPreferences.set(TasksPreferences.syncOngoingAndroid, newState)
                        refreshBroadcaster.broadcastRefresh()
                    }
                }
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

    private suspend fun runSync(source: SyncSource, skipDirtyCheck: Boolean) {
        if (source == SyncSource.NONE) {
            return
        }
        val versions = if (skipDirtyCheck) {
            null
        } else {
            dirtyDao.getSyncableDirtyVersions().also {
                if (it.isEmpty() || it == lastHandedOff) {
                    log.d { "nothing new to push (${it.size} pending), skipping $source" }
                    return
                }
            }
        }
        if (!isSyncEnabled()) {
            return
        }
        versions?.let { lastHandedOff = it }
        log.d { "sync source=$source pending=${versions?.size ?: "n/a"}" }
        backgroundWork.sync(source)
    }

    fun setOpenTaskSyncActive(active: Boolean) {
        openTaskSyncActive.trySend(active)
    }

    fun sync(source: SyncSource) {
        syncRequests.trySend(source)
    }

    private suspend fun presence(): AccountPresence = accountPresence.filterNotNull().first()

    private suspend fun isSyncEnabled(): Boolean {
        val presence = presence()
        return presence.syncable || presence.openTasks || openTaskListsActive()
    }

    private suspend fun isOpenTaskSyncEnabled(): Boolean =
        presence().openTasks || openTaskListsActive()

    companion object {
        private val log = Logger.withTag("SyncAdapters")
        const val TAG_SYNC = "tag_sync"
        private const val SYNC_DEBOUNCE_MS = 1000L
        private val SYNCABLE_TYPES = setOf(
            TYPE_GOOGLE_TASKS,
            TYPE_CALDAV,
            TYPE_TASKS,
            TYPE_ETEBASE,
            TYPE_MICROSOFT,
        )
    }
}
