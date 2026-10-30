package org.tasks.data

import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TaskDao
import org.tasks.filters.CaldavFilter

/**
 * Creates a new top-level task in a list and nests an existing task (and its
 * whole subtree) under it — the "make this task part of a bigger project"
 * action.
 *
 * Both halves deliberately reuse the app's existing write paths instead of
 * re-deriving commit recipes: creation mirrors the editor's new-task save
 * ([TaskDao.createNew] + [CaldavDao.insert] in one transaction, then a
 * [TaskSaver] pass so the row is marked for sync), and nesting goes through
 * [TaskMover.move] — the same code path the app uses when a task is moved to a
 * new parent. The new parent is empty, so the moved task keeping its existing
 * sort order on a same-list move places it at the end of its children.
 */
object MakeParentTask {

    /**
     * Creates a new top-level task in [list]. The returned [Task] has its row
     * id stamped. Throws if the underlying write fails; the caller is expected
     * to surface that, and nothing has been created when it does.
     */
    suspend fun create(
        taskDao: TaskDao,
        caldavDao: CaldavDao,
        taskSaver: TaskSaver,
        list: CaldavFilter,
        title: String,
    ): Task {
        val task = TaskCreator().createBlankTask(title.trim())
        taskDao.inTransaction {
            taskDao.createNew(task)
            caldavDao.insert(
                task = task,
                caldavTask = CaldavTask(task = task.id, calendar = list.uuid),
                addToTop = false,
            )
        }
        taskSaver.save(task, null)
        return task
    }

    /**
     * Nests the task [taskId] (and its subtree) as a direct child of
     * [parentId], staying in [listUuid]. Uses [TaskMover.move], which handles
     * the parent columns, the CalDAV remote parent, dirty marking and the
     * refresh broadcast the same way every other parent change in the app does.
     */
    suspend fun nest(
        taskMover: TaskMover,
        listUuid: String,
        taskId: Long,
        parentId: Long,
    ) {
        require(parentId > 0) { "the parent must be created first" }
        taskMover.move(taskId, listUuid, parentId)
    }
}
