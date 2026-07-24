package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import org.tasks.filters.Filter
import org.tasks.filters.TagFilter
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.DatePickerPreferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.noon

class TaskEditViewModel(
    private val taskDao: TaskDao,
    private val taskSaver: TaskSaver,
    private val caldavDao: CaldavDao,
    private val taskMover: TaskMover,
    private val tagDao: TagDao,
    private val tagDataDao: TagDataDao,
    private val appPreferences: AppPreferences,
    private val externalScope: CoroutineScope,
    private val taskCreator: TaskCreator = TaskCreator(),
) : ViewModel() {

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

    private val _closeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val closeEvents: SharedFlow<Unit> = _closeEvents.asSharedFlow()

    private val _saveError = MutableStateFlow(false)
    val saveError: StateFlow<Boolean> = _saveError.asStateFlow()

    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

    private val saveMutex = Mutex()
    private val pickerModeMutex = Mutex()
    private var initializeJob: Job? = null
    private var watchJob: Job? = null

    fun initialize(taskId: Long?, currentFilter: Filter? = null) {
        watchJob?.cancel()
        val normalized = taskId?.takeIf { it != Task.NO_ID }
        initializeJob?.cancel()
        initializeJob = viewModelScope.launch {
            _loadError.value = false
            _state.value = State(isLoading = true)
            try {
                val loaded: Task
                val list: CaldavFilter?
                val tags: List<TagData>
                val prefs: DatePickerPreferences
                if (normalized == null) {
                    loaded = taskCreator.createBlankTask()
                    tags = listOfNotNull((currentFilter as? TagFilter)?.tagData)
                    coroutineScope {
                        val listDeferred = async { (currentFilter as? CaldavFilter) ?: firstCaldavList() }
                        val prefsDeferred = async { appPreferences.datePickerPreferences() }
                        list = listDeferred.await()
                        prefs = prefsDeferred.await()
                    }
                } else {
                    coroutineScope {
                        val loadedDeferred = async { taskDao.fetch(normalized) ?: taskCreator.createBlankTask() }
                        val listDeferred = async { caldavListFor(normalized) }
                        val tagsDeferred = async { tagDataDao.getTagDataForTask(normalized) }
                        val prefsDeferred = async { appPreferences.datePickerPreferences() }
                        loaded = loadedDeferred.await()
                        list = listDeferred.await()
                        tags = tagsDeferred.await()
                        prefs = prefsDeferred.await()
                    }
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
                _state.value = State(
                    isLoading = false,
                    task = task,
                    originalTask = task.copy(),
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
                if (normalized != null) {
                    watchJob = viewModelScope.launch {
                        taskDao.watch(normalized)
                            .filterNotNull()
                            .distinctUntilChanged()
                            .collect { dbTask -> mergeDbUpdate(dbTask) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Failed to initialize task editor" }
                _loadError.value = true
                _state.value = State(isLoading = false)
            }
        }
    }

    suspend fun saveCurrentTask() {
        saveLocked()
    }

    private suspend fun saveLocked(): Boolean = withContext(NonCancellable) {
        saveMutex.withLock {
            val snapshot = _state.value
            if (snapshot.isLoading) return@withLock true
            try {
                saveIfNeeded(snapshot)
                _state.update {
                    it.copy(
                        originalTask = snapshot.task.copy(),
                        originalList = snapshot.list,
                        originalTags = snapshot.tags,
                        originalStartDay = snapshot.startDay,
                        originalStartTime = snapshot.startTime,
                    )
                }
                true
            } catch (e: Exception) {
                log.e(e) { "Failed to save task" }
                _saveError.value = true
                false
            }
        }
    }

    private suspend fun mergeDbUpdate(dbTask: Task) {
        val shouldClose = saveMutex.withLock { applyDbUpdate(dbTask) }
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

    fun clearSaveError() {
        _saveError.value = false
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
        viewModelScope.launch {
            if (saveLocked()) {
                _closeEvents.emit(Unit)
            }
        }
    }

    private suspend fun saveIfNeeded(snapshot: State) {
        val list = snapshot.list ?: return
        if (!snapshot.hasChanges) return
        val task = snapshot.task
        // TODO: apply calendar changes
        if (snapshot.isNew) {
            taskDao.createNew(task)
            caldavDao.insert(
                task = task,
                caldavTask = CaldavTask(task = task.id, calendar = list.uuid),
                addToTop = false,
            )
            applyTagsIfNeeded(snapshot, task)
            taskSaver.save(task, null)
        } else {
            applyTagsIfNeeded(snapshot, task)
            taskSaver.save(task, snapshot.originalTask)
            if (snapshot.list != snapshot.originalList) {
                taskMover.move(listOf(task.id), list)
            }
        }
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

internal fun Task.sameEditableContentAs(other: Task): Boolean =
    copy(
        transitoryData = null,
        id = other.id,
        creationDate = other.creationDate,
        remoteId = other.remoteId,
    ) == other.copy(transitoryData = null)
