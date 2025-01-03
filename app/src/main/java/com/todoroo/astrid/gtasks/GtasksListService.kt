/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks

import com.google.api.services.tasks.model.TaskList
import com.todoroo.astrid.service.TaskDeleter
import org.tasks.LocalBroadcastManager
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import javax.inject.Inject

class GtasksListService @Inject constructor(
    private val caldavDao: CaldavDao,
    private val taskDeleter: TaskDeleter,
    private val localBroadcastManager: LocalBroadcastManager,
) {

    /**
     * Reads in remote list information and updates local list objects.
     *
     * @param remoteLists remote information about your lists
     */
    suspend fun updateLists(account: CaldavAccount, remoteLists: List<TaskList>) {
        val lists = caldavDao.getCalendarsByAccount(account.uuid!!)
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
            caldavDao.insertOrReplace(local)
            previousLists.remove(local.id)
        }

        // check for lists that aren't on remote server
        for (listId in previousLists) {
            taskDeleter.delete(caldavDao.getCalendarById(listId)!!)
        }
        localBroadcastManager.broadcastRefreshList()
    }
}
