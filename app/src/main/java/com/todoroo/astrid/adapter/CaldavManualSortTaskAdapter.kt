package com.todoroo.astrid.adapter

import com.todoroo.astrid.dao.TaskDao
import org.tasks.data.CaldavDao

class CaldavManualSortTaskAdapter internal constructor(private val taskDao: TaskDao, private val caldavDao: CaldavDao) : CaldavTaskAdapter(taskDao, caldavDao) {
    override fun supportsManualSorting() = true

    override fun moved(from: Int, to: Int, indent: Int) {
        val task = getTask(from)
        val previous = if (to > 0) getTask(to - 1) else null
        val newParent = findNewParent(task, indent, to)

        if (from == to) {
            return
        }

        if (from != to) {
            val newPosition = when {
                previous == null -> 1
                indent > previous.getIndent() -> 1
                indent == previous.getIndent() -> previous.caldavSortOrder + 1
                else -> getTask((to - 1 downTo 0).find { getTask(it).indent == indent }!!).caldavSortOrder + 1
            }
            caldavDao.move(task, newParent, newPosition)
        }

        taskDao.touch(task.id)
    }
}