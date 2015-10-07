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
import android.support.v7.app.NotificationCompat;

import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Constants;

import org.tasks.R;
import org.tasks.notifications.NotificationManager;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;

public class TimerPlugin {

    /**
     * toggles timer and updates elapsed time.
     * @param start if true, start timer. else, stop it
     */
    public static void updateTimer(NotificationManager notificationManager, TaskService taskService, Context context, Task task, boolean start) {
        // if this call comes from tasklist, then we need to fill in the gaps to handle this correctly
        // this is needed just for stopping a task
        if (!task.containsNonNullValue(Task.TIMER_START)) {
            task = taskService.fetchById(task.getId(), Task.ID, Task.TIMER_START, Task.ELAPSED_SECONDS);
        }
        if (task == null) {
            return;
        }

        if(start) {
            if(task.getTimerStart() == 0) {
                task.setTimerStart(DateUtilities.now());
            }
        } else {
            if(task.getTimerStart() > 0) {
                int newElapsed = (int)((DateUtilities.now() - task.getTimerStart()) / 1000L);
                task.setTimerStart(0L);
                task.setELAPSED_SECONDS(
                        task.getElapsedSeconds() + newElapsed);
            }
        }
        taskService.save(task);

        // update notification
        TimerPlugin.updateNotifications(notificationManager, taskService, context);
    }

    private static void updateNotifications(NotificationManager notificationManager, TaskService taskService, Context context) {
        int count = taskService.count(Query.select(Task.ID).
                where(Task.TIMER_START.gt(0)));
        if(count == 0) {
            notificationManager.cancel(Constants.NOTIFICATION_TIMER);
        } else {
            Filter filter = TimerFilterExposer.createFilter(context);
            Intent notifyIntent = ShortcutActivity.createIntent(context, filter);
            notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(context,
                    Constants.NOTIFICATION_TIMER, notifyIntent, 0);

            Resources r = context.getResources();
            String appName = r.getString(R.string.app_name);
            String text = r.getString(R.string.TPl_notification,
                    r.getQuantityString(R.plurals.Ntasks, count, count));
            Notification notification = new NotificationCompat.Builder(context)
                    .setContentIntent(pendingIntent)
                    .setContentTitle(appName)
                    .setContentText(text)
                    .setWhen(currentTimeMillis())
                    .setSmallIcon(R.drawable.timers_notification)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .build();
            notificationManager.notify(Constants.NOTIFICATION_TIMER, notification);
        }
    }
}
