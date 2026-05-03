package org.tasks.wear

import org.tasks.GrpcProto
import org.tasks.GrpcProto.CompleteTaskRequest
import org.tasks.GrpcProto.CompleteTaskResponse
import org.tasks.GrpcProto.GetListsResponse
import org.tasks.GrpcProto.GetTaskResponse
import org.tasks.GrpcProto.GetTasksRequest
import org.tasks.GrpcProto.ListItem
import org.tasks.GrpcProto.ListItemType
import org.tasks.GrpcProto.GetTaskCountRequest
import org.tasks.GrpcProto.GetTaskCountResponse
import org.tasks.GrpcProto.GetVersionRequest
import org.tasks.GrpcProto.GetVersionResponse
import org.tasks.GrpcProto.SaveTaskResponse
import org.tasks.GrpcProto.Tasks
import org.tasks.GrpcProto.ToggleGroupRequest
import org.tasks.GrpcProto.ToggleGroupResponse
import org.tasks.WearServiceGrpcKt
import org.tasks.watch.WatchListItem
import org.tasks.watch.WatchServiceLogic
import org.tasks.watch.WatchUiItem

class WearService(
    private val logic: WatchServiceLogic,
    private val versionCode: Int,
) : WearServiceGrpcKt.WearServiceCoroutineImplBase() {

    override suspend fun getTasks(request: GetTasksRequest): Tasks {
        val result = logic.getTasks(
            filterPreference = request.filter.takeIf { request.hasFilter() },
            position = request.position,
            limit = request.limit,
            showHidden = request.showHidden,
            showCompleted = request.showCompleted,
            sortMode = if (request.hasSortMode()) request.sortMode else null,
            groupMode = if (request.hasGroupMode()) request.groupMode else null,
            collapsed = request.collapsedList.toSet(),
        )
        return Tasks.newBuilder()
            .setTotalItems(result.totalItems)
            .addAllItems(
                result.items.map { item ->
                    when (item) {
                        is WatchUiItem.Header ->
                            GrpcProto.UiItem.newBuilder()
                                .setId(item.id)
                                .setType(ListItemType.Header)
                                .setTitle(item.title)
                                .setCollapsed(item.collapsed)
                                .build()

                        is WatchUiItem.Task ->
                            GrpcProto.UiItem.newBuilder()
                                .setType(ListItemType.Item)
                                .setId(item.id)
                                .setPriority(item.priority)
                                .setCompleted(item.completed)
                                .setHidden(item.hidden)
                                .setIndent(item.indent)
                                .setCollapsed(item.collapsed)
                                .setNumSubtasks(item.numSubtasks)
                                .apply {
                                    if (item.title != null) {
                                        setTitle(item.title)
                                    }
                                    if (item.timestamp != null) {
                                        setTimestamp(item.timestamp)
                                    }
                                }
                                .setRepeating(item.repeating)
                                .build()
                    }
                }
            )
            .build()
    }

    override suspend fun completeTask(request: CompleteTaskRequest): CompleteTaskResponse {
        logic.completeTask(request.id, request.completed, "wearable")
        return CompleteTaskResponse.newBuilder().setSuccess(true).build()
    }

    override suspend fun toggleSubtasks(request: ToggleGroupRequest): ToggleGroupResponse {
        logic.toggleGroup(request.value, request.collapsed)
        return ToggleGroupResponse.newBuilder().build()
    }

    override suspend fun getLists(request: GrpcProto.GetListsRequest): GetListsResponse {
        val result = logic.getLists(request.position, request.limit)
        return GetListsResponse.newBuilder()
            .setTotalItems(result.totalItems)
            .addAllItems(
                result.items.map { item ->
                    when (item) {
                        is WatchListItem.FilterItem ->
                            ListItem.newBuilder()
                                .setId(item.id)
                                .setType(ListItemType.Item)
                                .setTitle(item.title)
                                .apply { item.icon?.let { setIcon(it) } }
                                .setColor(item.color)
                                .setTaskCount(item.taskCount)
                                .build()

                        is WatchListItem.Header ->
                            ListItem.newBuilder()
                                .setType(ListItemType.Header)
                                .setTitle(item.title)
                                .setId(item.id)
                                .build()
                    }
                }
            )
            .build()
    }

    override suspend fun getTask(request: GrpcProto.GetTaskRequest): GetTaskResponse {
        val task = logic.getTask(request.taskId)
        return GetTaskResponse.newBuilder()
            .setTitle(task.title)
            .setCompleted(task.completed)
            .setPriority(task.priority)
            .setRepeating(task.repeating)
            .setDescription(task.description)
            .build()
    }

    override suspend fun saveTask(request: GrpcProto.SaveTaskRequest): SaveTaskResponse {
        val resultId = logic.saveTask(
            taskId = request.taskId,
            title = request.title,
            completed = request.completed,
            filterPreference = request.filter.takeIf { request.hasFilter() },
            source = "wearable",
        )
        return SaveTaskResponse.newBuilder().setTaskId(resultId).build()
    }

    override suspend fun getTaskCount(request: GetTaskCountRequest): GetTaskCountResponse {
        val result = logic.getTaskCount(
            filterPreference = request.filter.takeIf { request.hasFilter() },
            showHidden = request.showHidden,
            showCompleted = request.showCompleted,
        )
        return GetTaskCountResponse.newBuilder()
            .setCount(result.count)
            .setCompletedCount(result.completedCount)
            .build()
    }

    override suspend fun getVersion(request: GetVersionRequest): GetVersionResponse {
        return GetVersionResponse.newBuilder()
            .setVersionCode(versionCode)
            .build()
    }
}
