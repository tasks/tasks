package org.tasks.notifications

import org.tasks.data.runBlocking
import javax.inject.Inject

@Deprecated("use coroutines")
class NotificationDaoBlocking @Inject constructor(private val dao: NotificationDao) {
    fun getAll(): List<Long> = runBlocking {
        dao.getAll()
    }

    fun getAllOrdered(): List<Notification> = runBlocking {
        dao.getAllOrdered()
    }

    fun insertAll(notifications: List<Notification>) = runBlocking {
        dao.insertAll(notifications)
    }

    fun delete(taskId: Long) = runBlocking {
        dao.delete(taskId)
    }

    fun deleteAll(taskIds: List<Long>) = runBlocking {
        dao.deleteAll(taskIds)
    }

    fun latestTimestamp(): Long = runBlocking {
        dao.latestTimestamp()
    }
}