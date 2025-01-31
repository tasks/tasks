package com.todoroo.astrid.service

import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT
import android.media.RingtoneManager
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.repeats.RepeatTaskHelper
import com.todoroo.astrid.repeats.RepeatTaskHelper.Companion.computePreviousDueDate
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.LocalBroadcastManager
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.CompletionDao
import org.tasks.data.entity.Task
import org.tasks.jobs.WorkManager
import org.tasks.notifications.NotificationManager
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import javax.inject.Inject

class TaskCompleter @Inject internal constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val preferences: Preferences,
    private val notificationManager: NotificationManager,
    private val localBroadcastManager: LocalBroadcastManager,
    private val repeatTaskHelper: RepeatTaskHelper,
    private val caldavDao: CaldavDao,
    private val gCalHelper: GCalHelper,
    private val workManager: WorkManager,
    private val completionDao: CompletionDao,
) {
    suspend fun setComplete(taskId: Long, completed: Boolean = true) =
            taskDao
                    .fetch(taskId)
                    ?.let { setComplete(it, completed) }
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
            .let { tasks ->
                setComplete(tasks, completionDate)
                if (completed && !item.isRecurring) {
                    localBroadcastManager.broadcastTaskCompleted(tasks.map { it.id })
                }
            }
    }

    suspend fun setComplete(tasks: List<Task>, completionDate: Long) {
        if (tasks.isEmpty()) {
            return
        }
        tasks.forEach { notificationManager.cancel(it.id) }
        val completed = completionDate > 0
        val repeated = ArrayList<Task>()
        Timber.d("Completing $tasks")
        completionDao.complete(
            tasks = tasks,
            completionDate = completionDate,
            afterSave = { updated ->
                updated.forEach { saved ->
                    val original = tasks.find { it.id == saved.id }
                    taskDao.afterUpdate(saved, original)
                }
                updated.forEach { task ->
                    if (completed && task.isRecurring) {
                        gCalHelper.updateEvent(task)

                        if (caldavDao.getAccountForTask(task.id)?.isSuppressRepeatingTasks != true) {
                            if (repeatTaskHelper.handleRepeat(task)) {
                                repeated.add(task)
                            }
                            if (task.completionDate == 0L) {
                                // un-complete children
                                setComplete(task, false)
                            }
                        }
                    }
                }
            }
        )
        localBroadcastManager.broadcastRefresh()
        workManager.triggerNotifications()
        workManager.scheduleRefresh()
        repeated.lastOrNull()?.let { task ->
            val oldDueDate = tasks.find { it.id == task.id }?.dueDate?.takeIf { it > 0 }
                ?: computePreviousDueDate(task)
            localBroadcastManager.broadcastTaskCompleted(arrayListOf(task.id), oldDueDate)
        }
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