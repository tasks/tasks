package com.todoroo.astrid.adapter

import com.todoroo.astrid.dao.TaskDao
import org.tasks.data.CaldavDao
import org.tasks.data.TaskContainer
import org.tasks.tasklist.ViewHolder

class CaldavTaskAdapter internal constructor(private val taskDao: TaskDao, private val caldavDao: CaldavDao) : TaskAdapter() {
    override fun canMove(sourceVh: ViewHolder, targetVh: ViewHolder): Boolean {
        val source = sourceVh.task
        val to = targetVh.adapterPosition
        return !taskIsChild(source, to)
    }

    override fun maxIndent(previousPosition: Int, task: TaskContainer): Int {
        val previous = getTask(previousPosition)
        return previous.getIndent() + 1
    }

    override fun minIndent(nextPosition: Int, task: TaskContainer) = 0

    override fun supportsParentingOrManualSort() = true

    override fun moved(from: Int, to: Int, indent: Int) {
        val task = getTask(from)
        val previous = if (to > 0) getTask(to - 1) else null
        var newParent = task.parent
        if (indent == 0) {
            newParent = 0
        } else if (previous != null) {
            when {
                indent == previous.getIndent() -> newParent = previous.parent
                indent > previous.getIndent() -> newParent = previous.id
                indent < previous.getIndent() -> {
                    newParent = previous.parent
                    var currentIndex = to
                    for (i in 0 until previous.getIndent() - indent) {
                        var thisParent = newParent
                        while (newParent == thisParent) {
                            thisParent = getTask(--currentIndex).parent
                        }
                        newParent = thisParent
                    }
                }
            }
        }

        // If nothing is changing, return
        if (newParent == task.parent) {
            return
        }
        changeParent(task, newParent)
        taskDao.touch(task.id)
    }

    private fun changeParent(task: TaskContainer, newParent: Long) {
        val caldavTask = task.getCaldavTask()
        if (newParent == 0L) {
            caldavTask.remoteParent = ""
            task.parent = 0
        } else {
            val parentTask = caldavDao.getTask(newParent) ?: return
            caldavTask.remoteParent = parentTask.remoteId
            task.parent = newParent
        }
        caldavDao.update(caldavTask)
        taskDao.save(task.getTask())
    }

    private fun taskIsChild(source: TaskContainer, destinationIndex: Int): Boolean {
        (destinationIndex downTo 0).forEach {
            when (getTask(it).parent) {
                0L -> return false
                source.parent -> return false
                source.id -> return true
            }
        }
        return false
    }
}