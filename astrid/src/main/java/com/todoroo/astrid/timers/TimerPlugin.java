/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskTimeLog;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Constants;

import org.tasks.R;
import org.tasks.notifications.NotificationManager;
import org.tasks.timelog.TimeLogService;

public class TimerPlugin {

    /**
     * toggles timer and updates elapsed time.
     *  @param timeLogService
     * @param start if true, start timer. else, stop it
     */
    public static TaskTimeLog updateTimer(NotificationManager notificationManager, TaskService taskService, TimeLogService timeLogService, Context context, Task task, boolean start) {
        // if this call comes from tasklist, then we need to fill in the gaps to handle this correctly
        // this is needed just for stopping a task
        TaskTimeLog timeLog = null;
        if (!task.containsNonNullValue(Task.TIMER_START)) {
            task = taskService.fetchById(task.getId(), Task.ID, Task.TIMER_START, Task.ELAPSED_SECONDS);
        }
        if (task == null) {
            return timeLog;
        }

        if (start) {
            if (task.getTimerStart() == 0) {
                task.setTimerStart(DateUtilities.now());
            }
        } else {
            if (task.getTimerStart() > 0) {
                int newElapsed = (int) ((DateUtilities.now() - task.getTimerStart()) / 1000L);
                task.setTimerStart(0L);
                timeLog = createTimeLog(task, newElapsed);
                timeLogService.addTimeLog(timeLog);
                task.lowerRemainingSeconds(newElapsed);
            }
        }
        taskService.save(task);

        // update notification
        TimerPlugin.updateNotifications(notificationManager, taskService, context);
        return timeLog;
    }

    public static TaskTimeLog createTimeLog(Task task, int newElapsed) {
        TaskTimeLog timeLog = new TaskTimeLog();
        timeLog.setTime(DateUtilities.now());
        timeLog.setTimeSpent(newElapsed);
        timeLog.setTaskId(task.getId());
        return timeLog;
    }

    private static void updateNotifications(NotificationManager notificationManager, TaskService taskService, Context context) {
        int count = taskService.count(Query.select(Task.ID).
                where(Task.TIMER_START.gt(0)));
        if (count == 0) {
            notificationManager.cancel(Constants.NOTIFICATION_TIMER);
        } else {
            Filter filter = TimerFilterExposer.createFilter(context);
            Intent notifyIntent = ShortcutActivity.createIntent(filter);
            notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(context,
                    Constants.NOTIFICATION_TIMER, notifyIntent, 0);

            Resources r = context.getResources();
            String appName = r.getString(R.string.app_name);
            String text = r.getString(R.string.TPl_notification,
                    r.getQuantityString(R.plurals.Ntasks, count, count));
            Notification notification = new Notification(
                    R.drawable.timers_notification, text, System.currentTimeMillis());
            notification.setLatestEventInfo(context, appName,
                    text, pendingIntent);
            notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
            notification.flags &= ~Notification.FLAG_AUTO_CANCEL;

            notificationManager.notify(Constants.NOTIFICATION_TIMER, notification);
        }
    }

}
