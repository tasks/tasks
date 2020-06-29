package org.tasks.data

import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Deprecated("use coroutines")
class DeletionDaoBlocking @Inject constructor(private val dao: DeletionDao) {
    fun deleteCaldavTasks(ids: List<Long>) = runBlocking {
        dao.deleteCaldavTasks(ids)
    }

    fun deleteGoogleTasks(ids: List<Long>) = runBlocking {
        dao.deleteGoogleTasks(ids)
    }

    fun deleteTags(ids: List<Long>) = runBlocking {
        dao.deleteTags(ids)
    }

    fun deleteGeofences(ids: List<Long>) = runBlocking {
        dao.deleteGeofences(ids)
    }

    fun deleteAlarms(ids: List<Long>) = runBlocking {
        dao.deleteAlarms(ids)
    }

    fun deleteTasks(ids: List<Long>) = runBlocking {
        dao.deleteTasks(ids)
    }

    fun delete(ids: List<Long>) = runBlocking {
        dao.delete(ids)
    }

    fun markDeletedInternal(ids: List<Long>) = runBlocking {
        dao.markDeletedInternal(ids)
    }

    fun markDeleted(ids: Iterable<Long>) = runBlocking {
        dao.markDeleted(ids)
    }

    fun getActiveGoogleTasks(listId: String): List<Long> = runBlocking {
        dao.getActiveGoogleTasks(listId)
    }

    fun deleteGoogleTaskList(googleTaskList: GoogleTaskList) = runBlocking {
        dao.deleteGoogleTaskList(googleTaskList)
    }

    fun delete(googleTaskList: GoogleTaskList): List<Long> = runBlocking {
        dao.delete(googleTaskList)
    }

    fun deleteGoogleTaskAccount(googleTaskAccount: GoogleTaskAccount) = runBlocking {
        dao.deleteGoogleTaskAccount(googleTaskAccount)
    }

    fun getLists(account: String): List<GoogleTaskList> = runBlocking {
        dao.getLists(account)
    }

    fun delete(googleTaskAccount: GoogleTaskAccount): List<Long> = runBlocking {
        dao.delete(googleTaskAccount)
    }

    fun getActiveCaldavTasks(calendar: String): List<Long> = runBlocking {
        dao.getActiveCaldavTasks(calendar)
    }

    fun deleteCaldavCalendar(caldavCalendar: CaldavCalendar) = runBlocking {
        dao.deleteCaldavCalendar(caldavCalendar)
    }

    fun delete(caldavCalendar: CaldavCalendar): List<Long> = runBlocking {
        dao.delete(caldavCalendar)
    }

    fun getCalendars(account: String): List<CaldavCalendar> = runBlocking {
        dao.getCalendars(account)
    }

    fun deleteCaldavAccount(caldavAccount: CaldavAccount) = runBlocking {
        dao.deleteCaldavAccount(caldavAccount)
    }

    fun purgeDeleted() = runBlocking {
        dao.purgeDeleted()
    }

    fun delete(caldavAccount: CaldavAccount): List<Long> = runBlocking {
        dao.delete(caldavAccount)
    }
}