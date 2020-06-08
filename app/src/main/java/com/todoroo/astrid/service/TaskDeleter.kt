package com.todoroo.astrid.service

import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import kotlinx.collections.immutable.persistentListOf
import org.tasks.LocalBroadcastManager
import org.tasks.data.*
import org.tasks.db.DbUtils.chunkedMap
import org.tasks.db.QueryUtils
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import java.util.*
import javax.inject.Inject

class TaskDeleter @Inject constructor(
        private val deletionDao: DeletionDao,
        private val workManager: WorkManager,
        private val taskDao: TaskDao,
        private val localBroadcastManager: LocalBroadcastManager,
        private val googleTaskDao: GoogleTaskDao,
        private val preferences: Preferences) {

    fun markDeleted(item: Task) = markDeleted(persistentListOf(item.id))

    fun markDeleted(taskIds: List<Long>): List<Task> {
        val ids: MutableSet<Long> = HashSet(taskIds)
        ids.addAll(taskIds.chunkedMap(googleTaskDao::getChildren))
        ids.addAll(taskIds.chunkedMap(taskDao::getChildren))
        deletionDao.markDeleted(ids)
        workManager.cleanup(ids)
        workManager.sync(false)
        localBroadcastManager.broadcastRefresh()
        return ids.chunkedMap(taskDao::fetch)
    }

    fun clearCompleted(filter: Filter): Int {
        val deleteFilter = Filter(null, null)
        deleteFilter.setFilterQueryOverride(
                QueryUtils.removeOrder(QueryUtils.showHiddenAndCompleted(filter.originalSqlQuery)))
        val completed = taskDao.fetchTasks(preferences, deleteFilter)
                .filter(TaskContainer::isCompleted)
                .map(TaskContainer::getId)
        markDeleted(completed)
        return completed.size
    }

    fun delete(task: Task) = delete(task.id)

    fun delete(task: Long) = delete(persistentListOf(task))

    fun delete(tasks: List<Long>) {
        deletionDao.delete(tasks)
        workManager.cleanup(tasks)
        localBroadcastManager.broadcastRefresh()
    }

    fun delete(list: GoogleTaskList) {
        val tasks = deletionDao.delete(list)
        delete(tasks)
        localBroadcastManager.broadcastRefreshList()
    }

    fun delete(list: GoogleTaskAccount) {
        val tasks = deletionDao.delete(list)
        delete(tasks)
        localBroadcastManager.broadcastRefreshList()
    }

    fun delete(list: CaldavCalendar) {
        val tasks = deletionDao.delete(list)
        delete(tasks)
        localBroadcastManager.broadcastRefreshList()
    }

    fun delete(list: CaldavAccount) {
        val tasks = deletionDao.delete(list)
        delete(tasks)
        localBroadcastManager.broadcastRefreshList()
    }
}