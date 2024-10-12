package org.tasks.presentation.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.grpc.GrpcExtensions.grpcClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.GrpcProto
import org.tasks.GrpcProto.Tasks
import org.tasks.WearServiceGrpcKt
import org.tasks.wear.TasksSerializer

data class TaskListScreenState(
    val error: String? = null,
    val tasks: Tasks = Tasks.getDefaultInstance(),
)

@OptIn(ExperimentalHorologistApi::class)
class TaskListViewModel(
    application: Application
) : AndroidViewModel(application) {
    val uiState: MutableStateFlow<TaskListScreenState> = MutableStateFlow(TaskListScreenState())
    private val scope = CoroutineScope(Dispatchers.IO)
    private val wearDataLayerRegistry = WearDataLayerRegistry.fromContext(
        application = application,
        coroutineScope = scope,
    ).apply {
        registerSerializer(TasksSerializer)
    }
    private val wearService : WearServiceGrpcKt.WearServiceCoroutineStub = wearDataLayerRegistry.grpcClient(
        nodeId = TargetNodeId.PairedPhone,
        coroutineScope = scope,
    ) {
        WearServiceGrpcKt.WearServiceCoroutineStub(it)
    }

    init {
        viewModelScope.launch {
            val tasks = wearService.getTasks(GrpcProto.GetTasksRequest.getDefaultInstance())
            uiState.update { it.copy(tasks = tasks) }
        }
    }

    fun completeTask(it: Long) = viewModelScope.launch {
        wearService.completeTask(GrpcProto.CompleteTaskRequest.newBuilder().setId(it).setCompleted(true).build())
    }
}