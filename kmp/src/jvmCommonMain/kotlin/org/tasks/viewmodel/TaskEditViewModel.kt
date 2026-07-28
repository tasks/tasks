package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tasks.compose.pickers.NO_DAY
import org.tasks.compose.pickers.NO_TIME
import org.tasks.compose.pickers.initialStartSelection
import org.tasks.compose.pickers.resolveStartDate
import org.tasks.compose.pickers.startDayOf
import org.tasks.compose.pickers.startSelectionDays
import org.tasks.compose.pickers.withTimeMarkerOr
import org.tasks.data.TaskCreator
import org.tasks.data.TaskMover
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.SYNC_TAGS
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.DatePickerPreferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.noon
import java.util.concurrent.atomic.AtomicLong

private const val WATCH_MAX_ATTEMPTS = 5
private const val WATCH_RETRY_DELAY_MS = 1_000L

/** Distinguishes editors on a destination that names no task at all - see [TaskEditViewModel]. */
private val anonymousEditors = AtomicLong()

class TaskEditViewModel(
    taskId: Long,
    private val remoteId: String,
    private val listId: Long?,
    private val tagUuid: String?,
    private val taskDao: TaskDao,
    private val taskSaver: TaskSaver,
    private val caldavDao: CaldavDao,
    private val taskMover: TaskMover,
    private val tagDao: TagDao,
    private val tagDataDao: TagDataDao,
    private val appPreferences: AppPreferences,
    private val externalScope: CoroutineScope,
    private val pendingSaves: PendingTaskSaves,
    private val taskCreator: TaskCreator = TaskCreator(),
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

    data class State(
        val isLoading: Boolean = true,
        val task: Task = Task(),
        val originalTask: Task = Task(),
        val list: CaldavFilter? = null,
        val originalList: CaldavFilter? = null,
        val tags: List<TagData> = emptyList(),
        val originalTags: List<TagData> = emptyList(),
        val deleted: Boolean = false,
        val startDay: Long = NO_DAY,
        val startTime: Int = NO_TIME,
        val originalStartDay: Long = NO_DAY,
        val originalStartTime: Int = NO_TIME,
        val datePickerPreferences: DatePickerPreferences = DatePickerPreferences(),
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
                    startChanged ||
                    !task.copy(hideUntil = originalTask.hideUntil).sameEditableContentAs(originalTask)

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
    // lock, and on Android the stop that drives that save blocks the main thread waiting for it - so
    // an emit that waited for the collector would be waiting on the very thread that is waiting for
    // it. Dropping the older of two queued closes costs nothing: they carry no payload.
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

    // Guards watchedTaskId. watchTask is reached from the main thread through load() and from
    // PendingTaskSaves' Dispatchers.Default scope through a teardown save, with no happens-before
    // edge between them, so a plain check-then-set on a non-volatile field armed two collectors on
    // the same row - doubling every merge round-trip and every close emitted on a remote delete.
    private val watchMutex = Mutex()
    private var watchedTaskId: Long? = null
    private val unregisterFlushHandler: () -> Unit

    init {
        load()
        // Quitting the desktop app tears down the JVM while this editor is still composed, so
        // nothing has cleared or stopped it. Shutdown asks for the commit through here instead, and
        // so does an editor loading this same task - see load().
        unregisterFlushHandler = pendingSaves.registerFlushHandler(saveKey) { persistCurrentTask() }
    }

    private fun load() {
        val normalized = taskId
        viewModelScope.launch {
            _loadError.value = false
            _state.value = State(isLoading = true)
            try {
                // Any editor still alive on this same task is holding an edit that is not in the
                // row yet: the nav host builds this editor before it disposes the one it replaces.
                // Asked for first, so it runs while this reads preferences.
                pendingSaves.flushPending(saveKey)
                // Read before waiting. This has nothing to do with the row, and on composeApp it is
                // an unbounded DataStore read - holding a task's save lock across it blocks the
                // departing editor's teardown save, on desktop past the shutdown timeout, which
                // loses the edit outright.
                val prefs = appPreferences.datePickerPreferences()
                // The ordering the lock alone doesn't provide. A save that couldn't claim the lock
                // synchronously only joins its queue once a worker thread picks it up, so a withLock
                // here could still queue ahead of the flush above and read the pre-save row. The
                // flush's own bookkeeping is what this waits on instead.
                pendingSaves.awaitPending(saveKey)
                // Still taken, so the read can't interleave with a save enqueued after the flush.
                val loaded = pendingSaves.withLock(saveKey) { readTask(normalized, prefs) }
                _state.value = loaded
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

    private suspend fun readTask(normalized: Long?, prefs: DatePickerPreferences): State {
        val loaded: Task
        val list: CaldavFilter?
        val tags: List<TagData>
        if (normalized == null) {
            val existing = uuid?.let { taskDao.fetch(it) }
            if (existing != null) {
                loaded = existing
                coroutineScope {
                    val listDeferred = async { caldavListFor(existing.id) }
                    val tagsDeferred = async { tagDataDao.getTagDataForTask(existing.id) }
                    list = listDeferred.await()
                    tags = tagsDeferred.await()
                }
            } else {
                loaded = uuid
                    ?.let { taskCreator.createBlankTask(remoteId = it) }
                    ?: taskCreator.createBlankTask()
                coroutineScope {
                    val listDeferred = async { seedList() }
                    val tagsDeferred = async { seedTags() }
                    list = listDeferred.await()
                    tags = tagsDeferred.await()
                }
            }
        } else {
            val existing: Task?
            coroutineScope {
                val loadedDeferred = async { taskDao.fetch(normalized) }
                val listDeferred = async { caldavListFor(normalized) }
                val tagsDeferred = async { tagDataDao.getTagDataForTask(normalized) }
                existing = loadedDeferred.await()
                list = listDeferred.await()
                tags = tagsDeferred.await()
            }
            // The row this destination names is gone - hard-deleted by a sync purge or by removing
            // its account, which leaves no tombstone for the check below to find. A blank task here
            // would look like a new one, and it carries a freshly generated remoteId that has
            // nothing to do with the one this editor is locked on, so typing into it would create
            // an unrelated duplicate every time the destination is opened.
            if (existing == null) {
                return State(isLoading = false, deleted = true)
            }
            loaded = existing
        }
        val (startDay, startTime) = initialStartSelection(
            hideUntil = loaded.hideUntil,
            dueDate = loaded.dueDate,
            isNew = loaded.isNew,
            defaultHideUntil = prefs.defaultHideUntil,
        )
        val task = if (loaded.hideUntil <= 0) {
            loaded.copy(hideUntil = resolveStartDate(startDayOf(startDay), startTime, loaded.dueDate))
        } else {
            loaded
        }
        return State(
            isLoading = false,
            task = task,
            originalTask = task.copy(),
            // Neither lookup filters deleted rows, and a new-task destination keeps resolving by
            // remoteId after its own save created the row - so a task deleted in between loads
            // here as if it were live.
            deleted = task.isDeleted,
            list = list,
            originalList = list,
            tags = tags,
            originalTags = tags,
            startDay = startDay,
            startTime = startTime,
            originalStartDay = startDay,
            originalStartTime = startTime,
            datePickerPreferences = prefs,
        )
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
        // Claimed atomically: both call sites are outside the per-task save lock, and they run on
        // different dispatchers.
        val claimed = watchMutex.withLock {
            if (watchedTaskId == id) false else { watchedTaskId = id; true }
        }
        if (!claimed) return
        viewModelScope.launch {
            // The query re-runs on every write to the tasks table, so this outlives the load that
            // started it and can fail long after. Nothing above catches it: viewModelScope's
            // SupervisorJob routes a child failure to the default handler, which on Android takes
            // the process down - from a view model whose screen the user left ages ago.
            //
            // Retried rather than abandoned. The only other place that arms a watch is a save, and
            // that runs after the write it should have been warned about, so a single failure here
            // used to leave the editor blind for as long as it stayed open - and the next save then
            // overwrote whatever it had missed.
            var attempts = 0
            while (true) {
                try {
                    taskDao.watch(id)
                        .filterNotNull()
                        .distinctUntilChanged()
                        .collect { dbTask -> mergeDbUpdate(dbTask) }
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (++attempts >= WATCH_MAX_ATTEMPTS) {
                        log.e(e) { "Gave up watching task $id after $attempts attempts" }
                        // Cleared so a later save can arm it again. Saves re-read the row under the
                        // lock either way, so a blind editor can no longer clobber what it missed.
                        watchMutex.withLock {
                            if (watchedTaskId == id) {
                                watchedTaskId = null
                            }
                        }
                        return@launch
                    }
                    log.e(e) { "Failed to watch task $id, retrying" }
                    delay(WATCH_RETRY_DELAY_MS)
                }
            }
        }
    }

    suspend fun saveCurrentTask(): Boolean = withContext(NonCancellable) {
        // Checked before taking the lock, not just inside it: there is nothing to write yet, and
        // load() holds this same lock, so waiting for it would leave the editor unable to close
        // while the row is still being read.
        if (_state.value.isLoading) return@withContext true
        val outcome = pendingSaves.withLock(saveKey) { saveWhileLocked() }
        watchTask(outcome.taskId)
        outcome.succeeded
    }

    /**
     * Commits the current edit without closing the editor, for the points where the editor can stop
     * being interactive without being destroyed first.
     */
    fun persistCurrentTask() {
        // Nothing to write, so don't enqueue anything: on Android ON_STOP blocks the main thread
        // waiting for whatever this enqueues, and backgrounding with an untouched editor open is
        // the common case. saveIfNeeded would have written nothing either way.
        val current = _state.value
        if (current.isLoading || (!current.hasChanges && !current.pendingSideEffects)) {
            return
        }
        pendingSaves.enqueueLocked(saveKey) {
            if (_state.value.isLoading) return@enqueueLocked
            val outcome = saveWhileLocked()
            watchTask(outcome.taskId)
        }
    }

    // Public so tests can drive the teardown save; the framework calls this via clear().
    public override fun onCleared() {
        unregisterFlushHandler()
        persistCurrentTask()
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
            _state.update {
                it.copy(
                    // The save works on its own copy, and the DAO stamps that copy: createNew
                    // writes the row id back, applyTags bumps the modification date. An edit
                    // landing while the save ran replaced state.task with a copy of the
                    // pre-save one, so both have to be carried across by hand - otherwise the
                    // editor keeps writing against id 0, and hasChanges never settles.
                    task = persisted
                        ?.let { p -> it.task.copy(id = p.id, modificationDate = p.modificationDate) }
                        ?: it.task,
                    originalTask = (persisted ?: snapshot.task).copy(),
                    originalList = snapshot.list,
                    originalTags = snapshot.tags,
                    originalStartDay = snapshot.startDay,
                    originalStartTime = snapshot.startTime,
                    // Only now: the row's side effects have run, so there is nothing left owing.
                    pendingSideEffects = false,
                )
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

    private suspend fun mergeDbUpdate(dbTask: Task) {
        val shouldClose = pendingSaves.withLock(saveKey) { applyDbUpdate(dbTask) }
        if (shouldClose) {
            _closeEvents.emit(Unit)
        }
    }

    private fun applyDbUpdate(dbTask: Task): Boolean =
        _state.updateAndGet { state ->
            if (state.isLoading) return@updateAndGet state
            val current = state.task
            val original = state.originalTask
            if (dbTask.sameEditableContentAs(original)) return@updateAndGet state
            if (dbTask.isDeleted && !original.isDeleted) {
                return@updateAndGet state.copy(deleted = true)
            }
            val merged = current.copy(
                title = merge(current.title, original.title, dbTask.title),
                priority = merge(current.priority, original.priority, dbTask.priority),
                dueDate = merge(current.dueDate, original.dueDate, dbTask.dueDate),
                completionDate = merge(current.completionDate, original.completionDate, dbTask.completionDate),
                deletionDate = merge(current.deletionDate, original.deletionDate, dbTask.deletionDate),
                notes = merge(current.notes, original.notes, dbTask.notes),
                estimatedSeconds = merge(current.estimatedSeconds, original.estimatedSeconds, dbTask.estimatedSeconds),
                elapsedSeconds = merge(current.elapsedSeconds, original.elapsedSeconds, dbTask.elapsedSeconds),
                timerStart = merge(current.timerStart, original.timerStart, dbTask.timerStart),
                ringFlags = merge(current.ringFlags, original.ringFlags, dbTask.ringFlags),
                recurrence = merge(current.recurrence, original.recurrence, dbTask.recurrence),
                repeatFrom = merge(current.repeatFrom, original.repeatFrom, dbTask.repeatFrom),
                calendarURI = merge(current.calendarURI, original.calendarURI, dbTask.calendarURI),
                isCollapsed = merge(current.isCollapsed, original.isCollapsed, dbTask.isCollapsed),
                parent = merge(current.parent, original.parent, dbTask.parent),
                order = merge(current.order, original.order, dbTask.order),
                readOnly = merge(current.readOnly, original.readOnly, dbTask.readOnly),
                modificationDate = dbTask.modificationDate,
                reminderLast = dbTask.reminderLast,
            )
            val start = reconcileStartDate(state, dbTask, merged.dueDate)
            state.copy(
                task = merged.copy(hideUntil = start.hideUntil),
                originalTask = dbTask,
                startDay = start.selectedDay,
                startTime = start.selectedTime,
                originalStartDay = start.baselineDay,
                originalStartTime = start.baselineTime,
            )
        }.deleted

    private fun reconcileStartDate(state: State, dbTask: Task, mergedDueDate: Long): StartReconciliation {
        val localStartDate = startDayOf(state.startDay)
        val startModifiedLocally = state.startDay != state.originalStartDay ||
            state.startTime != state.originalStartTime
        val startModifiedExternally = dbTask.hideUntil != state.originalTask.hideUntil
        val dueModifiedLocally = state.task.dueDate != state.originalTask.dueDate
        val backendStoresStartDate = state.originalList?.account?.syncsStartDate == true
        val keepLocalStart = startModifiedLocally ||
            (dueModifiedLocally && localStartDate.isRelative) ||
            (!startModifiedExternally && !backendStoresStartDate)
        val selectedDay: Long
        val selectedTime: Int
        val hideUntil: Long
        if (keepLocalStart) {
            hideUntil = resolveStartDate(localStartDate, state.startTime, mergedDueDate)
            selectedDay = state.startDay
            selectedTime = state.startTime
        } else {
            val (day, time) = startSelectionDays(dbTask.hideUntil, mergedDueDate)
            selectedDay = day
            selectedTime = time
            hideUntil = dbTask.hideUntil
        }
        val (baselineDay, baselineTime) = if (startModifiedLocally) {
            startSelectionDays(dbTask.hideUntil, dbTask.dueDate)
        } else {
            selectedDay to selectedTime
        }
        return StartReconciliation(hideUntil, selectedDay, selectedTime, baselineDay, baselineTime)
    }

    private data class StartReconciliation(
        val hideUntil: Long,
        val selectedDay: Long,
        val selectedTime: Int,
        val baselineDay: Long,
        val baselineTime: Int,
    )

    private fun <T> merge(current: T, original: T, db: T): T =
        if (current == original) db else current

    private suspend fun seedList(): CaldavFilter? {
        // A read-only list can't take a new task, so fall back the same way an unknown list does.
        val calendar = listId
            ?.let { caldavDao.getCalendarById(it) }
            ?.takeIf { !it.readOnly() }
            ?: return firstCaldavList()
        val account = calendar.account?.let { caldavDao.getAccountByUuid(it) } ?: return firstCaldavList()
        return CaldavFilter(calendar = calendar, account = account)
    }

    private suspend fun seedTags(): List<TagData> =
        listOfNotNull(tagUuid?.let { tagDataDao.getByUuid(it) })

    private suspend fun firstCaldavList(): CaldavFilter? {
        val calendar = caldavDao.getCalendars()
            .firstOrNull { !it.readOnly() } ?: return null
        val account = calendar.account?.let { caldavDao.getAccountByUuid(it) } ?: return null
        return CaldavFilter(calendar = calendar, account = account)
    }

    private suspend fun caldavListFor(taskId: Long): CaldavFilter? {
        val caldavTask = caldavDao.getTask(taskId)
        val calendar = caldavTask?.calendar?.let { caldavDao.getCalendarByUuid(it) }
        val account = calendar?.account?.let { caldavDao.getAccountByUuid(it) }
        return if (calendar != null && account != null) {
            CaldavFilter(calendar = calendar, account = account)
        } else {
            firstCaldavList()
        }
    }

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
        _state.update { it.withStartSelection(it.startDay, it.startTime, dueDate) }
        // TODO: add default reminders, update recurrence
    }

    fun setStartDate(day: Long, time: Int) {
        _state.update { it.withStartSelection(day, time, it.task.dueDate) }
        // TODO: add default reminders
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
    }

    fun setTags(tags: List<TagData>) {
        _state.update { it.copy(tags = tags) }
    }

    fun save() {
        // Guarded rather than queued. Back, escape and the toolbar arrow all land here, and this can
        // wait: a save already in flight for the same task holds its lock for as long as the
        // calendar provider and sync adapters take. Without the guard every repeat press stacked
        // another coroutine on that lock; with it - and with [saving] on screen - the press is
        // visibly doing something instead of looking dead.
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

    /**
     * Writes [snapshot] out if there is anything to write. Returns the task as persisted - stamped
     * with its row id and modification date - or null if nothing was written.
     */
    private suspend fun saveIfNeeded(snapshot: State): Task? {
        val list = snapshot.list ?: return null
        // The task was deleted out from under us; saving would write deletionDate back to 0.
        if (snapshot.deleted) return null
        if (!snapshot.hasChanges && !snapshot.pendingSideEffects) return null
        // Saved from a copy. Task is mutable exactly where the write path stamps it, and the
        // instance in the snapshot is the one the editor is still showing - so mutating it here
        // would bump the modification date under whatever the user is typing right now, leaving
        // hasChanges stuck true and every later save rewriting the row with a stale timestamp.
        val task = snapshot.task.copy()
        // TODO: apply calendar changes
        if (snapshot.isNew) {
            // One transaction: a task row without its caldav row belongs to no list and is
            // invisible in every filter, and the unique index on remoteId means a retry that tried
            // to create it again would fail for good rather than repair it.
            taskDao.inTransaction {
                taskDao.createNew(task)
                caldavDao.insert(
                    task = task,
                    caldavTask = CaldavTask(task = task.id, calendar = list.uuid),
                    addToTop = false,
                )
                applyTagsIfNeeded(snapshot, task)
            }
            // The row exists now, so record that before anything that can still fail. Otherwise a
            // save that throws below leaves the editor believing the task is new, and the next
            // teardown save collides with the row this one just wrote. The id goes onto the live
            // task too: from here on isNew is false, so every later save updates by id and a task
            // still holding 0 would match no rows and report success having written nothing.
            //
            // pendingSideEffects carries the other half. Marking the state clean here was enough to
            // make hasChanges false, so a save that then threw below left a created row that
            // nothing had marked dirty - invisible to every synchronizer - and every retry returned
            // success having done nothing, because it short-circuited on "no changes".
            _state.update {
                it.copy(
                    task = it.task.copy(id = task.id, modificationDate = task.modificationDate),
                    originalTask = task.copy(),
                    pendingSideEffects = true,
                )
            }
            taskSaver.save(task, null)
        } else {
            applyTagsIfNeeded(snapshot, task)
            // original = null where an earlier attempt created the row but never got its save
            // through: that is what tells TaskSaver this is still a creation, so the row is marked
            // dirty and handed to the synchronizers rather than compared against itself and skipped.
            taskSaver.save(task, snapshot.originalTask.takeUnless { snapshot.pendingSideEffects })
            if (snapshot.list != snapshot.originalList) {
                taskMover.move(listOf(task.id), list)
            }
        }
        return task
    }

    /** Stamps [task] - always the save's private copy, never the instance held in state. */
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

internal fun Task.sameEditableContentAs(other: Task): Boolean =
    copy(
        transitoryData = null,
        id = other.id,
        creationDate = other.creationDate,
        remoteId = other.remoteId,
    ) == other.copy(transitoryData = null)
