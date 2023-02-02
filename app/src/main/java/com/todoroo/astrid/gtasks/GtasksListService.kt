/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks

import com.google.api.services.tasks.model.TaskList
import com.todoroo.astrid.service.TaskDeleter
import org.tasks.LocalBroadcastManager
import org.tasks.data.GoogleTaskAccount
import org.tasks.data.GoogleTaskList
import org.tasks.data.GoogleTaskListDao
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class GtasksListService @Inject constructor(
        private val googleTaskListDao: GoogleTaskListDao,
        private val taskDeleter: TaskDeleter,
        private val localBroadcastManager: LocalBroadcastManager) {

    /**
     * Reads in remote list information and updates local list objects.
     *
     * @param remoteLists remote information about your lists
     */
    suspend fun updateLists(account: GoogleTaskAccount, remoteLists: List<TaskList>) {
        val lists = googleTaskListDao.getLists(account.account!!)
        val previousLists: MutableSet<Long> = HashSet()
        for (list in lists) {
            previousLists.add(list.id)
        }
        for (i in remoteLists.indices) {
            val remote = remoteLists[i]
            val id = remote.id
            var local: GoogleTaskList? = null
            for (list in lists) {
                if (list.remoteId == id) {
                    local = list
                    break
                }
            }
            val title = remote.title
            if (local == null) {
                val byRemoteId = googleTaskListDao.findExistingList(id)
                if (byRemoteId != null) {
                    byRemoteId.account = account.account
                    local = byRemoteId
                } else {
                    Timber.d("Adding new gtask list %s", title)
                    local = GoogleTaskList()
                    local.account = account.account
                    local.remoteId = id
                }
            }
            local.title = title
            googleTaskListDao.insertOrReplace(local)
            previousLists.remove(local.id)
        }

        // check for lists that aren't on remote server
        for (listId in previousLists) {
            taskDeleter.delete(googleTaskListDao.getById(listId)!!)
        }
        localBroadcastManager.broadcastRefreshList()
    }
}
