package org.tasks.data

import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Deprecated("use coroutines")
class UserActivityDaoBlocking @Inject constructor(private val dao: UserActivityDao) {
    fun insert(userActivity: UserActivity) = runBlocking {
        dao.insert(userActivity)
    }

    fun update(userActivity: UserActivity) = runBlocking {
        dao.update(userActivity)
    }

    fun delete(userActivity: UserActivity) = runBlocking {
        dao.delete(userActivity)
    }

    fun getCommentsForTask(taskUuid: String): List<UserActivity> = runBlocking {
        dao.getCommentsForTask(taskUuid)
    }

    fun getComments(task: Long): List<UserActivity> = runBlocking {
        dao.getComments(task)
    }

    fun getComments(): List<UserActivity> = runBlocking {
        dao.getComments()
    }

    fun createNew(item: UserActivity) = runBlocking {
        dao.createNew(item)
    }
}