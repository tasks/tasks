package com.todoroo.astrid.service

import android.app.NotificationManager
import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT
import android.media.RingtoneManager
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.LocalBroadcastManager
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject

class TaskCompleter @Inject internal constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val preferences: Preferences,
    private val notificationManager: NotificationManager,
    private val localBroadcastManager: LocalBroadcastManager,
    private val workManager: WorkManager,
) {
    suspend fun setComplete(taskId: Long) =
            taskDao
                    .fetch(taskId)
                    ?.let { setComplete(it, true) }
                    ?: Timber.e("Could not find task $taskId")

    suspend fun setComplete(item: Task, completed: Boolean, includeChildren: Boolean = true) {
        val completionDate = if (completed) DateUtilities.now() else 0L
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
        val completed = completionDate > 0
        taskDao.setCompletionDate(tasks.mapNotNull { it.remoteId }, completionDate)
        tasks.forEachIndexed { i, original ->
            if (i < tasks.size - 1) {
                original.suppressRefresh()
            }
            taskDao.saved(original)
        }
        tasks.forEach {
            if (completed && it.isRecurring) {
                workManager.scheduleRepeat(it)
            }
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