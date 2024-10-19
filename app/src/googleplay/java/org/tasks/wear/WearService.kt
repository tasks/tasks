package org.tasks.wear

import androidx.datastore.core.DataStore
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskCompleter
import kotlinx.coroutines.flow.firstOrNull
import org.tasks.GrpcProto
import org.tasks.GrpcProto.CompleteTaskRequest
import org.tasks.GrpcProto.CompleteTaskResponse
import org.tasks.GrpcProto.GetTasksRequest
import org.tasks.GrpcProto.Tasks
import org.tasks.GrpcProto.ToggleGroupRequest
import org.tasks.GrpcProto.ToggleGroupResponse
import org.tasks.WearServiceGrpcKt
import org.tasks.copy
import org.tasks.data.isHidden
import org.tasks.filters.AstridOrderingFilter
import org.tasks.filters.MyTasksFilter
import org.tasks.preferences.Preferences
import org.tasks.tasklist.HeaderFormatter
import org.tasks.tasklist.SectionedDataSource
import org.tasks.tasklist.UiItem

class WearService(
    private val taskDao: TaskDao,
    private val preferences: Preferences,
    private val taskCompleter: TaskCompleter,
    private val headerFormatter: HeaderFormatter,
    private val settings: DataStore<GrpcProto.Settings>,
) : WearServiceGrpcKt.WearServiceCoroutineImplBase() {
    override suspend fun getTasks(request: GetTasksRequest): Tasks {
        val position = request.position
        val limit = request.limit.takeIf { it > 0 } ?: Int.MAX_VALUE
        val filter = MyTasksFilter.create()
        val settingsData = settings.data.firstOrNull()
        val collapsed = settingsData?.collapsedList?.toSet() ?: emptySet()
        val payload = SectionedDataSource(
            tasks = taskDao.fetchTasks(preferences, filter),
            disableHeaders = filter.disableHeaders()
                    || (filter.supportsManualSort() && preferences.isManualSort)
                    || (filter is AstridOrderingFilter && preferences.isAstridSort),
            groupMode = preferences.groupMode,
            subtaskMode = preferences.subtaskMode,
            completedAtBottom = preferences.completedTasksAtBottom,
            collapsed = collapsed,
        )
        return Tasks.newBuilder()
            .setTotalItems(payload.size)
            .addAllItems(
                payload
                    .subList(position, position + limit)
                    .map { item ->
                        when (item) {
                            is UiItem.Header ->
                                GrpcProto.UiItem.newBuilder()
                                    .setId(item.value)
                                    .setType(GrpcProto.UiItemType.Header)
                                    .setTitle(headerFormatter.headerString(item.value))
                                    .setCollapsed(collapsed.contains(item.value))
                                    .build()

                            is UiItem.Task ->
                                GrpcProto.UiItem.newBuilder()
                                    .setType(GrpcProto.UiItemType.Task)
                                    .setId(item.task.id)
                                    .setPriority(item.task.priority)
                                    .setCompleted(item.task.isCompleted)
                                    .setHidden(item.task.task.isHidden)
                                    .apply {
                                        if (item.task.title != null) {
                                            setTitle(item.task.title)
                                        }
                                    }
                                    .setRepeating(item.task.task.isRecurring)
                                    .build()
                        }
                }
            )
            .build()
    }

    override suspend fun completeTask(request: CompleteTaskRequest): CompleteTaskResponse {
        taskCompleter.setComplete(request.id, request.completed)
        return CompleteTaskResponse.newBuilder().setSuccess(true).build()
    }

    override suspend fun toggleGroup(request: ToggleGroupRequest): ToggleGroupResponse {
        settings.updateData {
            it.copy {
                if (request.collapsed) {
                    if (!collapsed.contains(request.value)) {
                        collapsed.add(request.value)
                    }
                } else {
                    if (collapsed.contains(request.value)) {
                        collapsed.clear()
                        collapsed.addAll(it.collapsedList.toMutableList().apply { remove(request.value) })
                    }
                }
            }
        }

        return ToggleGroupResponse.getDefaultInstance()
    }
}
