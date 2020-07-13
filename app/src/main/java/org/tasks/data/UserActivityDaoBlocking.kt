package org.tasks.data

import javax.inject.Inject

@Deprecated("use coroutines")
class UserActivityDaoBlocking @Inject constructor(private val dao: UserActivityDao) {
    fun getCommentsForTask(taskUuid: String): List<UserActivity> = runBlocking {
        dao.getCommentsForTask(taskUuid)
    }
}