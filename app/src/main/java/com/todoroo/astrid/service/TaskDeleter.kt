package com.todoroo.astrid.service

import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.data.Task
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.runBlocking
import org.tasks.LocalBroadcastManager
import org.tasks.data.*
import org.tasks.db.QueryUtils
import org.tasks.db.SuspendDbUtils.chunkedMap
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import org.tasks.sync.SyncAdapters
import java.util.*
import javax.inject.Inject

class TaskDeleter @Inject constructor(
        private val deletionDao: DeletionDao,
        private val workManager: WorkManager,
        private val taskDao: TaskDao,
        private val localBroadcastManager: LocalBroadcastManager,
        private val googleTaskDao: GoogleTaskDao,
        private val preferences: Preferences,
        private val syncAdapters: SyncAdapters) {

    suspend fun markDeleted(item: Task) = markDeleted(persistentListOf(item.id))

    suspend fun markDeleted(taskIds: List<Long>): List<Task> {
        val ids: MutableSet<Long> = HashSet(taskIds)
        ids.addAll(taskIds.chunkedMap(googleTaskDao::getChildren))
        ids.addAll(taskIds.chunkedMap(taskDao::getChildren))
        deletionDao.markDeleted(ids)
        workManager.cleanup(ids)
        syncAdapters.sync()
        localBroadcastManager.broadcastRefresh()
        return ids.chunkedMap(taskDao::fetch)
    }

    suspend fun clearCompleted(filter: Filter): Int {
        val deleteFilter = Filter(null, null)
        deleteFilter.setFilterQueryOverride(
                QueryUtils.removeOrder(QueryUtils.showHiddenAndCompleted(filter.originalSqlQuery)))
        val completed = taskDao.fetchTasks(preferences, deleteFilter)
                .filter(TaskContainer::isCompleted)
                .map(TaskContainer::getId)
                .toMutableList()
        completed.removeAll(deletionDao.hasRecurringAncestors(completed))
        completed.removeAll(googleTaskDao.hasRecurringParent(completed))
        markDeleted(completed)
        return completed.size
    }

    suspend fun delete(task: Task) = delete(task.id)

    suspend fun delete(task: Long) = delete(persistentListOf(task))

    suspend fun delete(tasks: List<Long>) {
        deletionDao.delete(tasks)
        workManager.cleanup(tasks)
        localBroadcastManager.broadcastRefresh()
    }

    fun delete(list: GoogleTaskList) = runBlocking {
        val tasks = deletionDao.delete(list)
        delete(tasks)
        localBroadcastManager.broadcastRefreshList()
    }

    suspend fun delete(list: GoogleTaskAccount) {
        val tasks = deletionDao.delete(list)
        delete(tasks)
        localBroadcastManager.broadcastRefreshList()
    }

    suspend fun delete(list: CaldavCalendar) {
        val tasks = deletionDao.delete(list)
        delete(tasks)
        localBroadcastManager.broadcastRefreshList()
    }

    suspend fun delete(list: CaldavAccount) {
        val tasks = deletionDao.delete(list)
        delete(tasks)
        localBroadcastManager.broadcastRefreshList()
    }
}