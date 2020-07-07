package org.tasks.data

import javax.inject.Inject

@Deprecated("use coroutines")
class DeletionDaoBlocking @Inject constructor(private val dao: DeletionDao) {
    fun delete(ids: List<Long>) = runBlocking {
        dao.delete(ids)
    }

    fun markDeleted(ids: Iterable<Long>) = runBlocking {
        dao.markDeleted(ids)
    }

    fun delete(googleTaskList: GoogleTaskList): List<Long> = runBlocking {
        dao.delete(googleTaskList)
    }

    fun delete(googleTaskAccount: GoogleTaskAccount): List<Long> = runBlocking {
        dao.delete(googleTaskAccount)
    }

    fun delete(caldavCalendar: CaldavCalendar): List<Long> = runBlocking {
        dao.delete(caldavCalendar)
    }

    fun purgeDeleted() = runBlocking {
        dao.purgeDeleted()
    }

    fun delete(caldavAccount: CaldavAccount): List<Long> = runBlocking {
        dao.delete(caldavAccount)
    }
}