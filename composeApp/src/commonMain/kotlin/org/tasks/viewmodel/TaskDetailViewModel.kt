package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.data.TaskSaver
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.time.DateTimeUtils2.currentTimeMillis

class TaskDetailViewModel(
    private val taskDao: TaskDao,
    private val taskSaver: TaskSaver,
) : ViewModel() {

    data class State(
        val task: Task? = null,
        val title: String = "",
        val notes: String = "",
        val priority: Int = Task.Priority.NONE,
        val isCompleted: Boolean = false,
        val dueDate: Long = 0,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun load(taskId: Long) {
        viewModelScope.launch {
            val task = taskDao.fetch(taskId) ?: return@launch
            _state.value = State(
                task = task,
                title = task.title ?: "",
                notes = task.notes ?: "",
                priority = task.priority,
                isCompleted = task.isCompleted,
                dueDate = task.dueDate,
            )
        }
    }

    fun setTitle(v: String) = _state.update { it.copy(title = v) }
    fun setNotes(v: String) = _state.update { it.copy(notes = v) }
    fun setPriority(v: Int) = _state.update { it.copy(priority = v) }
    fun toggleComplete() = _state.update { it.copy(isCompleted = !it.isCompleted) }

    fun save() {
        val s = _state.value
        val original = s.task ?: return
        viewModelScope.launch {
            taskSaver.save(
                original.copy(
                    title = s.title,
                    notes = s.notes.ifBlank { null },
                    priority = s.priority,
                    completionDate = if (s.isCompleted)
                        original.completionDate.takeIf { it > 0 } ?: currentTimeMillis()
                    else 0L,
                    modificationDate = currentTimeMillis(),
                ),
                original,
            )
        }
    }
}
