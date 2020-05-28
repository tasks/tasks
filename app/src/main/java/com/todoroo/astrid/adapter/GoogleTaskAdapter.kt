package com.todoroo.astrid.adapter

import com.todoroo.astrid.dao.TaskDao
import org.tasks.BuildConfig
import org.tasks.data.GoogleTaskDao

open class GoogleTaskAdapter internal constructor(private val taskDao: TaskDao, private val googleTaskDao: GoogleTaskDao, private val newTasksOnTop: Boolean) : TaskAdapter() {

    override fun supportsParentingOrManualSort() = true

    override fun moved(from: Int, to: Int, indent: Int) {
        val task = getTask(from)
        val googleTask = task.googleTask
        val newParent = findParent(indent, to)?.id ?: 0
        if (googleTask.parent == newParent) {
            return
        }
        googleTaskDao.move(
                googleTask,
                newParent,
                if (newTasksOnTop) 0 else googleTaskDao.getBottom(googleTask.listId, newParent))
        taskDao.touch(task.id)
        if (BuildConfig.DEBUG) {
            googleTaskDao.validateSorting(task.googleTaskList!!)
        }
    }
}