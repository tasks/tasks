@file:Suppress("ClassName")

package com.todoroo.astrid.service

import co.touchlab.kermit.Logger
import org.tasks.caldav.VtodoCache
import org.tasks.caldav.iCalendar
import org.tasks.caldav.taskStatus
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.UpgraderDao
import org.tasks.data.db.DbUtils.dbchunk
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.Task
import org.tasks.service.Upgrade
import kotlin.coroutines.cancellation.CancellationException

class Upgrade_15_15_Status(
    private val upgraderDao: UpgraderDao,
    private val caldavDao: CaldavDao,
    private val vtodoCache: VtodoCache,
    private val readOpenTasksStatuses: suspend () -> Unit = {},
) : Upgrade {
    override suspend fun run() {
        readCachedStatuses()
        try {
            readOpenTasksStatuses()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(e, tag = TAG) { "Failed to read OpenTasks statuses" }
            upgraderDao.clearOpenTasksCtags()
            upgraderDao.clearOpenTasksEtags()
        }
    }

    suspend fun readCachedStatuses() {
        val calendars = caldavDao.getCalendars().associateBy { it.uuid }
        upgraderDao
            .tasksWithCachedVtodos()
            .mapNotNull { container ->
                val caldavTask = container.caldavTask
                val calendar = calendars[caldavTask.calendar] ?: return@mapNotNull null
                val status = try {
                    vtodoCache.getVtodo(calendar, caldavTask)?.let(::statusFromVtodo)
                } catch (e: Exception) {
                    Logger.e(e, tag = TAG) { "Failed to read status for task ${caldavTask.task}" }
                    null
                }
                status
                    ?.takeIf { it != Task.Status.NEEDS_ACTION }
                    ?.let { caldavTask.task to it }
            }
            .groupBy({ it.second }, { it.first })
            .forEach { (status, ids) ->
                ids.dbchunk().forEach { upgraderDao.setStatus(it, status) }
            }
    }

    companion object {
        private const val TAG = "Upgrade_15_15_Status"
        const val VERSION = Upgrade_15_15.VERSION
        val CACHED_VTODO_TYPES = setOf(TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE)

        fun statusFromVtodo(vtodo: String): Int? = iCalendar.fromVtodo(vtodo)?.taskStatus
    }
}
