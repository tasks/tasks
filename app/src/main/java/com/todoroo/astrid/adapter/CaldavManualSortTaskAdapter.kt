package com.todoroo.astrid.adapter

import com.todoroo.astrid.dao.TaskDao
import org.tasks.LocalBroadcastManager
import org.tasks.data.CaldavDao
import org.tasks.data.GoogleTaskDao
import org.tasks.data.TaskContainer

class CaldavManualSortTaskAdapter internal constructor(
        googleTaskDao: GoogleTaskDao,
        private val caldavDao: CaldavDao,
        private val taskDao: TaskDao,
        private val localBroadcastManager: LocalBroadcastManager)
    : TaskAdapter(false, googleTaskDao, caldavDao, taskDao, localBroadcastManager) {

    override suspend fun moved(from: Int, to: Int, indent: Int) {
        val task = getTask(from)
        val oldParent = task.parent
        val newParent = changeParent(task, indent, to)

        if (oldParent == newParent && from == to) {
            return
        }

        val previous = if (to > 0) getTask(to - 1) else null
        val next = if (to < count) getTask(to) else null

        val newPosition = when {
            previous == null -> next!!.caldavSortOrder - 1
            indent > previous.getIndent() && next?.indent == indent -> next.caldavSortOrder - 1
            indent > previous.getIndent() -> null
            indent == previous.getIndent() -> previous.caldavSortOrder + 1
            else -> getTask((to - 1 downTo 0).find { getTask(it).indent == indent }!!).caldavSortOrder + 1
        }
        caldavDao.move(task, newParent, newPosition)
        taskDao.touch(task.id)
        localBroadcastManager.broadcastRefresh()
    }

    private suspend fun changeParent(task: TaskContainer, indent: Int, to: Int): Long {
        val newParent = findParent(indent, to)?.id ?: 0
        if (task.parent != newParent) {
            changeParent(task, newParent)
        }
        return newParent
    }

    private suspend fun changeParent(task: TaskContainer, newParent: Long) {
        val caldavTask = task.getCaldavTask()
        if (newParent == 0L) {
            caldavTask.remoteParent = ""
            task.parent = 0
        } else {
            val parentTask = caldavDao.getTask(newParent) ?: return
            caldavTask.remoteParent = parentTask.remoteId
            task.parent = newParent
        }
        caldavDao.update(caldavTask.id, caldavTask.remoteParent)
        taskDao.save(task.getTask(), null)
    }
}