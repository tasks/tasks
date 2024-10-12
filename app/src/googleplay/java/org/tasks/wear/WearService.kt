package org.tasks.wear

import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskCompleter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.GrpcProto.CompleteTaskRequest
import org.tasks.GrpcProto.CompleteTaskResponse
import org.tasks.GrpcProto.GetTasksRequest
import org.tasks.GrpcProto.Task
import org.tasks.GrpcProto.Tasks
import org.tasks.WearServiceGrpcKt
import org.tasks.filters.MyTasksFilter
import org.tasks.preferences.Preferences

class WearService(
    private val taskDao: TaskDao,
    private val preferences: Preferences,
    private val taskCompleter: TaskCompleter,
) : WearServiceGrpcKt.WearServiceCoroutineImplBase() {

    override suspend fun getTasks(request: GetTasksRequest): Tasks {
        return Tasks.newBuilder()
            .addAllTasks(getTasks())
            .build()
    }

    override suspend fun completeTask(request: CompleteTaskRequest): CompleteTaskResponse {
        taskCompleter.setComplete(request.id, request.completed)
        return CompleteTaskResponse.newBuilder().setSuccess(true).build()
    }

    private suspend fun getTasks(): List<Task> = withContext(Dispatchers.IO) {
        val tasks = taskDao.fetchTasks(preferences, MyTasksFilter.create())
        return@withContext tasks.map {
            Task.newBuilder()
                .setId(it.task.id)
                .setPriority(it.task.priority)
                .setCompleted(it.task.isCompleted)
                .apply {
                    if (it.task.title != null) {
                        setTitle(it.task.title)
                    }
                }
                .setRepeating(it.task.isRecurring)
                .build()
        }
    }
}
