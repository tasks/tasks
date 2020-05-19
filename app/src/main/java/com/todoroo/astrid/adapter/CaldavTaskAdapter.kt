package com.todoroo.astrid.adapter

import com.todoroo.astrid.dao.TaskDao
import org.tasks.data.CaldavDao

class CaldavTaskAdapter internal constructor(taskDao: TaskDao, caldavDao: CaldavDao) : CaldavManualSortTaskAdapter(taskDao, caldavDao) {
    override fun supportsManualSorting() = false

    override fun moved(from: Int, to: Int, indent: Int) {
        val task = getTask(from)
        val newParent = findNewParent(indent, to)
        if (newParent != task.parent) {
            changeParent(task, newParent)
        }
    }
}