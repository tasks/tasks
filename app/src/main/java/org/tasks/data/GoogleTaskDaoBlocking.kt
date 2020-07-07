package org.tasks.data

import javax.inject.Inject

@Deprecated("use coroutines")
class GoogleTaskDaoBlocking @Inject constructor(private val dao: GoogleTaskDao) {
    fun insert(task: GoogleTask): Long = runBlocking {
        dao.insert(task)
    }

    fun insertAndShift(task: GoogleTask, top: Boolean) = runBlocking {
        dao.insertAndShift(task, top)
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

    fun getChildren(ids: List<Long>): List<Long> = runBlocking {
        dao.getChildren(ids)
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

    fun updateParents() = runBlocking {
        dao.updateParents()
    }

    fun updatePosition(id: String, parent: String?, position: String) = runBlocking {
        dao.updatePosition(id, parent, position)
    }

    fun reposition(listId: String) = runBlocking {
        dao.reposition(listId)
    }
}