package org.tasks.notifications

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.NotificationDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Task
import org.tasks.extensions.closeQuietly
import org.tasks.extensions.guarded
import org.tasks.time.endOfMinute
import org.tasks.time.monotonicMillis

private const val TAG = "DesktopNotifier"

internal class DesktopNotifier(
    private val taskDao: TaskDao,
    private val notificationDao: NotificationDao,
    private val alarmDao: AlarmDao,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val signalScheduler: () -> Unit,

    private val gatesOnPermission: () -> Boolean = { true },

    private val notificationsEnabled: suspend () -> Boolean = { true },

    private val recordScreenCleared: suspend () -> Unit = {},

    private val takeScreenCleared: suspend () -> Boolean = { false },

    private val claimPlatformIds: suspend () -> Boolean = { true },

    private val elapsedRealtime: () -> Long = { monotonicMillis() },

    private val createBackend: () -> PlatformNotifications?,
) : Notifier {
    private val permissionLock = Mutex()

    private val buildLock = Mutex()

    @Volatile
    private var backend: PlatformNotifications? = null

    @Volatile
    private var closed = false

    @Volatile
    private var retryBackendAt = Long.MIN_VALUE

    private suspend fun backend(): PlatformNotifications? {
        if (closed) {
            return null
        }
        backend?.let { return it }
        return buildLock.withLock {
            backend?.let { return@withLock it }
            if (closed || elapsedRealtime() < retryBackendAt) {
                return@withLock null
            }
            val built = withContext(Dispatchers.IO) { createBackend() }
            if (built == null) {
                retryBackendAt = elapsedRealtime() + BACKEND_RETRY_MS
                if (!closed) {
                    Logger.w(tag = TAG) {
                        "No notification backend, nothing will be posted for the next " +
                                "${BACKEND_RETRY_MS / 1_000}s"
                    }
                }
                return@withLock null
            }
            Logger.i(tag = TAG) {
                "Using ${built::class.simpleName} actions=${built.supportsActions} " +
                        "open=${built.supportsOpen} " +
                        "actionsSurviveRestart=${built.actionsSurviveRestart}"
            }
            val published = synchronized(this) {
                if (closed) null else built.also { backend = it }
            }
            if (published == null) {
                built.close()
                return@withLock null
            }
            adoptPlatformIds(published)
            published
        }
    }

    private suspend fun <T> withBackend(fallback: T, block: suspend (PlatformNotifications) -> T): T =
        backend()?.let { block(it) } ?: fallback

    private suspend fun adoptPlatformIds(backend: PlatformNotifications) {
        guarded("Failed to hand back platform ids", Unit, warnOnly = true) {
            val recorded = notificationDao.withPlatformIds()
                .mapNotNull { row -> row.platformId?.let { row.taskId to it } }
                .toMap()
            if (recorded.isEmpty()) {
                return@guarded
            }
            if (!claimPlatformIds()) {
                Logger.i(tag = TAG) { "Discarding ${recorded.size} id(s) from an earlier session" }
                notificationDao.clearPlatformIds()
                return@guarded
            }
            backend.adopt(recorded)
        }
    }

    @Volatile
    private var permissionResolved = false

    private suspend fun <T> guarded(
        what: String,
        fallback: T,
        warnOnly: Boolean = false,
        onFailure: suspend (Throwable) -> Unit = {},
        block: suspend () -> T,
    ): T = guarded(TAG, what, fallback, warnOnly, onFailure, block)

    suspend fun requestPermissionIfNeeded() =
        guarded("Failed to resolve notification permission", Unit) { requestPermission() }

    private suspend fun enabled(holding: String): Boolean {
        val enabled = guarded("Failed to read whether reminders are enabled", true) {
            notificationsEnabled()
        }
        if (!enabled) {
            Logger.d(tag = TAG) { "Reminders are turned off, $holding" }
        }
        return enabled
    }

    private suspend fun requestPermission() {
        if (permissionResolved) {
            return
        }

        if (!enabled("not asking for permission")) {
            return
        }

        if (!gatesOnPermission()) {
            permissionResolved = true
            return
        }

        if (!alarmDao.hasActiveAlarms()) {
            return
        }

        if (!permissionLock.tryLock()) {
            return
        }
        try {
            if (permissionResolved) {
                return
            }
            permissionResolved = withBackend(false) { backend ->

                val answered = when (backend.permission()) {
                    NotificationPermission.NOT_DETERMINED -> backend.requestPermission()
                    NotificationPermission.DENIED -> {
                        Logger.w(tag = TAG) {
                            "Reminders are set but notifications are turned off for Tasks"
                        }
                        true
                    }
                    NotificationPermission.GRANTED -> true
                }
                answered
            }
        } finally {
            permissionLock.unlock()
        }
        if (permissionResolved) {
            signalScheduler()
        }
    }

    suspend fun hold(): Hold {
        if (!enabled("not scanning")) {
            return Hold.UNTIL_SIGNALLED
        }
        if (!gatesOnPermission()) {
            return Hold.NONE
        }
        val backend = this.backend ?: return Hold.NONE
        val permission = guarded("Failed to read notification permission", null) {
            backend.permission()
        } ?: return Hold.NONE
        return when (permission) {
            NotificationPermission.GRANTED -> Hold.NONE
            NotificationPermission.NOT_DETERMINED,
            NotificationPermission.DENIED -> {
                Logger.d(tag = TAG) { "Permission is $permission, not scanning" }
                Hold.UNTIL_NEXT_ALARM
            }
        }
    }

    suspend fun triggerNotifications(entries: List<Notification>): List<Long> {
        if (entries.isEmpty()) {
            return emptyList()
        }

        Logger.d(tag = TAG) { "Due: $entries" }

        if (!enabled("holding ${entries.size}")) {
            return emptyList()
        }
        val tasks = taskDao.fetch(entries.map { it.taskId }).associateBy { it.id }
        val posting = entries.mapNotNull { entry ->
            val task = tasks[entry.taskId]
            when {
                task == null -> {
                    Logger.w(tag = TAG) { "No task for $entry" }
                    null
                }

                task.isCompleted || task.isDeleted -> {
                    Logger.d(tag = TAG) { "Already completed or deleted, skipping $task" }
                    null
                }
                else -> task to entry
            }
        }
        if (posting.isEmpty()) {
            Logger.d(tag = TAG) { "Nothing left to post from ${entries.map { it.taskId }}" }
            return emptyList()
        }

        return withBackend(emptyList()) { backend -> post(posting, backend) }
    }

    private suspend fun post(
        posting: List<Pair<Task, Notification>>,
        backend: PlatformNotifications,
    ): List<Long> {
        val permission = guarded(
            what = "Failed to read notification permission",
            fallback = null,
        ) { backend.permission() } ?: return emptyList()
        when (permission) {
            NotificationPermission.GRANTED -> {}
            NotificationPermission.NOT_DETERMINED, NotificationPermission.DENIED -> {
                Logger.d(tag = TAG) { "Permission is $permission, holding ${posting.size}" }
                return emptyList()
            }
        }

        val (superseded, batch) = if (posting.size > MAX_NOTIFICATIONS) {
            posting.dropLast(MAX_NOTIFICATIONS) to posting.takeLast(MAX_NOTIFICATIONS)
        } else {
            emptyList<Pair<Task, Notification>>() to posting
        }
        if (superseded.isNotEmpty()) {
            Logger.d(tag = TAG) {
                "${posting.size} due, over the cap of $MAX_NOTIFICATIONS - " +
                        "holding ${superseded.size} back"
            }
        }

        val existing = notificationDao.getAll().toSet()
        val delivered = mutableListOf<Long>()
        val failed = mutableListOf<Long>()

        var alerted = false
        for ((task, entry) in batch) {
            Logger.d(tag = TAG) { "Posting $entry for $task" }
            notificationDao.insertAll(listOf(entry))
            val result = guarded(
                what = "Failed to post notification for ${entry.taskId}",
                fallback = false,
            ) {
                backend.show(
                    taskId = entry.taskId,
                    title = NotificationContent.title(task),
                    body = NotificationContent.body(task, entry),
                    actions = NotificationContent.actionsFor(task, backend),

                    alert = if (alerted) Alert.QUIET else Alert.DEFAULT,
                )
            }

            alerted = true
            if (result) {
                taskDao.setLastNotified(entry.taskId, entry.timestamp.endOfMinute())
                guarded("Failed to record the platform id for ${entry.taskId}", Unit, warnOnly = true) {
                    backend.platformId(entry.taskId)?.let {
                        notificationDao.setPlatformId(entry.taskId, it)
                    }
                }
                delivered.add(entry.taskId)
            } else {
                failed.add(entry.taskId)
            }
        }

        Logger.i(tag = TAG) {
            "Posted $delivered" + if (failed.isEmpty()) "" else ", failed $failed"
        }

        if (superseded.isNotEmpty() && failed.isEmpty()) {
            for ((_, entry) in superseded) {
                taskDao.setLastNotified(entry.taskId, entry.timestamp.endOfMinute())
            }
        } else if (superseded.isNotEmpty()) {
            Logger.w(tag = TAG) {
                "${failed.size} of ${batch.size} did not go out, " +
                        "leaving ${superseded.size} over-cap reminder(s) overdue"
            }
        }

        if (failed.isNotEmpty()) {
            val discarded =
                undeliveredRows(attempted = failed, delivered = emptyList(), existing = existing)
            val kept = failed - discarded.toSet()
            if (discarded.isNotEmpty()) {
                Logger.w(tag = TAG) { "Undelivered, dropping rows: $discarded" }
                notificationDao.deleteAll(discarded)
            }
            if (kept.isNotEmpty()) {
                Logger.w(tag = TAG) { "Undelivered, keeping outstanding rows: $kept" }
            }
        }
        if (delivered.isNotEmpty()) {
            enforceLimit(backend)
        }
        if (delivered.isNotEmpty()) {
            refreshBroadcaster.broadcastRefresh()
        }
        return delivered
    }

    private suspend fun enforceLimit(backend: PlatformNotifications) {
        val outstanding = notificationDao.getAllOrdered()
        val excess = outstanding.size - MAX_NOTIFICATIONS
        if (excess <= 0) {
            return
        }
        val evicted = outstanding
            .sortedWith(EVICTION_ORDER)
            .take(excess)
            .map { it.taskId }
        Logger.d(tag = TAG) { "Over $MAX_NOTIFICATIONS on screen, evicting $evicted" }
        cancel(evicted, CancelReason.EVICTED, through = backend)
    }

    private suspend fun unchangedSince(
        candidates: List<Long>,
        before: List<Notification>,
    ): Pair<List<Long>, List<Long>> {
        val was = before.associate { it.taskId to it.timestamp }
        val now = notificationDao.getAllOrdered().associate { it.taskId to it.timestamp }
        return candidates.partition { now[it] == was[it] }
    }

    override suspend fun cancel(id: Long, reason: CancelReason) = cancel(listOf(id), reason)

    override suspend fun cancel(ids: List<Long>, reason: CancelReason) =
        cancel(ids, reason, through = null)

    override suspend fun cancelAll(reason: CancelReason) {
        val ids = notificationDao.getAll()
        if (ids.isEmpty()) {
            return
        }
        Logger.d(tag = TAG) { "Cancelling everything ($ids) reason=$reason" }

        val backend = guarded("Failed to build a backend to dismiss through", null) { backend() }
        cancel(ids, reason, through = backend)
    }

    private suspend fun cancel(
        ids: List<Long>,
        reason: CancelReason,
        through: PlatformNotifications?,

        withoutRows: Boolean = false,
    ) {
        if (ids.isEmpty()) {
            return
        }

        val before = if (ids.size == 1) {
            listOfNotNull(notificationDao.get(ids.first()))
        } else {
            val cancelling = ids.toSet()
            notificationDao.getAllOrdered().filter { it.taskId in cancelling }
        }
        val affected = before.map { it.taskId }.ifEmpty { if (withoutRows) ids else emptyList() }
        if (affected.isEmpty()) {
            return
        }
        Logger.d(tag = TAG) { "Cancelling $affected reason=$reason" }

        val backend = through ?: this.backend
        val dismissed: Set<Long> = if (backend == null) {
            Logger.w(tag = TAG) { "No backend to dismiss $affected through" }
            emptySet()
        } else {
            guarded(
                what = "Failed to dismiss $affected",
                fallback = emptySet(),
                warnOnly = true,
            ) {
                backend.dismiss(affected)
            }
        }

        val (unchanged, republished) = unchangedSince(affected, before)
        if (republished.isNotEmpty()) {
            Logger.d(tag = TAG) { "Re-posted while cancelling, keeping $republished" }
        }
        val (gone, stillUp) = unchanged.partition { it in dismissed }
        if (gone.isNotEmpty()) {
            notificationDao.deleteAll(gone)
        }
        if (stillUp.isNotEmpty()) {
            Logger.w(tag = TAG) { "Not dismissed, keeping rows for $stillUp" }
        }
        refreshBroadcaster.broadcastRefresh()
    }

    suspend fun cancelDeleted(taskIds: List<Long>) {
        val backend = this.backend ?: return
        cancel(taskIds, CancelReason.CLEANUP, through = backend, withoutRows = true)
    }

    suspend fun reconcileNotifications() {
        guarded("Failed to reconcile notifications", Unit) {
            val screenCleared = takeScreenCleared()
            val outstanding = notificationDao.getAllOrdered()
            if (outstanding.isEmpty()) {
                Logger.d(tag = TAG) { "Nothing outstanding to reconcile" }
                return@guarded
            }
            Logger.i(tag = TAG) { "Reconciling $outstanding" }

            if (!enabled("not reconciling ${outstanding.size} row(s)")) {
                return@guarded
            }

            val tasks = taskDao.fetch(outstanding.map { it.taskId }).associateBy { it.id }
            val (live, gone) = outstanding.partition {
                tasks[it.taskId]?.let { task -> !task.isCompleted && !task.isDeleted } == true
            }
            if (gone.isNotEmpty()) {
                Logger.d(tag = TAG) { "Completed or deleted while closed: $gone" }
            }
            val dropping = gone.map { it.taskId }.toMutableList()

            reconcile(reconcilingBackend(), live, dropping, outstanding, tasks, screenCleared)
        }
    }

    private suspend fun reconcile(
        backend: PlatformNotifications?,
        live: List<Notification>,
        dropping: MutableList<Long>,
        outstanding: List<Notification>,
        tasks: Map<Long, Task>,
        screenCleared: Boolean,
    ) {
        val surviving = if (screenCleared) {
            if (live.isNotEmpty()) {
                Logger.d(tag = TAG) { "Taken down at shutdown, restoring ${live.size} row(s)" }
            }
            live
        } else {
            val stillUp = when {
                live.isEmpty() -> DeliveredQuery.Known(emptySet())
                backend == null -> DeliveredQuery.Unknown
                else -> delivered(backend)
            }
            if (stillUp is DeliveredQuery.Unknown) {
                Logger.d(tag = TAG) {
                    "Nothing can confirm what is still on screen, dropping ${live.size} row(s)"
                }
                dropping += live.map { it.taskId }
                emptyList()
            } else {
                val onScreen = (stillUp as DeliveredQuery.Known).taskIds
                Logger.d(tag = TAG) { "Still on screen: $onScreen" }
                val (up, cleared) = live.partition { it.taskId in onScreen }
                if (cleared.isNotEmpty()) {
                    Logger.d(tag = TAG) { "Cleared while closed: ${cleared.map { it.taskId }}" }
                    dropping += cleared.map { it.taskId }
                }
                up
            }
        }
        if (dropping.isNotEmpty()) {
            drop(backend, dropping, outstanding)
        }
        if (backend != null && !backend.actionsSurviveRestart) {
            repost(backend, surviving, tasks)
        }
    }

    private suspend fun drop(
        backend: PlatformNotifications?,
        dropping: List<Long>,
        before: List<Notification>,
    ) {
        val (stale, republished) = unchangedSince(dropping, before)
        if (republished.isNotEmpty()) {
            Logger.d(tag = TAG) { "Re-posted while reconciling, keeping $republished" }
        }
        if (stale.isEmpty()) {
            return
        }

        if (backend != null) {
            val takenDown = guarded("Failed to dismiss $stale", emptySet<Long>(), warnOnly = true) {
                backend.dismiss(stale)
            }
            val left = stale.filterNot { it in takenDown }
            if (left.isNotEmpty()) {
                Logger.w(tag = TAG) { "Could not take down $left, dropping the rows anyway" }
            }
        }

        val (gone, delivered) = unchangedSince(stale, before)
        if (delivered.isNotEmpty()) {
            Logger.d(tag = TAG) { "Re-posted while dismissing, keeping $delivered" }
        }
        if (gone.isEmpty()) {
            return
        }
        notificationDao.deleteAll(gone)
        refreshBroadcaster.broadcastRefresh()
    }

    private suspend fun repost(
        backend: PlatformNotifications,
        entries: List<Notification>,
        tasks: Map<Long, Task>,
    ) {
        if (entries.isEmpty()) {
            return
        }
        Logger.d(tag = TAG) { "Restoring actions on $entries" }
        for (entry in entries) {
            val task = tasks[entry.taskId] ?: continue
            val reposted = guarded(
                "Failed to restore actions on ${entry.taskId}",
                false,
                warnOnly = true,
            ) {
                backend.show(
                    taskId = entry.taskId,
                    title = NotificationContent.title(task),
                    body = NotificationContent.body(task, entry),
                    actions = NotificationContent.actionsFor(task, backend),
                    alert = Alert.SUPPRESSED,
                )
            }
            if (reposted) {
                guarded("Failed to record the platform id for ${entry.taskId}", Unit, warnOnly = true) {
                    backend.platformId(entry.taskId)?.let {
                        notificationDao.setPlatformId(entry.taskId, it)
                    }
                }
            }
        }
    }

    private suspend fun reconcilingBackend(): PlatformNotifications? =
        guarded("Failed to build a backend to reconcile against", null) { backend() }

    private suspend fun delivered(backend: PlatformNotifications): DeliveredQuery =
        guarded("Failed to read delivered notifications", DeliveredQuery.Unknown) {
            backend.delivered()
        }

    override fun triggerNotifications() {
        signalScheduler()
    }

    override suspend fun updateTimerNotification() {
    }

    suspend fun shutdown() {
        if (close()) {
            guarded("Failed to record that the screen was cleared", Unit, warnOnly = true) {
                recordScreenCleared()
            }
            guarded("Failed to clear the recorded platform ids", Unit, warnOnly = true) {
                notificationDao.clearPlatformIds()
            }
        }
    }

    fun close(): Boolean {
        closed = true
        Logger.i(tag = TAG) { "Shutting the notification backend down" }

        val closing = synchronized(this) { backend.also { backend = null } } ?: return false
        var cleared = false
        closeQuietly(TAG, "the notification backend") {
            cleared = closing.close()
        }
        return cleared
    }

    companion object {
        internal val EVICTION_ORDER: Comparator<Notification> =
            compareBy({ it.timestamp }, { it.taskId })

        internal const val BACKEND_RETRY_MS = 60_000L
    }
}
