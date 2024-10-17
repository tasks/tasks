package org.tasks.presentation.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ProtoDataStoreHelper.protoFlow
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.grpc.GrpcExtensions.grpcClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.tasks.GrpcProto
import org.tasks.GrpcProto.LastUpdate
import org.tasks.GrpcProto.UiItem
import org.tasks.WearServiceGrpcKt
import org.tasks.presentation.MyPagingSource
import org.tasks.wear.LastUpdateSerializer

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
                    .itemsList
            }
                .also { pagingSource = it }
        }
    )
        .flow
    private val wearDataLayerRegistry = WearDataLayerRegistry.fromContext(
        application = application,
        coroutineScope = viewModelScope,
    ).apply {
        registerSerializer(LastUpdateSerializer)
    }
    private val wearService : WearServiceGrpcKt.WearServiceCoroutineStub = wearDataLayerRegistry.grpcClient(
        nodeId = TargetNodeId.PairedPhone,
        coroutineScope = viewModelScope,
    ) {
        WearServiceGrpcKt.WearServiceCoroutineStub(it)
    }

    init {
        wearDataLayerRegistry
            .protoFlow<LastUpdate>(TargetNodeId.PairedPhone)
            .onEach { pagingSource?.invalidate() }
            .launchIn(viewModelScope)
    }

    fun completeTask(it: Long) = viewModelScope.launch {
        wearService.completeTask(GrpcProto.CompleteTaskRequest.newBuilder().setId(it).setCompleted(true).build())
    }
}
