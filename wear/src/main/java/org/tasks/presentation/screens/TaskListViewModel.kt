package org.tasks.presentation.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ProtoDataStoreHelper.protoFlow
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.datalayer.grpc.GrpcExtensions.grpcClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.tasks.GrpcProto
import org.tasks.GrpcProto.CompleteTaskRequest
import org.tasks.GrpcProto.LastUpdate
import org.tasks.GrpcProto.Settings
import org.tasks.GrpcProto.ToggleGroupRequest
import org.tasks.GrpcProto.UiItem
import org.tasks.WearServiceGrpcKt
import org.tasks.extensions.wearDataLayerRegistry
import org.tasks.presentation.MyPagingSource

@OptIn(ExperimentalHorologistApi::class)
class TaskListViewModel(
    application: Application
) : AndroidViewModel(application) {
    private var pagingSource: MyPagingSource<UiItem>? = null
    val uiItems: Flow<PagingData<UiItem>> = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = {
            MyPagingSource { position, limit ->
                wearService
                    .getTasks(
                        GrpcProto
                            .GetTasksRequest
                            .newBuilder()
                            .setPosition(position)
                            .setLimit(limit)
                            .build()
                    )
                    .let { Pair(it.totalItems, it.itemsList) }
            }
                .also { pagingSource = it }
        }
    )
        .flow
        .cachedIn(viewModelScope)

    private val registry = application.wearDataLayerRegistry(viewModelScope)

    private val wearService : WearServiceGrpcKt.WearServiceCoroutineStub = registry.grpcClient(
        nodeId = TargetNodeId.PairedPhone,
        coroutineScope = viewModelScope,
    ) {
        WearServiceGrpcKt.WearServiceCoroutineStub(it)
    }

    init {
        registry
            .protoFlow<LastUpdate>(TargetNodeId.PairedPhone)
            .onEach { pagingSource?.invalidate() }
            .launchIn(viewModelScope)
        registry
            .protoFlow<Settings>(TargetNodeId.PairedPhone)
            .onEach { pagingSource?.invalidate() }
            .launchIn(viewModelScope)
    }

    fun toggleGroup(value: Long, setCollapsed: Boolean) = viewModelScope.launch {
        wearService.toggleGroup(
            ToggleGroupRequest.newBuilder()
                .setValue(value)
                .setCollapsed(setCollapsed)
                .build()
        )
    }

    fun completeTask(id: Long, completed: Boolean) = viewModelScope.launch {
        wearService.completeTask(
            CompleteTaskRequest.newBuilder().setId(id).setCompleted(completed).build()
        )
    }

    fun toggleSubtasks(id: Long, collapsed: Boolean) = viewModelScope.launch {
        wearService.toggleSubtasks(
            ToggleGroupRequest.newBuilder().setValue(id).setCollapsed(collapsed).build()
        )
    }
}
