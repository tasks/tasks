package org.tasks.presentation.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import org.tasks.jobs.PendingTaskQueue
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import org.tasks.presentation.phoneTargetNodeId
import com.google.android.horologist.datalayer.grpc.GrpcExtensions.grpcClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.GrpcProto
import org.tasks.WearServiceGrpcKt
import org.tasks.extensions.wearDataLayerRegistry
import org.tasks.jobs.CreateTaskWorker
import org.tasks.presentation.WearSettings
import timber.log.Timber

data class UiState(
    val loaded: Boolean = false,
    val taskId: Long = 0,
    val completed: Boolean = false,
    val repeating: Boolean = false,
    val priority: Int = 0,
    val title: String = "",
    val description: String = "",
    val error: Boolean = false,
)

@OptIn(ExperimentalHorologistApi::class)
class TaskEditViewModel(
    private val applicationContext: Context,
    taskId: Long,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState(taskId = taskId))
    val uiState = _uiState.asStateFlow()
    private val registry = applicationContext.wearDataLayerRegistry(viewModelScope)
    private val wearSettings = WearSettings.getInstance(applicationContext)

    private val _navigateBack = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateBack = _navigateBack.asSharedFlow()

    private var taskQueued = false

    private val wearService : WearServiceGrpcKt.WearServiceCoroutineStub = registry.grpcClient(
        nodeId = applicationContext.phoneTargetNodeId(),
        coroutineScope = viewModelScope,
    ) {
        WearServiceGrpcKt.WearServiceCoroutineStub(it)
    }

    init {
        if (taskId > 0) {
            viewModelScope.launch {
                fetchTask(taskId)
            }
        }
    }

    private fun fetchTask(taskId: Long) = viewModelScope.launch {
        try {
            val task = wearService
                .getTask(GrpcProto.GetTaskRequest.newBuilder().setTaskId(taskId).build())
            Timber.d("Received $task")
            _uiState.update {
                it.copy(
                    loaded = true,
                    taskId = taskId,
                    completed = task.completed,
                    title = task.title,
                    repeating = task.repeating,
                    priority = task.priority,
                    description = task.description,
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch task $taskId")
            _uiState.update { it.copy(error = true) }
        }
    }

    fun save(onComplete: () -> Unit) = viewModelScope.launch {
        try {
            val state = uiState.value
            wearService.saveTask(
                GrpcProto.SaveTaskRequest.newBuilder()
                    .setTitle(state.title)
                    .setTaskId(state.taskId)
                    .setCompleted(state.completed)
                    .build()
            )
            onComplete()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save task")
            _uiState.update { it.copy(error = true) }
        }
    }

    fun setTitle(title: String) {
        _uiState.update { it.copy(title = title) }
        if (uiState.value.taskId == 0L && !taskQueued) {
            taskQueued = true
            PendingTaskQueue.getInstance(applicationContext).add(
                title = title,
                filter = wearSettings.stateFlow.value.filter,
            )
            CreateTaskWorker.enqueue(applicationContext)
            _navigateBack.tryEmit(Unit)
        }
    }

    fun setCompleted(completed: Boolean, onComplete: () -> Unit) {
        _uiState.update { it.copy(completed = completed) }
        if (completed) {
            save(onComplete)
        }
    }
}

class TaskEditViewModelFactory(
    private val applicationContext: Context,
    private val taskId: Long,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskEditViewModel::class.java)) {
            return TaskEditViewModel(
                applicationContext = applicationContext,
                taskId = taskId,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
