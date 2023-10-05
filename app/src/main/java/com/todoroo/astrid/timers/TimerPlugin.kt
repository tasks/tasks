/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.todoroo.andlib.sql.Criterion.Companion.and
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.FilterImpl
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.utility.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.data.TaskDao
import org.tasks.intents.TaskIntents
import org.tasks.notifications.NotificationManager
import org.tasks.time.DateTimeUtils
import javax.inject.Inject

class TimerPlugin @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val notificationManager: NotificationManager,
        private val taskDao: TaskDao) {
    suspend fun startTimer(task: Task?) {
        updateTimer(task, true)
    }

    suspend fun stopTimer(task: Task?) {
        updateTimer(task, false)
    }

    /**
     * toggles timer and updates elapsed time.
     *
     * @param start if true, start timer. else, stop it
     */
    private suspend fun updateTimer(task: Task?, start: Boolean) {
        if (task == null) {
            return
        }
        if (start) {
            if (task.timerStart == 0L) {
                task.timerStart = DateUtilities.now()
            }
        } else {
            if (task.timerStart > 0) {
                val newElapsed = ((DateUtilities.now() - task.timerStart) / 1000L).toInt()
                task.timerStart = 0L
                task.elapsedSeconds = task.elapsedSeconds + newElapsed
            }
        }
        taskDao.update(task)
        updateNotifications()
    }

    suspend fun updateNotifications() {
        val count = taskDao.activeTimers()
        if (count == 0) {
            notificationManager.cancel(Constants.NOTIFICATION_TIMER.toLong())
        } else {
            val filter = createFilter(context)
            val notifyIntent = TaskIntents.getTaskListIntent(context, filter)
            val pendingIntent = PendingIntent.getActivity(
                context,
                Constants.NOTIFICATION_TIMER,
                notifyIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val r = context.resources
            val appName = r.getString(R.string.app_name)
            val text = r.getString(
                    R.string.TPl_notification, r.getQuantityString(R.plurals.Ntasks, count, count))
            val builder = NotificationCompat.Builder(context, NotificationManager.NOTIFICATION_CHANNEL_TIMERS)
                    .setContentIntent(pendingIntent)
                    .setContentTitle(appName)
                    .setContentText(text)
                    .setWhen(DateTimeUtils.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_timer_white_24dp)
                    .setAutoCancel(false)
                    .setOngoing(true)
            notificationManager.notify(
                    Constants.NOTIFICATION_TIMER.toLong(),
                    builder,
                    alert = false,
                    nonstop = false,
                    fiveTimes = false)
        }
    }

    companion object {
        fun createFilter(context: Context): Filter {
            return FilterImpl(
                title = context.getString(R.string.TFE_workingOn),
                sql = QueryTemplate()
                    .where(
                        and(
                            Task.TIMER_START.gt(0),
                            Task.DELETION_DATE.eq(0)
                        )
                    ).toString(),
                icon = R.drawable.ic_outline_timer_24px,
            )
        }
    }
}