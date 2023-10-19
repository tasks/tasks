/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao

import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.timers.TimerPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.data.TaskContainer
import org.tasks.data.TaskDao
import org.tasks.date.DateTimeUtils.isAfterNow
import org.tasks.db.SuspendDbUtils.eachChunk
import org.tasks.jobs.WorkManager
import org.tasks.location.GeofenceApi
import org.tasks.notifications.NotificationManager
import org.tasks.preferences.Preferences
import org.tasks.scheduling.RefreshScheduler
import org.tasks.sync.SyncAdapters
import javax.inject.Inject

class TaskDao @Inject constructor(
        private val taskDao: TaskDao,
        private val refreshScheduler: RefreshScheduler,
        private val localBroadcastManager: LocalBroadcastManager,
        private val notificationManager: NotificationManager,
        private val geofenceApi: GeofenceApi,
        private val timerPlugin: TimerPlugin,
        private val syncAdapters: SyncAdapters,
        private val alarmService: AlarmService,
        private val workManager: WorkManager,
) {

    suspend fun fetch(id: Long): Task? = taskDao.fetch(id)

    suspend fun fetch(ids: List<Long>): List<Task> = taskDao.fetch(ids)

    suspend fun fetch(remoteId: String): Task? = taskDao.fetch(remoteId)

    suspend fun getRecurringTasks(remoteIds: List<String>): List<Task> =
            taskDao.getRecurringTasks(remoteIds)

    suspend fun setCompletionDate(remoteId: String, completionDate: Long) =
        setCompletionDate(listOf(remoteId), completionDate)

    suspend fun setCompletionDate(remoteIds: List<String>, completionDate: Long) =
        taskDao.setCompletionDate(remoteIds, completionDate)

    suspend fun getGoogleTasksToPush(account: String): List<Task> =
            taskDao.getGoogleTasksToPush(account)

    suspend fun getCaldavTasksToPush(calendar: String): List<Task> =
            taskDao.getCaldavTasksToPush(calendar)

    suspend fun fetchTasks(preferences: Preferences, filter: Filter): List<TaskContainer> =
            taskDao.fetchTasks(preferences, filter)

    suspend fun touch(id: Long) = touch(listOf(id))

    suspend fun touch(ids: List<Long>) {
        ids.eachChunk { taskDao.touch(ids) }
        syncAdapters.sync()
    }

    suspend fun setOrder(taskId: Long, order: Long?) = taskDao.setOrder(taskId, order)

    suspend fun setParent(parent: Long, tasks: List<Long>) = taskDao.setParent(parent, tasks)

    suspend fun getChildren(ids: List<Long>) = taskDao.getChildren(ids)

    suspend fun getChildren(id: Long): List<Long> = taskDao.getChildren(id)

    suspend fun getParents(parent: Long): List<Long> = taskDao.getParents(parent)

    suspend fun setCollapsed(id: Long, collapsed: Boolean) {
        taskDao.setCollapsed(listOf(id), collapsed)
        syncAdapters.sync()
        localBroadcastManager.broadcastRefresh()
    }

    suspend fun setCollapsed(preferences: Preferences, filter: Filter, collapsed: Boolean) {
        taskDao.setCollapsed(preferences, filter, collapsed)
        syncAdapters.sync()
    }

    // --- save
    // TODO: get rid of this super-hack
    /**
     * Saves the given task to the database.getDatabase(). Task must already exist. Returns true on
     * success.
     */
    suspend fun save(task: Task) = save(task, fetch(task.id))

    suspend fun saved(original: Task) =
        fetch(original.id)?.let {
            afterUpdate(
                it.apply { if (original.isSuppressRefresh()) suppressRefresh() },
                original
            )
        }

    suspend fun save(task: Task, original: Task?) {
        val updated = taskDao.update(task, original)
        afterUpdate(updated, original)
    }

    private suspend fun afterUpdate(task: Task, original: Task?) {
        val completionDateModified = task.completionDate != (original?.completionDate ?: 0)
        val deletionDateModified = task.deletionDate != (original?.deletionDate ?: 0)
        val justCompleted = completionDateModified && task.isCompleted
        val justDeleted = deletionDateModified && task.isDeleted
        if (task.calendarURI?.isNotBlank() == true) {
            workManager.updateCalendar(task)
        }
        coroutineScope {
            launch(Dispatchers.Default) {
                if (justCompleted || justDeleted) {
                    notificationManager.cancel(task.id)
                    if (task.timerStart > 0) {
                        timerPlugin.stopTimer(task)
                    }
                }
                if (task.dueDate != original?.dueDate && task.dueDate.isAfterNow()) {
                    notificationManager.cancel(task.id)
                }
                if (completionDateModified || deletionDateModified) {
                    geofenceApi.update(task.id)
                }
                alarmService.scheduleAlarms(task)
                refreshScheduler.scheduleRefresh(task)
                if (!task.isSuppressRefresh()) {
                    localBroadcastManager.broadcastRefresh()
                }
                syncAdapters.sync(task, original)
            }
        }
    }

    suspend fun createNew(task: Task) = taskDao.createNew(task)

    suspend fun fetchFiltered(queryTemplate: String): List<Task> =
            taskDao.fetchFiltered(queryTemplate)

    internal suspend fun insert(task: Task): Long = taskDao.insert(task)

    internal suspend fun fetchTasks(callback: suspend () -> List<String>): List<TaskContainer> =
            taskDao.fetchTasks(callback)

    internal suspend fun getAll(): List<Task> = taskDao.getAll()

    internal suspend fun getActiveTasks(): List<Task> = taskDao.getActiveTasks()
}