package com.todoroo.astrid.service

import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT
import android.media.RingtoneManager
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.repeats.RepeatTaskHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.LocalBroadcastManager
import org.tasks.data.dao.CaldavDao
import org.tasks.data.db.Database
import org.tasks.data.entity.Task
import org.tasks.data.withTransaction
import org.tasks.jobs.WorkManager
import org.tasks.notifications.NotificationManager
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import javax.inject.Inject

class TaskCompleter @Inject internal constructor(
    @ApplicationContext private val context: Context,
    private val database: Database,
    private val taskDao: TaskDao,
    private val preferences: Preferences,
    private val notificationManager: NotificationManager,
    private val localBroadcastManager: LocalBroadcastManager,
    private val repeatTaskHelper: RepeatTaskHelper,
    private val caldavDao: CaldavDao,
    private val gCalHelper: GCalHelper,
    private val workManager: WorkManager,
) {
    suspend fun setComplete(taskId: Long) =
            taskDao
                    .fetch(taskId)
                    ?.let { setComplete(it, true) }
                    ?: Timber.e("Could not find task $taskId")

    suspend fun setComplete(item: Task, completed: Boolean, includeChildren: Boolean = true) {
        val completionDate = if (completed) currentTimeMillis() else 0L
        ArrayList<Task?>()
            .apply {
                if (includeChildren) {
                    addAll(taskDao.getChildren(item.id).let { taskDao.fetch(it) })
                }
                if (!completed) {
                    addAll(taskDao.getParents(item.id).let { taskDao.fetch(it) })
                }
                add(item)
            }
            .filterNotNull()
            .filter { it.isCompleted != completionDate > 0 }
            .filterNot { it.readOnly }
            .let {
                setComplete(it, completionDate)
                if (completed && !item.isRecurring) {
                    localBroadcastManager.broadcastTaskCompleted(ArrayList(it.map(Task::id)))
                }
            }
    }

    suspend fun setComplete(tasks: List<Task>, completionDate: Long) {
        if (tasks.isEmpty()) {
            return
        }
        tasks.forEach { notificationManager.cancel(it.id) }
        val completed = completionDate > 0
        val modified = currentTimeMillis()
        database.withTransaction {
            tasks
                .map {
                    it.copy(
                        completionDate = completionDate,
                        modificationDate = modified,
                    )
                }
                .also { completed ->
                    completed.subList(0, completed.lastIndex).forEach { it.suppressRefresh() }
                    taskDao.save(completed, tasks)
                }
                .forEach { task ->
                    if (completed && task.isRecurring) {
                        gCalHelper.updateEvent(task)

                        if (caldavDao.getAccountForTask(task.id)?.isSuppressRepeatingTasks != true) {
                            repeatTaskHelper.handleRepeat(task)
                            if (task.completionDate == 0L) {
                                // un-complete children
                                setComplete(task, false)
                            }
                        }
                    }
                }
        }
        workManager.triggerNotifications()
        workManager.scheduleRefresh()
        if (completed && notificationManager.currentInterruptionFilter == INTERRUPTION_FILTER_ALL) {
            preferences
                .completionSound
                ?.takeUnless { preferences.isCurrentlyQuietHours }
                ?.let {
                    RingtoneManager
                        .getRingtone(context, it)
                        .apply {
                            audioAttributes = AudioAttributes.Builder()
                                .setUsage(USAGE_NOTIFICATION_EVENT)
                                .build()
                        }
                        .play()
                }
        }
    }
}