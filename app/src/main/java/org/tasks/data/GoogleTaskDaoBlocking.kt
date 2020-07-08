package org.tasks.data

import com.todoroo.astrid.data.Task
import org.tasks.time.DateTimeUtils.currentTimeMillis
import javax.inject.Inject

@Deprecated("use coroutines")
class GoogleTaskDaoBlocking @Inject constructor(private val dao: GoogleTaskDao) {
    fun insert(task: GoogleTask): Long = runBlocking {
        dao.insert(task)
    }

    fun insert(tasks: Iterable<GoogleTask>) = runBlocking {
        dao.insert(tasks)
    }

    fun insertAndShift(task: GoogleTask, top: Boolean) = runBlocking {
        dao.insertAndShift(task, top)
    }

    fun move(task: SubsetGoogleTask, newParent: Long, newPosition: Long) = runBlocking {
        dao.move(task, newParent, newPosition)
    }

    fun setCollapsed(id: Long, collapsed: Boolean) = runBlocking {
        dao.setCollapsed(id, collapsed)
    }

    fun getByTaskId(taskId: Long): GoogleTask? = runBlocking {
        dao.getByTaskId(taskId)
    }

    fun update(googleTask: GoogleTask) = runBlocking {
        dao.update(googleTask)
    }

    fun update(id: Long, parent: Long, order: Long) = runBlocking {
        dao.update(id, parent, order)
    }

    fun markDeleted(task: Long, now: Long = currentTimeMillis()) = runBlocking {
        dao.markDeleted(task, now)
    }

    fun delete(deleted: GoogleTask) = runBlocking {
        dao.delete(deleted)
    }

    fun getByRemoteId(remoteId: String): GoogleTask? = runBlocking {
        dao.getByRemoteId(remoteId)
    }

    fun getDeletedByTaskId(taskId: Long): List<GoogleTask> = runBlocking {
        dao.getDeletedByTaskId(taskId)
    }

    fun getAllByTaskId(taskId: Long): List<GoogleTask> = runBlocking {
        dao.getAllByTaskId(taskId)
    }

    fun getLists(tasks: List<Long>): List<String> = runBlocking {
        dao.getLists(tasks)
    }

    fun getChildren(ids: List<Long>): List<Long> = runBlocking {
        dao.getChildren(ids)
    }

    fun getChildTasks(taskId: Long): List<Task> = runBlocking {
        dao.getChildTasks(taskId)
    }

    fun getChildren(id: Long): List<GoogleTask> = runBlocking {
        dao.getChildren(id)
    }

    fun getBottom(listId: String, parent: Long): Long = runBlocking {
        dao.getBottom(listId, parent)
    }

    fun getPrevious(listId: String, parent: Long, order: Long): String? = runBlocking {
        dao.getPrevious(listId, parent, order)
    }

    fun getRemoteId(task: Long): String? = runBlocking {
        dao.getRemoteId(task)
    }

    fun getTask(remoteId: String): Long = runBlocking {
        dao.getTask(remoteId)
    }

    fun getByLocalOrder(listId: String): List<GoogleTask> = runBlocking {
        dao.getByLocalOrder(listId)
    }

    fun updateParents() = runBlocking {
        dao.updateParents()
    }

    fun updateParents(listId: String) = runBlocking {
        dao.updateParents(listId)
    }

    fun updatePosition(id: String, parent: String?, position: String) = runBlocking {
        dao.updatePosition(id, parent, position)
    }

    fun reposition(listId: String) = runBlocking {
        dao.reposition(listId)
    }

    fun validateSorting(listId: String) = runBlocking {
        dao.validateSorting(listId)
    }
}