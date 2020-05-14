package com.todoroo.astrid.adapter

import com.todoroo.astrid.dao.TaskDao
import org.tasks.BuildConfig
import org.tasks.data.GoogleTaskDao

class GoogleTaskAdapter internal constructor(taskDao: TaskDao, googleTaskDao: GoogleTaskDao, private val newTasksOnTop: Boolean) : GoogleTaskManualSortAdapter(taskDao, googleTaskDao) {
    override fun supportsManualSorting() = false

    override fun moved(from: Int, to: Int, indent: Int) {
        val task = getTask(from)
        val googleTask = task.googleTask
        val previous = if (to > 0) getTask(to - 1) else null
        if (indent == 0) {
            if (googleTask.indent == 0) {
                return
            }
            googleTaskDao.move(
                    googleTask, 0, if (newTasksOnTop) 0 else googleTaskDao.getBottom(googleTask.listId, 0))
        } else {
            val newParent = if (previous!!.hasParent()) previous.parent else previous.id
            if (googleTask.parent == newParent) {
                return
            }
            googleTaskDao.move(
                    googleTask,
                    newParent,
                    if (newTasksOnTop) 0 else googleTaskDao.getBottom(googleTask.listId, newParent))
        }
        taskDao.touch(task.id)
        if (BuildConfig.DEBUG) {
            googleTaskDao.validateSorting(task.googleTaskList)
        }
    }
}