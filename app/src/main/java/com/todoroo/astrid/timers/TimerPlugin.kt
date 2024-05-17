/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers

import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.notifications.NotificationManager
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import javax.inject.Inject

class TimerPlugin @Inject constructor(
        private val notificationManager: NotificationManager,
        private val taskDao: TaskDao
) {
    suspend fun startTimer(task: Task) {
        updateTimer(task, true)
    }

    suspend fun stopTimer(task: Task) {
        updateTimer(task, false)
    }

    /**
     * toggles timer and updates elapsed time.
     *
     * @param start if true, start timer. else, stop it
     */
    private suspend fun updateTimer(task: Task, start: Boolean) {
        if (start) {
            if (task.timerStart == 0L) {
                task.timerStart = currentTimeMillis()
            }
        } else {
            if (task.timerStart > 0) {
                val newElapsed = ((currentTimeMillis() - task.timerStart) / 1000L).toInt()
                task.timerStart = 0L
                task.elapsedSeconds += newElapsed
            }
        }
        taskDao.update(task)
        notificationManager.updateTimerNotification()
    }
}