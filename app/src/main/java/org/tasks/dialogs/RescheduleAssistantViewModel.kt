package org.tasks.dialogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoroo.astrid.service.TaskCompleter
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.data.isOverdue
import org.tasks.date.DateTimeUtils.newDateTime
import javax.inject.Inject

@HiltViewModel
class RescheduleAssistantViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val localBroadcastManager: LocalBroadcastManager,
    private val taskCompleter: TaskCompleter,
    private val taskDeleter: TaskDeleter,
) : ViewModel() {

    data class ViewState(
        val currentTask: Task? = null,
        val isDueToday: Boolean = false
    )

    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asStateFlow()

    private val skippedTasks = mutableListOf<Task>()

    private var filter = ""

    fun initData(filter: String){
        this.filter = filter
        getNextTask()
    }

    fun reschedule(day: Long) {
        viewModelScope.launch {
            val updatedTask = viewState.value.currentTask
            updatedTask?.dueDate = day

            updatedTask?.let {
                taskDao.update(it)

                if (isToday(day)) {
                    skippedTasks.add(it)
                }
            }

            getNextTask()
            localBroadcastManager.broadcastRefresh()
        }
    }

    fun markAsDone() {
        viewModelScope.launch {
            viewState.value.currentTask?.let {
                taskCompleter.setComplete(it.id, true)
                getNextTask()
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            viewState.value.currentTask?.let {
                taskDeleter.delete(it)
                getNextTask()
            }
        }
    }

    fun skip() {
        viewState.value.currentTask?.let {
            skippedTasks.add(it)
            getNextTask()
        }
    }

    fun getNextTask() {
        viewModelScope.launch {
            val tasks = taskDao.fetchTasks(filter).map { it.task }
            val tasksToReschedule = tasks
                .filter { task -> task.isOverdue || isToday(task.dueDate) }
                .filter { !skippedTasks.contains(it) }

            tasksToReschedule.firstOrNull().let { task ->
                _viewState.update { state ->
                    state.copy(
                        currentTask = task,
                        isDueToday = isToday(task?.dueDate ?: 0)
                    )
                }
            }
        }
    }

    fun isToday(dueDate: Long): Boolean {
        val todayStart = newDateTime().startOfDay()
        val tomorrowStart = todayStart.plusDays(1)
        return dueDate >= todayStart.millis && dueDate < tomorrowStart.millis
    }
}