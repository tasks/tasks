package org.tasks.presentation.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.datalayer.grpc.GrpcExtensions.grpcClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.GrpcProto
import org.tasks.WearServiceGrpcKt
import org.tasks.extensions.wearDataLayerRegistry
import timber.log.Timber

data class UiState(
    val isNew: Boolean,
    val loading: Boolean = !isNew,
    val completed: Boolean = false,
    val repeating: Boolean = false,
    val priority: Int = 0,
    val title: String = "",
)

@OptIn(ExperimentalHorologistApi::class)
class TaskEditViewModel(
    applicationContext: Context,
    private val taskId: Long,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState(isNew = taskId == 0L))
    val uiState = _uiState.asStateFlow()
    private val registry = applicationContext.wearDataLayerRegistry(viewModelScope)

    private val wearService : WearServiceGrpcKt.WearServiceCoroutineStub = registry.grpcClient(
        nodeId = TargetNodeId.PairedPhone,
        coroutineScope = viewModelScope,
    ) {
        WearServiceGrpcKt.WearServiceCoroutineStub(it)
    }

    init {
        if (taskId > 0) {
            viewModelScope.launch {
                val task = wearService
                    .getTask(GrpcProto.GetTaskRequest.newBuilder().setTaskId(taskId).build())
                Timber.d("Received $task")
                _uiState.update {
                    it.copy(
                        loading = false,
                        completed = task.completed,
                        title = task.title,
                        repeating = task.repeating,
                        priority = task.priority,
                    )
                }

            }
        }
    }

    fun save(onComplete: () -> Unit) = viewModelScope.launch {
        val state = uiState.value
        wearService.saveTask(
            GrpcProto.SaveTaskRequest.newBuilder()
                .setTitle(state.title)
                .build()
        )
        onComplete()
    }

    fun setTitle(title: String) {
        _uiState.update { it.copy(title = title) }
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
