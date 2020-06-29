package org.tasks.data

import androidx.lifecycle.LiveData
import kotlinx.coroutines.runBlocking
import org.tasks.filters.GoogleTaskFilters
import org.tasks.time.DateTimeUtils.currentTimeMillis
import javax.inject.Inject

@Deprecated("use coroutines")
class GoogleTaskListDaoBlocking @Inject constructor(private val dao: GoogleTaskListDao) {
    fun accountCount(): Int = runBlocking {
        dao.accountCount()
    }

    fun getAccounts(): List<GoogleTaskAccount> = runBlocking {
        dao.getAccounts()
    }

    fun getAccount(account: String): GoogleTaskAccount? = runBlocking {
        dao.getAccount(account)
    }

    fun getById(id: Long): GoogleTaskList? = runBlocking {
        dao.getById(id)
    }

    fun getLists(account: String): List<GoogleTaskList> = runBlocking {
        dao.getLists(account)
    }

    fun getByRemoteId(remoteId: String): GoogleTaskList? = runBlocking {
        dao.getByRemoteId(remoteId)
    }

    fun getByRemoteId(remoteIds: List<String>): List<GoogleTaskList> = runBlocking {
        dao.getByRemoteId(remoteIds)
    }

    fun subscribeToLists(): LiveData<List<GoogleTaskList>> {
        return dao.subscribeToLists()
    }

    fun findExistingList(remoteId: String): GoogleTaskList? = runBlocking {
        dao.findExistingList(remoteId)
    }

    fun getAllLists(): List<GoogleTaskList> = runBlocking {
        dao.getAllLists()
    }

    fun resetLastSync(account: String) = runBlocking {
        dao.resetLastSync(account)
    }

    fun insertOrReplace(googleTaskList: GoogleTaskList): Long = runBlocking {
        dao.insertOrReplace(googleTaskList)
    }

    fun insert(googleTaskList: GoogleTaskList): Long = runBlocking {
        dao.insert(googleTaskList)
    }

    fun insert(googleTaskAccount: GoogleTaskAccount) = runBlocking {
        dao.insert(googleTaskAccount)
    }

    fun update(account: GoogleTaskAccount) = runBlocking {
        dao.update(account)
    }

    fun update(list: GoogleTaskList) = runBlocking {
        dao.update(list)
    }

    fun getGoogleTaskFilters(account: String, now: Long = currentTimeMillis()): List<GoogleTaskFilters> = runBlocking {
        dao.getGoogleTaskFilters(account, now)
    }

    fun resetOrders() = runBlocking {
        dao.resetOrders()
    }

    fun setOrder(id: Long, order: Int) = runBlocking {
        dao.setOrder(id, order)
    }
}