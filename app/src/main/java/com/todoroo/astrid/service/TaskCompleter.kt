package com.todoroo.astrid.service

import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import org.tasks.data.GoogleTaskDao
import timber.log.Timber
import javax.inject.Inject

class TaskCompleter @Inject internal constructor(
        private val taskDao: TaskDao,
        private val googleTaskDao: GoogleTaskDao) {

    suspend fun setComplete(taskId: Long) =
            taskDao
                    .fetch(taskId)
                    ?.let { setComplete(it, true) }
                    ?: Timber.e("Could not find task $taskId")

    suspend fun setComplete(item: Task, completed: Boolean) {
        val completionDate = if (completed) DateUtilities.now() else 0L
        completeChildren(item.id, completionDate)
        setComplete(listOf(item), completionDate)
    }

    suspend fun completeChildren(id: Long, completionDate: Long) {
        googleTaskDao
                .getChildTasks(id)
                .plus(taskDao.getChildren(id)
                        .takeIf { it.isNotEmpty() }
                        ?.let { taskDao.fetch(it) }
                        ?: emptyList()
                )
                .filter { it.isCompleted != completionDate > 0 }
                .let { setComplete(it, completionDate)}
    }

    private suspend fun setComplete(tasks: List<Task>, completionDate: Long) {
        tasks.forEachIndexed { i, task ->
            task.completionDate = completionDate
            if (i < tasks.size - 1) {
                task.suppressRefresh()
            }
            taskDao.save(task)
        }
    }
}