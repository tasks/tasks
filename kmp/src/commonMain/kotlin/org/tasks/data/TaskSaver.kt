package org.tasks.data

import co.touchlab.kermit.Logger
import com.todoroo.astrid.timers.TimerPlugin
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.TaskDao
import org.tasks.data.db.SuspendDbUtils.eachChunk
import org.tasks.data.entity.Task
import org.tasks.filters.Filter
import org.tasks.preferences.QueryPreferences
import org.tasks.jobs.BackgroundWork
import org.tasks.location.LocationService
import org.tasks.notifications.Notifier
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import org.tasks.time.DateTimeUtils2.currentTimeMillis

class TaskSaver(
    private val taskDao: TaskDao,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val notifier: Notifier,
    private val locationService: LocationService,
    private val timerPlugin: TimerPlugin,
    private val syncAdapters: SyncAdapters,
    private val backgroundWork: BackgroundWork,
) {
    suspend fun save(task: Task, original: Task? = null) {
        if (taskDao.update(task, original)) {
            Logger.d("TaskSaver") { "Saved $task" }
            afterSave(task, original)
        }
    }

    suspend fun afterSave(task: Task, original: Task?) {
        val completionDateModified = task.completionDate != (original?.completionDate ?: 0)
        val deletionDateModified = task.deletionDate != (original?.deletionDate ?: 0)
        val justCompleted = completionDateModified && task.isCompleted
        if (task.calendarURI?.isNotBlank() == true) {
            backgroundWork.updateCalendar(task)
        }
        if (justCompleted) {
            if (task.timerStart > 0) {
                timerPlugin.stopTimer(task)
            }
        }
        if (task.dueDate != original?.dueDate && task.dueDate > currentTimeMillis()) {
            notifier.cancel(task.id)
        }
        if (completionDateModified || deletionDateModified) {
            locationService.updateGeofences(task.id)
        }
        syncAdapters.sync(task, original)
        if (!task.isSuppressRefresh()) {
            refreshBroadcaster.broadcastRefresh()
        }
        notifier.triggerNotifications()
        backgroundWork.scheduleRefresh()
    }

    suspend fun touch(ids: List<Long>) {
        ids.eachChunk { taskDao.touch(it) }
        syncAdapters.sync(SyncSource.TASK_CHANGE)
    }

    suspend fun setCollapsed(id: Long, collapsed: Boolean) {
        taskDao.setCollapsed(listOf(id), collapsed)
        syncAdapters.sync(SyncSource.TASK_CHANGE)
        refreshBroadcaster.broadcastRefresh()
    }

    suspend fun setCollapsed(preferences: QueryPreferences, filter: Filter, collapsed: Boolean) {
        taskDao.fetchTasks(TaskListQuery.getQuery(preferences, filter))
            .filter(TaskContainer::hasChildren)
            .map(TaskContainer::id)
            .eachChunk { taskDao.setCollapsed(it, collapsed) }
        syncAdapters.sync(SyncSource.TASK_CHANGE)
        refreshBroadcaster.broadcastRefresh()
    }
}
