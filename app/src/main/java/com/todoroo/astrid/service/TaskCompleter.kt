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

    fun setComplete(taskId: Long) =
            taskDao.fetch(taskId)?.let { setComplete(it, true) }
                    ?: Timber.e("Could not find task $taskId")

    fun setComplete(item: Task, completed: Boolean) {
        val completionDate = if (completed) DateUtilities.now() else 0L
        setComplete(listOf(item), completionDate)
        val tasks = googleTaskDao.getChildTasks(item.id)
                .plus(taskDao.getChildren(item.id).takeIf { it.isNotEmpty() }?.let(taskDao::fetch)
                        ?: emptyList())
                .filter { it.isCompleted != completed }
        setComplete(tasks, completionDate)
    }

    private fun setComplete(tasks: List<Task>, completionDate: Long) {
        tasks.forEachIndexed { i, task ->
            task.completionDate = completionDate
            if (i < tasks.size - 1) {
                task.suppressRefresh()
            }
            taskDao.save(task)
        }
    }
}