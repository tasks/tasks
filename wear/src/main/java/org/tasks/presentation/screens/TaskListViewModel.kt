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
import timber.log.Timber

@OptIn(ExperimentalHorologistApi::class)
class TaskListViewModel(
    application: Application
) : AndroidViewModel(application) {
    private var pagingSource: MyPagingSource<UiItem>? = null
    val uiItems: Flow<PagingData<UiItem>> = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = {
            Timber.d("Creating new paging source")
            MyPagingSource { position, limit ->
                Timber.d("Fetching $limit @ $position")
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
                    .also {
                        Timber.d("Fetched ${it.second.size} items [position=$position limit=$limit totalItems=${it.first}]")
                    }
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
            .onEach { invalidate() }
            .launchIn(viewModelScope)
        registry
            .protoFlow<Settings>(TargetNodeId.PairedPhone)
            .onEach { invalidate() }
            .launchIn(viewModelScope)
    }

    fun toggleGroup(value: Long, setCollapsed: Boolean) = viewModelScope.launch {
        try {
            wearService.toggleGroup(
                ToggleGroupRequest.newBuilder()
                    .setValue(value)
                    .setCollapsed(setCollapsed)
                    .build()
            )
            invalidate()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun completeTask(id: Long, completed: Boolean) = viewModelScope.launch {
        try {
            wearService.completeTask(
                CompleteTaskRequest.newBuilder().setId(id).setCompleted(completed).build()
            )
            invalidate()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun toggleSubtasks(id: Long, collapsed: Boolean) = viewModelScope.launch {
        try {
            wearService.toggleSubtasks(
                ToggleGroupRequest.newBuilder().setValue(id).setCollapsed(collapsed).build()
            )
            invalidate()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun invalidate() {
        pagingSource?.invalidate()
    }
}
