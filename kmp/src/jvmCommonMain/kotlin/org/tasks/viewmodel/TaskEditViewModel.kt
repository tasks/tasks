package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.todoroo.astrid.alarms.AlarmService
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.WeekDay
import org.jetbrains.compose.resources.getString
import org.tasks.compose.pickers.NO_DAY
import org.tasks.compose.pickers.NO_TIME
import org.tasks.compose.pickers.resolveStartDate
import org.tasks.compose.pickers.startDayOf
import org.tasks.compose.pickers.withTimeMarkerOr
import org.tasks.data.TaskContainer
import org.tasks.data.TaskCreator
import org.tasks.data.applicableTo
import org.tasks.data.PendingTask
import org.tasks.data.SubtaskNode
import org.tasks.data.SubtaskRow
import org.tasks.data.StagedSubtaskEdits
import org.tasks.data.SubtaskTreeWriter
import org.tasks.data.SubtaskTreeRegistry
import org.tasks.data.SubtaskTrees
import org.tasks.data.TaskMover
import org.tasks.data.TaskSaver
import org.tasks.data.resolveMove
import org.tasks.data.nested
import org.tasks.data.visible
import org.tasks.data.isRearranged
import org.tasks.data.rowsOf
import org.tasks.data.subtaskQuery
import org.tasks.data.subtaskKey
import org.tasks.data.fetchTasks
import org.tasks.data.getDefaultAlarms
import org.tasks.data.setDefaultReminders
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.SYNC_ALARMS
import org.tasks.data.entity.SYNC_TAGS
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.DatePickerPreferences
import org.tasks.preferences.TaskDefaultSettings
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.service.TaskCompleter
import org.tasks.service.TaskDeleter
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.noon
import org.tasks.time.startOfDay
import java.util.concurrent.atomic.AtomicLong
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.no_title

internal const val WATCH_MAX_ATTEMPTS = 5
private const val WATCH_RETRY_DELAY_MS = 1_000L

/** Distinguishes editors on a destination that names no task at all - see [TaskEditViewModel]. */
private val anonymousEditors = AtomicLong()

class TaskEditViewModel(
    taskId: Long,
    private val remoteId: String,
    private val listId: Long?,
    private val tagUuid: String?,
    private val isSubtaskDraft: Boolean = false,
    private val taskDao: TaskDao,
    private val taskSaver: TaskSaver,
    private val caldavDao: CaldavDao,
    private val taskMover: TaskMover,
    private val tagDao: TagDao,
    private val tagDataDao: TagDataDao,
    private val alarmDao: AlarmDao,
    private val alarmService: AlarmService,
    private val appPreferences: AppPreferences,
    private val externalScope: CoroutineScope,
    private val pendingSaves: PendingTaskSaves,
    private val taskCompleter: TaskCompleter,
    private val taskDeleter: TaskDeleter,
    private val treeRegistry: SubtaskTreeRegistry,
    private val subtaskWriter: SubtaskTreeWriter,
    private val refreshFlow: Flow<Unit> = emptyFlow(),
    private val taskCreator: TaskCreator = TaskCreator(),
    private val untitled: suspend () -> String = { getString(Res.string.no_title) },
) : ViewModel() {
    private val taskId: Long? = taskId.takeIf { it != Task.NO_ID && it > 0 }

    /**
     * The uuid this editor was opened on, or null if the destination doesn't name one.
     *
     * [Task.NO_UUID] is not a uuid: it is what [Task.uuid] returns for a task whose remoteId is null
     * or empty, so every such task carries it. Taking it at face value put them all on one shared
     * save lock, and had a destination with no row id create a blank task stamped with it - a
     * remoteId that collides with the next one the moment there are two.
     */
    private val uuid: String? = remoteId.takeIf { it.isNotBlank() && it != Task.NO_UUID }

    /**
     * Identifies the task being edited for [PendingTaskSaves.withLock]. Two editors opened on the
     * same task share it - that is the whole point - while editors on different tasks never block
     * each other.
     *
     * A destination carrying neither a row id nor a uuid - a restored back stack, a deep link, a
     * caller that forgot to mint one - identifies nothing at all, so it gets a key of its own rather
     * than the shared one that "id:null" collapsed to.
     */
    private val saveKey: String = uuid
        ?: this.taskId?.let { "id:$it" }
        ?: "editor:${anonymousEditors.incrementAndGet()}"

    private val log = Logger.withTag("TaskEditViewModel")

    private val borrowedTree: SubtaskTrees? =
        uuid?.let { key ->
            treeRegistry.holding(key)?.takeIf { it.get(key)?.isNew == true }
        }

    private val subtaskTrees: SubtaskTrees = borrowedTree ?: treeRegistry.open()

    private val ownsTree: Boolean get() = borrowedTree == null

    private fun nodeTree(): SubtaskTrees? = treeRegistry.holding(treeKey.value)

    data class State(
        val isLoading: Boolean = true,
        val task: Task = Task(),
        val originalTask: Task = Task(),
        val list: CaldavFilter? = null,
        val originalList: CaldavFilter? = null,
        val tags: List<TagData> = emptyList(),
        val originalTags: List<TagData> = emptyList(),
        val alarms: ImmutableSet<Alarm> = persistentSetOf(),
        val originalAlarms: ImmutableSet<Alarm> = persistentSetOf(),
        val deleted: Boolean = false,
        val startDay: Long = NO_DAY,
        val startTime: Int = NO_TIME,
        val originalStartDay: Long = NO_DAY,
        val originalStartTime: Int = NO_TIME,
        val datePickerPreferences: DatePickerPreferences = DatePickerPreferences(),
        val addTasksToTop: Boolean = TaskDefaultSettings().addTasksToTop,
        val subtasks: List<SubtaskRow> = emptyList(),
        val subtasksNested: Boolean = false,
        val subtasksChanged: Boolean = false,
        val focusSubtask: String? = null,
        val isDraft: Boolean = false,
        /**
         * True between the row for a new task being created and the save that follows it
         * succeeding. It forces the next save even when nothing has changed since: the row exists,
         * but nothing has marked it dirty yet, so no synchronizer can see it.
         */
        val pendingSideEffects: Boolean = false,
    ) {
        val isNew: Boolean get() = originalTask.isNew
        val hasChanges: Boolean
            get() = list != originalList ||
                    tags.toHashSet() != originalTags.toHashSet() ||
                    alarmsChanged ||
                    startChanged ||
                    subtasksChanged ||
                    !task.copy(hideUntil = originalTask.hideUntil).sameEditableContentAs(originalTask)

        val allowsNesting: Boolean
            get() = listOfNotNull(originalList, list).let { lists ->
                lists.isEmpty() || lists.any { !it.isSingleLevel }
            }

        internal val alarmsChanged: Boolean
            get() = !alarms.sameAlarmsAs(originalAlarms)

        internal val alarmsNeedSaving: Boolean
            get() = applicableAlarms().let {
                if (isNew) it.isNotEmpty() else !it.sameAlarmsAs(originalAlarms)
            }

        internal fun applicableAlarms(): ImmutableSet<Alarm> =
            alarms.applicableTo(task).toPersistentSet()

        private val startChanged: Boolean
            get() = task.hideUntil != originalTask.hideUntil &&
                    task.hideUntil != resolveStartDate(
                        startDayOf(originalStartDay), originalStartTime, task.dueDate,
                    )
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    // Replayed, because the editor's nav entry is routinely not composed when a close is decided:
    // pushing settings over an open task leaves the view model alive and its screen - and so the
    // collector - gone. A replay-0 emission there is dropped, and the watch that produced it never
    // repeats itself, so the user comes back to an editor that silently discards everything typed
    // into it.
    //
    // Buffered so that emitting never suspends. A close is emitted from under the per-task save
    // lock, and shutdown waits on that same save - so an emit that waited for a collector which by
    // then may not exist would hold the lock for the whole of that wait. Dropping the older of two
    // queued closes costs nothing: they carry no payload.
    private val _closeEvents = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val closeEvents: SharedFlow<Unit> = _closeEvents.asSharedFlow()

    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

    /** True while a [save] is waiting on the per-task lock or writing. */
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val pickerModeMutex = Mutex()

    private val watchMutex = Mutex()
    private val watchedTaskIds = mutableMapOf<String, Long>()
    private val unregisterFlushHandler: () -> Unit

    private val treeKey = MutableStateFlow(saveKey)

    private val lockKey: String get() = saveKey

    private fun retainTree(key: String) {
        treeKey.value = key
    }

    private fun releaseTree() {
        if (ownsTree) {
            treeRegistry.close(subtaskTrees)
        }
    }

    private var draft: SubtaskNode? = null

    private var draftSealed = false

    private var adopting = false

    private val staged = StagedSubtaskEdits(subtaskTrees) { treeKey.value }

    private val loader = TaskEditLoader(
        taskId = this.taskId,
        uuid = uuid,
        listId = listId,
        tagUuid = tagUuid,
        isSubtaskDraft = isSubtaskDraft,
        taskDao = taskDao,
        caldavDao = caldavDao,
        tagDataDao = tagDataDao,
        alarmDao = alarmDao,
        appPreferences = appPreferences,
        taskCreator = taskCreator,
        log = log,
    )

    init {
        retainTree(saveKey)
        load()
        watchSubtaskTree()
        watchSubtasks()
        watchPendingEdits()
        // Quitting the desktop app tears down the JVM while this editor is still composed, so
        // nothing has cleared or stopped it. Shutdown asks for the commit through here instead, and
        // so does an editor loading this same task - see load().
        unregisterFlushHandler = pendingSaves.registerFlushHandler(saveKey) { persistCurrentTask() }
    }

    private fun load() {
        viewModelScope.launch {
            _loadError.value = false
            _state.value = State(isLoading = true)
            try {
                subtaskTrees.get(uuid)?.takeIf { it.isNew }?.let { existing ->
                    draft = existing
                    retainTree(existing.key)
                    _state.value = existing
                        .toState(appPreferences.datePickerPreferences())
                        .withTree(existing.key)
                    return@launch
                }
                // Any editor still alive on this same task is holding an edit that is not in the
                // row yet: the nav host builds this editor before it disposes the one it replaces.
                // Asked for first, so it runs while this reads preferences.
                pendingSaves.flushPending(saveKey)
                // Read before waiting. This has nothing to do with the row, and on composeApp it is
                // an unbounded DataStore read - holding a task's save lock across it blocks the
                // departing editor's teardown save, on desktop past the shutdown timeout, which
                // loses the edit outright.
                val prefs = appPreferences.datePickerPreferences()
                val defaults = appPreferences.taskDefaults()
                // The ordering the lock alone doesn't provide. A save that couldn't claim the lock
                // synchronously only joins its queue once a worker thread picks it up, so a withLock
                // here could still queue ahead of the flush above and read the pre-save row. The
                // flush's own bookkeeping is what this waits on instead.
                pendingSaves.awaitPending(saveKey)
                // Still taken, so the read can't interleave with a save enqueued after the flush.
                val loaded = pendingSaves.withLock(lockKey) { loader.read(prefs, defaults) }
                retainTree(subtaskKey(loaded.task))
                _state.value = loaded.withStagedTitle().withTree(treeKey.value)
                // The row this editor was opened on is a tombstone: there is nothing to edit, and
                // staying would let the teardown save write onto a deleted row.
                if (loaded.deleted) {
                    _closeEvents.emit(Unit)
                    return@launch
                }
                watchTask(loaded.task.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Failed to initialize task editor" }
                _loadError.value = true
                _state.value = State(isLoading = false)
            }
        }
    }

    private fun watchSubtaskTree() {
        combine(subtaskTrees.nodes, treeKey) { nodes, key -> nodes to key }
            .distinctUntilChanged()
            .onEach { (nodes, key) ->
                _state.update { if (it.isLoading) it else it.withTree(key, nodes) }
            }
            .launchIn(viewModelScope)
    }

    private fun State.withTree(
        key: String,
        nodes: Map<String, SubtaskNode> = subtaskTrees.nodes.value,
    ): State = nodes.rowsOf(key).let { rows ->
        copy(
            subtasks = rows.visible(),
            subtasksNested = rows.nested(),
            subtasksChanged = nodes.isRearranged(key),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun watchSubtasks() {
        combine(
            _state.map { it.task.id }.distinctUntilChanged(),
            _state.map { it.list?.isGoogleTasks == true }.distinctUntilChanged(),
            refreshFlow.onStart { emit(Unit) },
        ) { id, isGoogleTasks, _ -> id to isGoogleTasks }
            .mapLatest { (id, isGoogleTasks) ->
                if (id <= 0) {
                    return@mapLatest
                }
                val rows = fetchSubtasks(id, isGoogleTasks) ?: return@mapLatest
                subtaskTrees.merge(treeKey.value, id, rows)
            }
            .launchIn(viewModelScope)
    }

    private suspend fun fetchSubtasks(id: Long, isGoogleTasks: Boolean): List<TaskContainer>? =
        try {
            taskDao.fetchTasks(subtaskQuery(parentId = id, isGoogleTasks = isGoogleTasks))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Failed to load subtasks" }
            null
        }

    private fun watchPendingEdits() {
        _state
            .onEach { state ->
                if (state.isDraft && !state.isLoading) {
                    writePending(state)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun writePending(state: State) {
        if (draftSealed) {
            return
        }
        val key = draft?.key ?: return
        var settled: SubtaskNode? = null
        val written = subtaskTrees.update(key) { identity ->
            if (!identity.isNew) {
                settled = identity
                return@update identity
            }
            identity.copy(
                task = state.task,
                stagedTitle = null,
                stagedCompleted = null,
                pending = PendingTask(
                    list = state.list,
                    tags = state.tags,
                    alarms = state.alarms,
                    startDay = state.startDay,
                    startTime = state.startTime,
                ),
            )
        }
        if (written == null || settled != null) {
            adoptCreatedRow(settled?.task)
        }
    }

    private fun adoptCreatedRow(row: Task?) {
        if (adopting) {
            return
        }
        adopting = true
        viewModelScope.launch {
            val created = row?.takeIf { it.id > 0 }
                ?: uuid?.let { runCatching { taskDao.fetch(it) }.getOrNull() }
            if (created == null || created.id <= 0) {
                adopting = false
                return@launch
            }
            draft = null
            _state.update { state ->
                if (!state.isDraft) {
                    return@update state
                }
                state.copy(
                    task = state.task.copy(
                        id = created.id,
                        creationDate = created.creationDate,
                        modificationDate = created.modificationDate,
                        remoteId = created.remoteId,
                        parent = created.parent,
                        order = created.order,
                    ),
                    originalTask = created.copy(),
                    isDraft = false,
                )
            }
            watchTask(created.id)
        }
    }

    private fun State.withStagedTitle(): State {
        val staged = nodeTree()?.get(treeKey.value)?.stagedTitle ?: return this
        return if (staged == task.title) this else copy(task = task.copy(title = staged))
    }

    private fun SubtaskNode.toState(prefs: DatePickerPreferences): State {
        val drafted = task.copy(title = title).withCompletion(completed, currentTimeMillis())
        return State(
            isLoading = false,
            task = drafted,
            originalTask = drafted.copy(),
            list = pending?.list,
            originalList = pending?.list,
            tags = pending?.tags.orEmpty(),
            originalTags = pending?.tags.orEmpty(),
            alarms = pending?.alarms ?: persistentSetOf(),
            originalAlarms = pending?.alarms ?: persistentSetOf(),
            startDay = pending?.startDay ?: NO_DAY,
            startTime = pending?.startTime ?: NO_TIME,
            originalStartDay = pending?.startDay ?: NO_DAY,
            originalStartTime = pending?.startTime ?: NO_TIME,
            datePickerPreferences = prefs,
            isDraft = true,
        )
    }

    fun addSubtask(after: SubtaskNode? = null) {
        val task = taskCreator.createBlankTask()
        val list = _state.value.list
        val sibling = after?.let { subtaskTrees.get(it.key) }
        val node = if (sibling != null) {
            subtaskTrees.addAfter(sibling = sibling, task = task, list = list)
        } else {
            subtaskTrees.add(rootKey = treeKey.value, task = task, list = list)
        }
        staged.added(node.key)
        _state.update {
            if (it.isLoading) it else it.withTree(treeKey.value).copy(focusSubtask = node.key)
        }
        viewModelScope.launch {
            val defaults = try {
                task.copy().apply { setDefaultReminders(appPreferences) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Failed to read default reminders" }
                return@launch
            }
            val alarms = defaults
                .getDefaultAlarms(appPreferences.isDefaultDueTimeEnabled())
                .toPersistentSet()
            subtaskTrees.update(node.key) { current ->
                val pending = current.pending ?: return@update current
                val ringFlags = defaults.ringFlags.takeIf { it != current.task.ringFlags }
                val takesAlarms = alarms.isNotEmpty() && pending.alarms.isEmpty()
                if (ringFlags == null && !takesAlarms) {
                    return@update current
                }
                current.copy(
                    task = if (ringFlags == null) {
                        current.task
                    } else {
                        current.task.copy(ringFlags = ringFlags)
                    },
                    pending = if (takesAlarms) pending.copy(alarms = alarms) else pending,
                )
            }
        }
    }

    fun onSubtaskFocused(key: String) {
        _state.update { if (it.focusSubtask == key) it.copy(focusSubtask = null) else it }
    }

    fun setSubtaskTitle(node: SubtaskNode, title: String) {
        staged.edited(subtaskTrees.setTitle(node.key, title))
        refreshSubtasks()
    }

    private fun refreshSubtasks() {
        val key = treeKey.value
        _state.update { if (it.isLoading) it else it.withTree(key) }
    }

    fun toggleSubtaskComplete(node: SubtaskNode) {
        val completed = _state.value.subtasks.firstOrNull { it.key == node.key }?.completed
            ?: node.completed
        staged.edited(subtaskTrees.setCompleted(key = node.key, completed = !completed))
        refreshSubtasks()
    }

    fun removeSubtask(node: SubtaskNode) {
        staged.deletionStaged(node.key)
        subtaskTrees.delete(node.key)
        refreshSubtasks()
    }

    fun backspaceSubtask(node: SubtaskNode) {
        if (node.isNew) {
            subtaskTrees.drop(node.key)
            staged.dropped(node.key)
        } else {
            staged.deletionStaged(node.key)
            staged.edited(subtaskTrees.revertTitle(node.key))
            subtaskTrees.delete(node.key)
        }
        refreshSubtasks()
    }

    fun restoreSubtask(node: SubtaskNode) {
        staged.deletionStaged(node.key)
        subtaskTrees.restore(node.key)
        refreshSubtasks()
    }

    fun moveSubtask(fromKey: String, toKey: String, indent: Int? = null) {
        val rows = _state.value.subtasks
        val from = rows.indexOfFirst { it.key == fromKey }
        val to = rows.indexOfFirst { it.key == toKey }
        val landing = rows.resolveMove(
            from = from,
            to = to,
            rootKey = treeKey.value,
            desiredIndent = indent,
        ) ?: return
        staged.rememberArrangement()
        subtaskTrees.move(key = fromKey, parentKey = landing.parentKey, after = landing.after)
        refreshSubtasks()
    }

    fun indentSubtask(node: SubtaskNode, steps: Int) {
        if (steps > 0 && !_state.value.allowsNesting) {
            return
        }
        repeat(kotlin.math.abs(steps)) {
            val before = subtaskTrees.arrangementUnder(treeKey.value)
            val moved = if (steps > 0) {
                subtaskTrees.indent(node.key)
            } else {
                subtaskTrees.outdent(rootKey = treeKey.value, key = node.key)
            }
            if (!moved) {
                refreshSubtasks()
                return
            }
            staged.rememberArrangement(before)
        }
        refreshSubtasks()
    }

    fun toggleSubtaskCollapsed(node: SubtaskNode) {
        val collapsed = !node.task.isCollapsed
        subtaskTrees.setCollapsed(node.key, collapsed)
        refreshSubtasks()
        val id = node.id
        if (id <= 0) {
            return
        }
        viewModelScope.launch {
            try {
                taskSaver.setCollapsed(id, collapsed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Failed to collapse subtask" }
                subtaskTrees.setCollapsed(node.key, !collapsed)
                refreshSubtasks()
            }
        }
    }

    /**
     * Watches the task row for external edits and deletes.
     *
     * Called again after a save because a task created by *this* editor has no row to watch when it
     * loads: without the second call an external delete never reaches [State.deleted] and the
     * teardown save writes `deletionDate` back to zero, resurrecting the task.
     */
    private suspend fun watchTask(id: Long) {
        if (id <= 0) return
        watchStream(id, "task") {
            taskDao.watch(id)
                .filterNotNull()
                .distinctUntilChanged()
                .collect { dbTask -> mergeDbUpdate(dbTask) }
        }
        watchStream(id, "alarms") {
            alarmDao.watchAlarms(id)
                .distinctUntilChanged()
                .collect { dbAlarms -> mergeAlarmUpdate(dbAlarms) }
        }
    }

    private suspend fun watchStream(id: Long, what: String, collect: suspend () -> Unit) {
        val claimed = watchMutex.withLock {
            if (watchedTaskIds[what] == id) false else { watchedTaskIds[what] = id; true }
        }
        if (!claimed) return
        viewModelScope.launch { watchWithRetry(id, what, collect) }
    }

    private suspend fun watchWithRetry(id: Long, what: String, collect: suspend () -> Unit) {
        var attempts = 0
        while (true) {
            try {
                collect()
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (++attempts >= WATCH_MAX_ATTEMPTS) {
                    log.e(e) { "Gave up watching $what for $id after $attempts attempts" }
                    watchMutex.withLock {
                        if (watchedTaskIds[what] == id) {
                            watchedTaskIds.remove(what)
                        }
                    }
                    return
                }
                log.e(e) { "Failed to watch $what for $id, retrying" }
                delay(WATCH_RETRY_DELAY_MS)
            }
        }
    }

    suspend fun saveCurrentTask(): Boolean = withContext(NonCancellable) {
        // Checked before taking the lock, not just inside it: there is nothing to write yet, and
        // load() holds this same lock, so waiting for it would leave the editor unable to close
        // while the row is still being read.
        if (_state.value.isLoading) return@withContext true
        if (_state.value.isDraft) {
            writePending(_state.value)
            return@withContext true
        }
        val outcome = pendingSaves.withLock(lockKey) { saveWhileLocked() }
        watchTask(outcome.taskId)
        outcome.succeeded
    }

    fun persistCurrentTask(): Boolean = persistCurrentTask(andThen = null)

    private fun persistCurrentTask(andThen: (() -> Unit)?): Boolean {
        val current = _state.value
        if (current.isLoading) {
            return false
        }
        if (current.isDraft) {
            writePending(current)
            return false
        }
        if (!current.hasChanges && !current.pendingSideEffects) {
            return false
        }
        pendingSaves.enqueueLocked(key = saveKey) {
            try {
                if (_state.value.isLoading) return@enqueueLocked
                val outcome = saveWhileLocked()
                watchTask(outcome.taskId)
            } finally {
                andThen?.invoke()
            }
        }
        return true
    }

    // Public so tests can drive the teardown save; the framework calls this via clear().
    public override fun onCleared() {
        unregisterFlushHandler()
        if (!persistCurrentTask(andThen = ::releaseTree)) {
            releaseTree()
        }
    }

    private data class SaveOutcome(val succeeded: Boolean, val taskId: Long)

    /** The save itself. The caller must already hold [saveKey]'s lock. */
    private suspend fun saveWhileLocked(): SaveOutcome {
        if (_state.value.isLoading) return SaveOutcome(true, _state.value.task.id)
        return try {
            // Re-read the row here rather than trusting the watch to have caught up. The watch is a
            // separate coroutine hop, so it can be arbitrarily behind - and after a failure it may
            // not be armed at all. Without this, a delete that has committed but not been merged yet
            // is written straight back: the full-row update below carries deletionDate 0, and
            // because the dirty check compares against the pre-delete original the resurrected row
            // is pushed to the server too. It also means an external edit is merged instead of being
            // overwritten.
            //
            // Inside the try deliberately, so that a re-read which throws aborts the save. Logging
            // it and carrying on performed exactly the overwrite this read exists to prevent, on a
            // row nothing had checked; failing closed reports it like any other save failure, which
            // keeps the editor open with the edit still in it.
            refreshFromDb(_state.value.task.id)
            val snapshot = _state.value
            val persisted = saveIfNeeded(snapshot)
            if (persisted != null) {
                _state.update {
                    // The save works on its own copy, and the DAO stamps that copy: createNew
                    // writes the row id back, applyTags bumps the modification date. An edit
                    // landing while the save ran replaced state.task with a copy of the
                    // pre-save one, so both have to be carried across by hand - otherwise the
                    // editor keeps writing against id 0, and hasChanges never settles.
                    val task = it.task.copy(
                        id = persisted.id,
                        modificationDate = persisted.modificationDate,
                    )
                    it.copy(
                        task = task,
                        originalTask = persisted.copy(),
                        originalList = snapshot.list,
                        originalTags = snapshot.tags,
                        alarms = it.alarms.applicableTo(task).toPersistentSet(),
                        originalAlarms = if (snapshot.alarmsNeedSaving) {
                            snapshot.applicableAlarms()
                        } else {
                            it.originalAlarms
                        },
                        originalStartDay = snapshot.startDay,
                        originalStartTime = snapshot.startTime,
                        pendingSideEffects = false,
                    )
                }
            }
            // Creating the task is what gives it a row to watch, so this is the first chance to.
            // Taken from the row that was actually written, not from live state: an edit landing
            // mid-save swaps in a task instance that predates the id being stamped.
            SaveOutcome(true, persisted?.id ?: snapshot.task.id)
        } catch (e: CancellationException) {
            // Rethrown first, as everywhere else in this file. On jvmCommon CancellationException is
            // a RuntimeException, so the catch below would otherwise turn a cancelled save into a
            // reported failure - a snackbar, an editor that refuses to close, and on desktop a quit
            // that turns itself back. Both callers sit inside NonCancellable today; the re-read now
            // inside this try is one more suspension point that must not start relying on that.
            throw e
        } catch (e: Exception) {
            log.e(e) { "Failed to save task" }
            pendingSaves.reportSaveFailure()
            SaveOutcome(false, _state.value.task.id)
        }
    }

    /**
     * Merges the row as it currently stands in the database into state. Called under [saveKey]'s
     * lock, immediately before writing. Throws if the row can't be read, which aborts the save -
     * see [saveWhileLocked].
     */
    private suspend fun refreshFromDb(id: Long) {
        if (id <= 0) return
        val row = taskDao.fetch(id)
        if (row == null) {
            // Hard-deleted - a sync purge, or the account it belonged to being removed. There is no
            // row left to update, and the full-row update would silently match nothing.
            _state.update { it.copy(deleted = true) }
            // Closed here as well as from the watch. The watch drops nulls, so a hard delete never
            // reaches it and this is the only place that sees one; leaving the editor open left it
            // looking untouched while every later save short-circuited on state.deleted and reported
            // success having written nothing.
            _closeEvents.emit(Unit)
            return
        }
        // Same close the watch performs when it merges a soft delete. Discarding it left a save-time
        // delete detected but never acted on.
        if (applyDbUpdate(row)) {
            _closeEvents.emit(Unit)
        }
    }

    private suspend fun mergeAlarmUpdate(dbAlarms: List<Alarm>) {
        val alarms = dbAlarms.toPersistentSet()
        pendingSaves.withLock(lockKey) {
            _state.update { state ->
                if (state.isLoading) {
                    state
                } else {
                    state.copy(
                        alarms = mergeAlarms(state.alarms, state.originalAlarms, alarms),
                        originalAlarms = alarms,
                    )
                }
            }
        }
    }

    private suspend fun mergeDbUpdate(dbTask: Task) {
        val shouldClose = pendingSaves.withLock(lockKey) { applyDbUpdate(dbTask) }
        if (shouldClose) {
            _closeEvents.emit(Unit)
        }
    }

    private fun applyDbUpdate(dbTask: Task): Boolean =
        _state.updateAndGet { it.mergedWith(dbTask) }.deleted

    fun setTitle(title: String) {
        _state.update { it.copy(task = it.task.copy(title = title)) }
    }

    fun setDescription(description: String) {
        _state.update { it.copy(task = it.task.copy(notes = description)) }
    }

    fun setPriority(priority: Int) {
        _state.update { it.copy(task = it.task.copy(priority = priority)) }
    }

    fun setDueDate(value: Long) {
        val dueDate = value.withTimeMarkerOr { it.noon() }
        val previous = _state.value.task.dueDate
        val previousStart = _state.value.task.hideUntil
        _state.update { it.withStartSelection(it.startDay, it.startTime, dueDate) }
        addDefaultAlarmsForDates(previous, previousStart)
        onDueDateChanged()
    }

    fun setRecurrence(recurrence: String?) {
        val previousDue = _state.value.task.dueDate
        val previousStart = _state.value.task.hideUntil
        applyRecurrence(recurrence)
        addDefaultAlarmsForDates(previousDue, previousStart)
    }

    private fun applyRecurrence(recurrence: String?) {
        _state.update { state ->
            val dueDate = if (!recurrence.isNullOrBlank() && state.task.dueDate == 0L) {
                currentTimeMillis().startOfDay()
            } else {
                state.task.dueDate
            }
            state
                .withStartSelection(state.startDay, state.startTime, dueDate)
                .let { it.copy(task = it.task.copy(recurrence = recurrence)) }
        }
    }

    private fun addDefaultAlarmsForDates(previousDue: Long, previousStart: Long) {
        val state = _state.value
        addDefaultAlarms(TYPE_REL_END, previousDue, state.task.dueDate)
        if (state.isNew) {
            addDefaultAlarms(TYPE_REL_START, previousStart, state.task.hideUntil)
        }
    }

    fun setRepeatFrom(repeatFrom: @Task.RepeatFrom Int) {
        _state.update { it.copy(task = it.task.copy(repeatFrom = repeatFrom)) }
    }

    private fun onDueDateChanged() {
        val state = _state.value
        val recurrence = state.task.recurrence?.takeIf { it.isNotBlank() } ?: return
        val recur = try {
            newRecur(recurrence)
        } catch (e: Exception) {
            log.e(e) { "Failed to parse $recurrence" }
            return
        }
        if (recur.frequency != Recur.Frequency.MONTHLY || recur.dayList.isEmpty()) {
            return
        }
        val weekdayNum = recur.dayList[0]
        val dateTime = DateTime(state.task.dueDate.takeIf { it > 0 } ?: currentTimeMillis())
        val dayOfWeekInMonth = dateTime.dayOfWeekInMonth
        val num = if (weekdayNum.offset == -1 || dayOfWeekInMonth == 5) {
            if (dayOfWeekInMonth == dateTime.maxDayOfWeekInMonth) -1 else dayOfWeekInMonth
        } else {
            dayOfWeekInMonth
        }
        recur.dayList.let {
            it.clear()
            it.add(WeekDay(dateTime.weekDay, num))
        }
        applyRecurrence(recur.toString())
    }

    fun setStartDate(day: Long, time: Int) {
        val previous = _state.value.task.hideUntil
        _state.update { it.withStartSelection(day, time, it.task.dueDate) }
        addDefaultAlarms(TYPE_REL_START, previous, _state.value.task.hideUntil)
    }

    private fun addDefaultAlarms(type: Int, previous: Long, current: Long) {
        val hadDate = previous > 0
        val addedDate = !hadDate && current > 0
        val addedTime = hadDate && !Task.hasDueTime(previous) && Task.hasDueTime(current)
        if (!addedDate && !addedTime) return
        viewModelScope.launch {
            try {
                val defaultTime = appPreferences.isDefaultDueTimeEnabled()
                val wanted = if (addedDate) {
                    Task.hasDueTime(current) || defaultTime
                } else {
                    !defaultTime
                }
                if (!wanted) return@launch
                appPreferences.defaultAlarms()
                    .filter { it.type == type }
                    .forEach { addAlarm(it.copy(id = 0, task = 0)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Failed to read default reminders" }
            }
        }
    }

    private fun State.withStartSelection(day: Long, time: Int, dueDate: Long): State =
        copy(
            startDay = day,
            startTime = time,
            task = task.copy(
                dueDate = dueDate,
                hideUntil = resolveStartDate(startDayOf(day), time, dueDate),
            ),
        )

    fun setDatePickerInputMode(inputMode: Boolean) = setPickerInputMode(
        inputMode = inputMode,
        apply = { prefs, value -> prefs.copy(datePickerInputMode = value) },
        read = { it.datePickerInputMode },
        write = { appPreferences.setDatePickerInputMode(it) },
    )

    fun setTimePickerInputMode(inputMode: Boolean) = setPickerInputMode(
        inputMode = inputMode,
        apply = { prefs, value -> prefs.copy(timePickerInputMode = value) },
        read = { it.timePickerInputMode },
        write = { appPreferences.setTimePickerInputMode(it) },
    )

    private fun setPickerInputMode(
        inputMode: Boolean,
        apply: (DatePickerPreferences, Boolean) -> DatePickerPreferences,
        read: (DatePickerPreferences) -> Boolean,
        write: suspend (Boolean) -> Unit,
    ) {
        val previous = read(_state.value.datePickerPreferences)
        _state.update { it.copy(datePickerPreferences = apply(it.datePickerPreferences, inputMode)) }
        externalScope.launch {
            pickerModeMutex.withLock {
                try {
                    write(inputMode)
                } catch (e: Exception) {
                    log.e(e) { "Failed to persist picker input mode" }
                    _state.update { it.copy(datePickerPreferences = apply(it.datePickerPreferences, previous)) }
                }
            }
        }
    }

    fun setList(list: CaldavFilter) {
        _state.update { it.copy(list = list) }
        subtaskTrees.setList(treeKey.value, list)
        refreshSubtasks()
    }

    fun setTags(tags: List<TagData>) {
        _state.update { it.copy(tags = tags) }
    }

    fun addAlarm(alarm: Alarm) {
        _state.update { state ->
            if (state.alarms.any { it.same(alarm) }) {
                state
            } else {
                state.copy(alarms = state.alarms.plus(alarm).toPersistentSet())
            }
        }
    }

    fun removeAlarm(alarm: Alarm) {
        _state.update { state ->
            state.copy(alarms = state.alarms.filterNot { it.same(alarm) }.toPersistentSet())
        }
    }

    fun save() {
        if (!_saving.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                if (saveCurrentTask()) {
                    _closeEvents.emit(Unit)
                }
            } finally {
                _saving.value = false
            }
        }
    }

    fun markComplete() {
        if (!_saving.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                if (_state.value.isDraft) {
                    writePending(_state.value)
                    draft?.key?.let { key ->
                        draftSealed = true
                        staged.edited(
                            subtaskTrees.setCompleted(key = key, completed = true)
                        )
                    }
                    _closeEvents.emit(Unit)
                    return@launch
                }
                if (!saveCurrentTask()) return@launch
                val snapshot = _state.value
                if (snapshot.task.id > 0 && !snapshot.deleted) {
                    try {
                        pendingSaves.withLock(lockKey) {
                            taskCompleter.setComplete(snapshot.task.id, true)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log.e(e) { "Failed to complete task" }
                        pendingSaves.reportSaveFailure()
                        return@launch
                    }
                }
                _closeEvents.emit(Unit)
            } finally {
                _saving.value = false
            }
        }
    }

    fun delete() {
        if (!_saving.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                val above = nodeTree()
                if (above != null) {
                    if (_state.value.isDraft) {
                        draftSealed = true
                    }
                    above.delete(treeKey.value)
                    _state.update { it.copy(deleted = true) }
                    _closeEvents.emit(Unit)
                    return@launch
                }
                val id = _state.value.task.id
                if (id > 0) {
                    try {
                        pendingSaves.withLock(lockKey) {
                            _state.update { it.copy(deleted = true) }
                            taskDeleter.markDeleted(listOf(id))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log.e(e) { "Failed to delete task" }
                        _state.update { it.copy(deleted = false) }
                        pendingSaves.reportSaveFailure()
                        return@launch
                    }
                } else {
                    _state.update { it.copy(deleted = true) }
                }
                dropSubtasks()
                _closeEvents.emit(Unit)
            } finally {
                _saving.value = false
            }
        }
    }

    fun discardChanges() {
        if (_state.value.isDraft) {
            discardDraft()
            viewModelScope.launch { _closeEvents.emit(Unit) }
            return
        }
        _state.update { state ->
            if (state.isLoading) return@update state
            state.copy(
                task = state.originalTask.copy(),
                list = state.originalList,
                tags = state.originalTags,
                alarms = state.originalAlarms,
                startDay = state.originalStartDay,
                startTime = state.originalStartTime,
            )
        }
        staged.discard()
        refreshSubtasks()
        viewModelScope.launch { _closeEvents.emit(Unit) }
    }

    private fun discardDraft() {
        draftSealed = true
        staged.discard()
        draft?.let { subtaskTrees.drop(it.key) }
    }

    private fun dropSubtasks() {
        staged.clear()
        if (ownsTree) {
            subtaskTrees.clear()
        }
    }

    private suspend fun saveIfNeeded(snapshot: State): Task? {
        val list = snapshot.list ?: return null
        if (snapshot.deleted) return null
        if (!snapshot.hasChanges && !snapshot.pendingSideEffects) return null
        val supersededTitle = nodeTree()?.get(treeKey.value)?.stagedTitle
        val task = snapshot.task.copy()
        if (task.title.isNullOrBlank()) {
            task.title = untitled()
        }
        // TODO: apply calendar changes
        if (snapshot.isNew) {
            taskDao.inTransaction {
                taskDao.createNew(task)
                caldavDao.insert(
                    task = task,
                    caldavTask = CaldavTask(task = task.id, calendar = list.uuid),
                    addToTop = snapshot.addTasksToTop,
                )
                applyTagsIfNeeded(snapshot, task)
            }
            applyAlarmsIfNeeded(snapshot, task)
            _state.update {
                it.copy(
                    task = it.task.copy(
                        id = task.id,
                        order = task.order,
                        modificationDate = task.modificationDate,
                    ),
                    originalTask = task.copy(),
                    pendingSideEffects = true,
                )
            }
            taskSaver.save(task, null)
        } else {
            applyTagsIfNeeded(snapshot, task)
            applyAlarmsIfNeeded(snapshot, task)
            taskSaver.save(
                task,
                snapshot.originalTask.takeUnless { snapshot.pendingSideEffects },
                preserveHierarchy = true,
            )
            if (snapshot.list != snapshot.originalList) {
                taskMover.move(listOf(task.id), list)
            }
        }
        writeSubtasks(task, list, supersededTitle)
        return task
    }

    private suspend fun writeSubtasks(task: Task, list: CaldavFilter, supersededTitle: String?) {
        val key = treeKey.value
        val result = subtaskWriter.write(
            trees = subtaskTrees,
            rootKey = key,
            parent = task,
            list = list,
            untitled = untitled(),
        )
        subtaskTrees.clearWritten(result.staged)
        staged.clearWritten(result.staged.map { it.key })
        val read = result.read
        if (!result.wrote && read != null) {
            subtaskTrees.merge(key, task.id, read)
        }
        nodeTree()?.update(key) { node ->
            node.copy(
                task = task.copy(),
                stagedTitle = node.stagedTitle?.takeUnless { it == supersededTitle },
            )
        }
    }

    private suspend fun applyAlarmsIfNeeded(snapshot: State, task: Task) {
        if (!snapshot.alarmsNeedSaving) return
        alarmService.synchronizeAlarms(task.id, snapshot.applicableAlarms().toMutableSet())
        task.putTransitory(SYNC_ALARMS, true)
        task.modificationDate = currentTimeMillis()
    }

    private suspend fun applyTagsIfNeeded(snapshot: State, task: Task) {
        val selected = snapshot.tags
        val changed = snapshot.originalTags.toHashSet() != selected.toHashSet()
        if ((snapshot.isNew && selected.isNotEmpty()) || changed) {
            tagDao.applyTags(task, selected)
            task.putTransitory(SYNC_TAGS, true)
            task.modificationDate = currentTimeMillis()
        }
    }
}

private fun Task.withCompletion(completed: Boolean, at: Long): Task = when {
    completed == isCompleted -> this
    completed -> copy(completionDate = at)
    else -> copy(completionDate = 0L)
}
