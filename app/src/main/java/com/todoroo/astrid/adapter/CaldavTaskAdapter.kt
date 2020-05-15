package com.todoroo.astrid.adapter

import com.todoroo.astrid.dao.TaskDao
import org.tasks.data.CaldavDao

class CaldavTaskAdapter internal constructor(taskDao: TaskDao, caldavDao: CaldavDao) : CaldavManualSortTaskAdapter(taskDao, caldavDao) {
    override fun supportsManualSorting() = false

    override fun moved(from: Int, to: Int, indent: Int) {
        val task = getTask(from)
        val previous = if (to > 0) getTask(to - 1) else null
        var newParent = task.parent
        if (indent == 0) {
            newParent = 0
        } else if (previous != null) {
            when {
                indent == previous.getIndent() -> {
                    newParent = previous.parent
                }
                indent > previous.getIndent() -> {
                    newParent = previous.id
                }
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
    }
}