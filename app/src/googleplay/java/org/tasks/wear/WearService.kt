package org.tasks.wear

import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskCompleter
import org.tasks.GrpcProto
import org.tasks.GrpcProto.CompleteTaskRequest
import org.tasks.GrpcProto.CompleteTaskResponse
import org.tasks.GrpcProto.GetTasksRequest
import org.tasks.GrpcProto.Tasks
import org.tasks.WearServiceGrpcKt
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
) : WearServiceGrpcKt.WearServiceCoroutineImplBase() {
    override suspend fun getTasks(request: GetTasksRequest): Tasks {
        val position = request.position
        val limit = request.limit.takeIf { it > 0 } ?: Int.MAX_VALUE
        val filter = MyTasksFilter.create()
        val payload = SectionedDataSource(
            tasks = taskDao.fetchTasks(preferences, filter),
            disableHeaders = filter.disableHeaders()
                    || (filter.supportsManualSort() && preferences.isManualSort)
                    || (filter is AstridOrderingFilter && preferences.isAstridSort),
            groupMode = preferences.groupMode,
            subtaskMode = preferences.subtaskMode,
            completedAtBottom = preferences.completedTasksAtBottom,
        )
        return Tasks.newBuilder()
            .addAllItems(
                payload
                    .subList(position, position + limit)
                    .map { item ->
                        when (item) {
                            is UiItem.Header ->
                                GrpcProto.UiItem.newBuilder()
                                    .setType(GrpcProto.UiItemType.Header)
                                    .setTitle(headerFormatter.headerString(item.value))
                                    .build()

                            is UiItem.Task ->
                                GrpcProto.UiItem.newBuilder()
                                    .setType(GrpcProto.UiItemType.Task)
                                    .setId(item.task.id)
                                    .setPriority(item.task.priority)
                                    .setCompleted(item.task.isCompleted)
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
}
