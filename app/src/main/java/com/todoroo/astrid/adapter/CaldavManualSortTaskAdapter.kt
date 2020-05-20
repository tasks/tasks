package com.todoroo.astrid.adapter

import com.todoroo.astrid.dao.TaskDao
import org.tasks.data.CaldavDao

class CaldavManualSortTaskAdapter internal constructor(private val taskDao: TaskDao, private val caldavDao: CaldavDao) : CaldavTaskAdapter(taskDao, caldavDao) {
    override fun supportsManualSorting() = true

    override fun moved(from: Int, to: Int, indent: Int) {
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
    }
}