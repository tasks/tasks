package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.time.DateTimeUtils2.currentTimeMillis

class TaskEditViewModel(
    private val taskDao: TaskDao,
    private val taskSaver: TaskSaver,
    private val caldavDao: CaldavDao,
) : ViewModel() {

    data class State(
        val isLoading: Boolean = true,
        val task: Task = Task(),
        val originalTask: Task = Task(),
        val list: CaldavFilter? = null,
    ) {
        val isNew: Boolean get() = originalTask.isNew
        val hasChanges: Boolean get() = task != originalTask
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _closeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val closeEvents: SharedFlow<Unit> = _closeEvents.asSharedFlow()

    fun initialize(taskId: Long?) {
        val normalized = taskId?.takeIf { it != Task.NO_ID }
        _state.value = State(isLoading = true)
        viewModelScope.launch {
            val loaded: Task
            val list: CaldavFilter?
            if (normalized == null) {
                loaded = Task(creationDate = currentTimeMillis())
                list = firstCaldavList()
            } else {
                loaded = taskDao.fetch(normalized) ?: Task(creationDate = currentTimeMillis())
                val caldavTask = caldavDao.getTask(normalized)
                val calendar = caldavTask?.calendar?.let { caldavDao.getCalendarByUuid(it) }
                val account = calendar?.account?.let { caldavDao.getAccountByUuid(it) }
                list = if (calendar != null && account != null) {
                    CaldavFilter(calendar = calendar, account = account)
                } else {
                    firstCaldavList()
                }
            }
            _state.value = State(
                isLoading = false,
                task = loaded,
                originalTask = loaded.copy(),
                list = list,
            )
        }
    }

    private suspend fun firstCaldavList(): CaldavFilter? {
        val calendar = caldavDao.getCalendars().firstOrNull() ?: return null
        val account = calendar.account?.let { caldavDao.getAccountByUuid(it) } ?: return null
        return CaldavFilter(calendar = calendar, account = account)
    }

    fun setTitle(title: String) {
        _state.update { it.copy(task = it.task.copy(title = title)) }
    }

    fun setDescription(description: String) {
        _state.update { it.copy(task = it.task.copy(notes = description)) }
    }

    fun save() {
        val snapshot = _state.value
        if (snapshot.isLoading) {
            _closeEvents.tryEmit(Unit)
            return
        }
        val list = snapshot.list
        val isNew = snapshot.isNew
        val nothingToSave = (isNew && snapshot.task.title.isNullOrBlank()) ||
                !snapshot.hasChanges
        if (list == null || nothingToSave) {
            _closeEvents.tryEmit(Unit)
            return
        }
        viewModelScope.launch {
            val task = snapshot.task
            if (isNew) {
                taskDao.createNew(task)
                caldavDao.insert(
                    task = task,
                    caldavTask = CaldavTask(task = task.id, calendar = list.uuid),
                    addToTop = false,
                )
                taskSaver.save(task, null)
            } else {
                taskSaver.save(task, snapshot.originalTask)
            }
            _state.update { it.copy(originalTask = task.copy()) }
            _closeEvents.emit(Unit)
        }
    }
}
