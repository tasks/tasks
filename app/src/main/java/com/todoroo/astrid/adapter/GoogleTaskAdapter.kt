package com.todoroo.astrid.adapter

import com.todoroo.astrid.dao.TaskDao
import org.tasks.BuildConfig
import org.tasks.data.GoogleTaskDao
import org.tasks.data.TaskContainer

open class GoogleTaskAdapter internal constructor(private val taskDao: TaskDao, private val googleTaskDao: GoogleTaskDao, private val newTasksOnTop: Boolean) : TaskAdapter() {

    override fun canMove(source: TaskContainer, from: Int, target: TaskContainer, to: Int): Boolean {
        return if (!source.hasChildren() || to <= 0 || to >= count - 1) {
            true
        } else if (from < to) {
            when {
                target.hasChildren() -> false
                target.hasParent() -> !getTask(to + 1).hasParent()
                else -> true
            }
        } else {
            when {
                target.hasChildren() -> true
                target.hasParent() -> target.parent == source.id && target.secondarySort == 0L
                else -> true
            }
        }
    }

    override fun maxIndent(previousPosition: Int, task: TaskContainer) = if (task.hasChildren()) 0 else 1

    override fun minIndent(nextPosition: Int, task: TaskContainer) = if (task.hasChildren() || !getTask(nextPosition).hasParent()) 0 else 1

    override fun supportsParentingOrManualSort() = true

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