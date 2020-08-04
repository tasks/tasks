package org.tasks.data

import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Deprecated("use coroutines")
class GoogleTaskListDaoBlocking @Inject constructor(private val dao: GoogleTaskListDao) {
    fun getAccounts(): List<GoogleTaskAccount> = runBlocking {
        dao.getAccounts()
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

    fun findExistingList(remoteId: String): GoogleTaskList? = runBlocking {
        dao.findExistingList(remoteId)
    }

    fun getAllLists(): List<GoogleTaskList> = runBlocking {
        dao.getAllLists()
    }

    fun insertOrReplace(googleTaskList: GoogleTaskList): Long = runBlocking {
        dao.insertOrReplace(googleTaskList)
    }

    fun insert(googleTaskAccount: GoogleTaskAccount) = runBlocking {
        dao.insert(googleTaskAccount)
    }
}