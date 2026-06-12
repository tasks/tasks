package org.tasks.data

import co.touchlab.kermit.Logger
import com.todoroo.astrid.timers.TimerPlugin
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.db.SuspendDbUtils.eachChunk
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.SUPPRESS_SYNC
import org.tasks.data.entity.SyncTrait
import org.tasks.data.entity.SYNC_ALARMS
import org.tasks.data.entity.SYNC_LOCATION
import org.tasks.data.entity.SYNC_TAGS
import org.tasks.data.entity.Task
import org.tasks.filters.Filter
import org.tasks.preferences.QueryPreferences
import org.tasks.jobs.BackgroundWork
import org.tasks.location.LocationService
import org.tasks.notifications.CancelReason
import org.tasks.notifications.Notifier
import org.tasks.time.DateTimeUtils2.currentTimeMillis

class TaskSaver(
    private val taskDao: TaskDao,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val notifier: Notifier,
    private val locationService: LocationService,
    private val timerPlugin: TimerPlugin,
    private val backgroundWork: BackgroundWork,
    private val caldavDao: CaldavDao,
) {
    suspend fun save(task: Task, original: Task?, dirty: Boolean = true) {
        val markDirty = dirty && needsSync(task, original)
        if (taskDao.update(task, original, updateTimestamp = dirty, markDirty)) {
            Logger.d("TaskSaver") { "Saved $task" }
            afterSave(task, original)
        }
    }

    private suspend fun needsSync(task: Task, original: Task?): Boolean {
        if (task.checkTransitory(SUPPRESS_SYNC)) {
            return false
        }
        val accountType = caldavDao.getAccountType(task.id) ?: return false
        val traits = CaldavAccount.syncTraits(accountType)
        val transitoryChanged =
            (SyncTrait.TAGS in traits && task.checkTransitory(SYNC_TAGS)) ||
                    (SyncTrait.ALARMS in traits && task.checkTransitory(SYNC_ALARMS)) ||
                    (SyncTrait.LOCATION in traits && task.checkTransitory(SYNC_LOCATION))
        if (transitoryChanged) {
            return true
        }
        return when (accountType) {
            TYPE_LOCAL -> false
            TYPE_GOOGLE_TASKS -> !task.googleTaskUpToDate(original)
            TYPE_MICROSOFT -> !task.microsoftUpToDate(original)
            else -> !task.caldavUpToDate(original)
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
            notifier.cancel(task.id, CancelReason.DUE_DATE_CHANGE)
        }
        if (completionDateModified || deletionDateModified) {
            locationService.updateGeofences(task.id)
        }
        if (!task.isSuppressRefresh()) {
            refreshBroadcaster.broadcastRefresh()
        }
        notifier.triggerNotifications()
        backgroundWork.scheduleRefresh()
    }

    suspend fun setCollapsed(id: Long, collapsed: Boolean) {
        taskDao.setCollapsed(listOf(id), collapsed)
        refreshBroadcaster.broadcastRefresh()
    }

    suspend fun setCollapsed(preferences: QueryPreferences, filter: Filter, collapsed: Boolean) {
        taskDao.fetchTasks(TaskListQuery.getQuery(preferences, filter))
            .filter(TaskContainer::hasChildren)
            .map(TaskContainer::id)
            .eachChunk { taskDao.setCollapsed(it, collapsed) }
        refreshBroadcaster.broadcastRefresh()
    }
}
