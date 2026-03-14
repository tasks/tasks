package org.tasks.service

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.VtodoCache
import org.tasks.data.dao.DeletionDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.db.SuspendDbUtils.chunkedMap
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Task
import org.tasks.filters.filterPreferencesKey
import org.tasks.preferences.FilterPreferences.Companion.delete
import org.tasks.preferences.TasksPreferences

class TaskDeleter(
    private val deletionDao: DeletionDao,
    private val taskDao: TaskDao,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val vtodoCache: VtodoCache,
    private val tasksPreferences: TasksPreferences,
    private val taskCleanup: TaskCleanup,
) {
    suspend fun markDeleted(item: Task) = markDeleted(listOf(item.id))

    suspend fun markDeleted(taskIds: List<Long>): List<Task> = withContext(NonCancellable) {
        val ids = taskIds
            .toSet()
            .plus(taskIds.chunkedMap(taskDao::getChildren))
            .let { taskDao.fetch(it.toList()) }
            .filterNot { it.readOnly }
            .map { it.id }
        deletionDao.markDeleted(
            ids = ids,
            cleanup = { taskCleanup.cleanup(it) }
        )
        taskCleanup.onMarkedDeleted()
        refreshBroadcaster.broadcastRefresh()
        taskDao.fetch(ids)
    }

    suspend fun delete(task: Task) = delete(task.id)

    suspend fun delete(task: Long) = delete(listOf(task))

    suspend fun delete(tasks: List<Long>) {
        deletionDao.delete(
            ids = tasks,
            cleanup = { taskCleanup.cleanup(it) }
        )
        refreshBroadcaster.broadcastRefresh()
    }

    suspend fun delete(list: CaldavCalendar) {
        vtodoCache.delete(list)
        deletionDao.delete(
            caldavCalendar = list,
            cleanup = { taskCleanup.cleanup(it) }
        )
        tasksPreferences.delete(list.filterPreferencesKey())
        refreshBroadcaster.broadcastRefresh()
    }

    suspend fun delete(account: CaldavAccount) {
        vtodoCache.delete(account)
        val calendars = deletionDao.delete(
            caldavAccount = account,
            cleanup = { taskCleanup.cleanup(it) }
        )
        calendars.forEach { tasksPreferences.delete(it.filterPreferencesKey()) }
        refreshBroadcaster.broadcastRefresh()
    }

    fun isDeleted(task: Long): Boolean = deletionDao.isDeleted(task)
}
