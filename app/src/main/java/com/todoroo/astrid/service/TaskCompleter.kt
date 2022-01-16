package com.todoroo.astrid.service

import android.app.NotificationManager
import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.content.Context
import android.media.MediaPlayer
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.data.GoogleTaskDao
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject

class TaskCompleter @Inject internal constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val googleTaskDao: GoogleTaskDao,
    private val preferences: Preferences,
    private val notificationManager: NotificationManager,
) {
    suspend fun setComplete(taskId: Long) =
            taskDao
                    .fetch(taskId)
                    ?.let { setComplete(it, true) }
                    ?: Timber.e("Could not find task $taskId")

    suspend fun setComplete(item: Task, completed: Boolean) {
        val completionDate = if (completed) DateUtilities.now() else 0L
        googleTaskDao
            .getChildTasks(item.id)
            .let {
                if (completed) {
                    it
                } else {
                    it
                        .plus(googleTaskDao.getParentTask(item.id))
                        .plus(taskDao.getParents(item.id).mapNotNull { ids -> taskDao.fetch(ids) })
                }
            }
            .plus(
                taskDao.getChildren(item.id)
                    .takeIf { it.isNotEmpty() }
                    ?.let { taskDao.fetch(it) }
                    ?: emptyList()
            )
            .plus(listOf(item))
            .filterNotNull()
            .filter { it.isCompleted != completionDate > 0 }
            .let { setComplete(it, completionDate) }
    }

    private suspend fun setComplete(tasks: List<Task>, completionDate: Long) {
        taskDao.setCompletionDate(tasks.mapNotNull { it.remoteId }, completionDate)
        tasks.forEachIndexed { i, task ->
            taskDao.saved(task, i < tasks.size - 1)
        }
        if (
            tasks.isNotEmpty() &&
            completionDate > 0 &&
            notificationManager.currentInterruptionFilter == INTERRUPTION_FILTER_ALL
        ) {
            preferences
                .completionSound
                ?.takeUnless { preferences.isCurrentlyQuietHours }
                ?.let { MediaPlayer.create(context, it).start() }
        }
    }
}