/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks

import com.google.api.services.tasks.model.TaskList
import com.todoroo.astrid.service.TaskDeleter
import org.tasks.LocalBroadcastManager
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.dao.GoogleTaskListDao
import javax.inject.Inject

class GtasksListService @Inject constructor(
    private val googleTaskListDao: GoogleTaskListDao,
    private val taskDeleter: TaskDeleter,
    private val localBroadcastManager: LocalBroadcastManager,
) {

    /**
     * Reads in remote list information and updates local list objects.
     *
     * @param remoteLists remote information about your lists
     */
    suspend fun updateLists(account: CaldavAccount, remoteLists: List<TaskList>) {
        val lists = googleTaskListDao.getLists(account.uuid!!)
        val previousLists: MutableSet<Long> = HashSet()
        for (list in lists) {
            previousLists.add(list.id)
        }
        for (i in remoteLists.indices) {
            val remote = remoteLists[i]
            val id = remote.id
            var local: CaldavCalendar? = null
            for (list in lists) {
                if (list.uuid == id) {
                    local = list
                    break
                }
            }
            if (local == null) {
                local = CaldavCalendar(
                    account = account.uuid,
                    uuid = id,
                )
            }
            local.name = remote.title
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
