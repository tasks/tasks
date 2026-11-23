@file:Suppress("ClassName")

package com.todoroo.astrid.service

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dmfs.tasks.contract.TaskContract.Tasks
import org.tasks.data.OpenTaskDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.UpgraderDao
import org.tasks.data.db.DbUtils.dbchunk
import org.tasks.data.entity.Task

class Upgrade_15_15_OpenTasksStatus(
    private val context: Context,
    private val openTaskDao: OpenTaskDao,
    private val caldavDao: CaldavDao,
    private val upgraderDao: UpgraderDao,
) {
    suspend fun readOpenTasksStatuses() {
        for (list in openTaskDao.getLists()) {
            val calendar = caldavDao.getCalendarByUrl(list.account!!, list.url ?: continue) ?: continue
            openTaskDao
                .getStatuses(list.id)
                .groupBy({ it.second }, { it.first })
                .forEach { (status, uids) ->
                    uids.dbchunk().forEach { upgraderDao.setStatus(calendar.uuid!!, it, status) }
                }
        }
    }

    private suspend fun OpenTaskDao.getStatuses(listId: Long): List<Pair<String, Int>> =
        withContext(Dispatchers.IO) {
            val items = ArrayList<Pair<String, Int>>()
            context.contentResolver.query(
                tasks,
                arrayOf(Tasks._UID, Tasks.STATUS),
                "${Tasks.LIST_ID} = $listId AND ${Tasks.COMPLETED} IS NULL AND ${Tasks.STATUS} IN (${Tasks.STATUS_IN_PROCESS}, ${Tasks.STATUS_CANCELLED})",
                null,
                null,
            )?.use {
                val uidIndex = it.getColumnIndexOrThrow(Tasks._UID)
                val statusIndex = it.getColumnIndexOrThrow(Tasks.STATUS)
                while (it.moveToNext()) {
                    val uid = it.getString(uidIndex)?.takeIf { uid -> uid.isNotBlank() } ?: continue
                    val status = when (it.getInt(statusIndex)) {
                        Tasks.STATUS_IN_PROCESS -> Task.Status.IN_PROCESS
                        else -> Task.Status.CANCELLED
                    }
                    items.add(uid to status)
                }
            }
            items
        }
}
