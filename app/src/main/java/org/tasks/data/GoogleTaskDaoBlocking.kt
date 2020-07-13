package org.tasks.data

import javax.inject.Inject

@Deprecated("use coroutines")
class GoogleTaskDaoBlocking @Inject constructor(private val dao: GoogleTaskDao) {
    fun insertAndShift(task: GoogleTask, top: Boolean) = runBlocking {
        dao.insertAndShift(task, top)
    }

    fun setCollapsed(id: Long, collapsed: Boolean) = runBlocking {
        dao.setCollapsed(id, collapsed)
    }

    fun getByTaskId(taskId: Long): GoogleTask? = runBlocking {
        dao.getByTaskId(taskId)
    }
}