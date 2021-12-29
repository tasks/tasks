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
        completeChildren(item.id, completionDate)
        setComplete(listOf(item), completionDate)
    }

    suspend fun completeChildren(id: Long, completionDate: Long) {
        googleTaskDao
                .getChildTasks(id)
                .plus(taskDao.getChildren(id)
                        .takeIf { it.isNotEmpty() }
                        ?.let { taskDao.fetch(it) }
                        ?: emptyList()
                )
                .filter { it.isCompleted != completionDate > 0 }
                .let { setComplete(it, completionDate)}
    }

    private suspend fun setComplete(tasks: List<Task>, completionDate: Long) {
        tasks.forEachIndexed { i, task ->
            task.completionDate = completionDate
            if (i < tasks.size - 1) {
                task.suppressRefresh()
            }
            taskDao.save(task)
        }
        if (
            tasks.size == 1 &&
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