package com.todoroo.astrid.adapter

import com.todoroo.astrid.dao.TaskDao
import org.tasks.data.CaldavDao
import org.tasks.data.TaskContainer
import org.tasks.date.DateTimeUtils.toAppleEpoch

open class CaldavTaskAdapter internal constructor(private val taskDao: TaskDao, private val caldavDao: CaldavDao, private val newTasksOnTop: Boolean = false) : TaskAdapter() {
    override fun supportsParentingOrManualSort() = true

    override fun moved(from: Int, to: Int, indent: Int) {
        val task = getTask(from)
        val newParent = changeParent(task, indent, to)
        if (newParent != task.parent) {
            val newPosition = if (newTasksOnTop) {
                caldavDao.findFirstTask(task.caldav!!, newParent)
                        ?.takeIf { task.creationDate.toAppleEpoch() >= it}
                        ?.minus(1)
            } else {
                caldavDao.findLastTask(task.caldav!!, newParent)
                        ?.takeIf { task.creationDate.toAppleEpoch() <= it }
                        ?.plus(1)
            }
            caldavDao.update(task.caldavTask.cd_id, newPosition)
        }
    }

    internal fun changeParent(task: TaskContainer, indent: Int, to: Int): Long {
        val newParent = findParent(indent, to)?.id ?: 0
        if (task.parent != newParent) {
            changeParent(task, newParent)
        }
        return newParent
    }

    private fun changeParent(task: TaskContainer, newParent: Long) {
        val caldavTask = task.getCaldavTask()
        if (newParent == 0L) {
            caldavTask.cd_remote_parent = ""
            task.parent = 0
        } else {
            val parentTask = caldavDao.getTask(newParent) ?: return
            caldavTask.cd_remote_parent = parentTask.remoteId
            task.parent = newParent
        }
        caldavDao.update(caldavTask.cd_id, caldavTask.cd_remote_parent)
        taskDao.save(task.getTask(), null)
    }
}