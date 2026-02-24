package org.tasks.presentation.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import org.tasks.presentation.phoneTargetNodeId
import com.google.android.horologist.datalayer.grpc.GrpcExtensions.grpcClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.tasks.GrpcProto
import org.tasks.GrpcProto.CompleteTaskRequest
import org.tasks.GrpcProto.ToggleGroupRequest
import org.tasks.GrpcProto.UiItem
import org.tasks.WearServiceGrpcKt
import org.tasks.extensions.wearDataLayerRegistry
import org.tasks.presentation.MyPagingSource
import org.tasks.presentation.RefreshTrigger
import org.tasks.presentation.WearSettings
import timber.log.Timber

@OptIn(ExperimentalHorologistApi::class)
class TaskListViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val registry = application.wearDataLayerRegistry(viewModelScope)
    private val targetNodeId = application.phoneTargetNodeId()
    private val wearSettings = WearSettings.getInstance(application)

    private val wearService : WearServiceGrpcKt.WearServiceCoroutineStub = registry.grpcClient(
        nodeId = targetNodeId,
        coroutineScope = viewModelScope,
    ) {
        WearServiceGrpcKt.WearServiceCoroutineStub(it)
    }

    private var pagingSource: MyPagingSource<UiItem>? = null
    val uiItems: Flow<PagingData<UiItem>> = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = {
            MyPagingSource { position, limit ->
                val settings = wearSettings.stateFlow.value
                wearService
                    .getTasks(
                        GrpcProto
                            .GetTasksRequest
                            .newBuilder()
                            .setPosition(position)
                            .setLimit(limit)
                            .setShowHidden(settings.showHidden)
                            .setShowCompleted(settings.showCompleted)
                            .addAllCollapsed(settings.collapsed)
                            .apply {
                                settings.filter?.takeIf { it.isNotBlank() }?.let {
                                    setFilter(it)
                                }
                                setSortMode(settings.sortMode)
                                setGroupMode(settings.groupMode)
                            }
                            .build()
                    )
                    .let { Pair(it.totalItems, it.itemsList) }
            }
                .also { pagingSource = it }
        }
    )
        .flow
        .cachedIn(viewModelScope)

    init {
        wearSettings.stateFlow
            .onEach { invalidate() }
            .launchIn(viewModelScope)
        RefreshTrigger.flow
            .onEach { invalidate() }
            .launchIn(viewModelScope)
    }

    fun toggleGroup(value: Long, setCollapsed: Boolean) {
        wearSettings.setCollapsed(value, setCollapsed)
        invalidate()
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

    fun invalidate() {
        pagingSource?.invalidate()
    }
}
