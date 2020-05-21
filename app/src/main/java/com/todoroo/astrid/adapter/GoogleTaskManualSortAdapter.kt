package com.todoroo.astrid.adapter

import com.todoroo.astrid.dao.TaskDao
import org.tasks.BuildConfig
import org.tasks.data.GoogleTaskDao
import org.tasks.data.TaskContainer

open class GoogleTaskManualSortAdapter internal constructor(val taskDao: TaskDao, val googleTaskDao: GoogleTaskDao) : TaskAdapter() {

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

    override fun supportsManualSorting() = true

    override fun moved(from: Int, to: Int, indent: Int) {
        val task = getTask(from)
        val googleTask = task.googleTask
        val previous = if (to > 0) getTask(to - 1) else null
        if (previous == null) {
            googleTaskDao.move(googleTask, 0, 0)
        } else if (to == count || to <= from) {
            when {
                indent == 0 -> googleTaskDao.move(googleTask, 0, previous.getPrimarySort() + if (to == count) 0 else 1)
                previous.hasParent() && previous.parent == googleTask.parent -> googleTaskDao.move(googleTask, previous.parent, previous.getSecondarySort() + if (to == count) 0 else 1)
                previous.hasParent() -> googleTaskDao.move(googleTask, previous.parent, previous.getSecondarySort() + 1)
                else -> googleTaskDao.move(googleTask, previous.id, 0)
            }
        } else {
            when {
                indent == 0 -> googleTaskDao.move(googleTask, 0, previous.getPrimarySort() + if (task.hasParent()) 1 else 0)
                previous.hasParent() && previous.parent == googleTask.parent -> googleTaskDao.move(googleTask, previous.parent, previous.getSecondarySort())
                previous.hasParent() -> googleTaskDao.move(googleTask, previous.parent, previous.getSecondarySort() + 1)
                else -> googleTaskDao.move(googleTask, previous.id, 0)
            }
        }
        taskDao.touch(task.id)
        if (BuildConfig.DEBUG) {
            googleTaskDao.validateSorting(task.googleTaskList)
        }
    }
}