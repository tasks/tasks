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
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.utility.Constants;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.injection.ForApplication;
import org.tasks.intents.TaskIntents;
import org.tasks.notifications.NotificationManager;

import javax.inject.Inject;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;

public class TimerPlugin {

    private final Context context;
    private final NotificationManager notificationManager;
    private final Tracker tracker;
    private final TaskDao taskDao;

    @Inject
    public TimerPlugin(@ForApplication Context context, NotificationManager notificationManager,
                       Tracker tracker, TaskDao taskDao) {
        this.context = context;
        this.notificationManager = notificationManager;
        this.tracker = tracker;
        this.taskDao = taskDao;
    }

    public void startTimer(Task task) {
        tracker.reportEvent(Tracking.Events.TIMER_START);
        updateTimer(task, true);
    }

    public void stopTimer(Task task) {
        updateTimer(task, false);
    }

    /**
     * toggles timer and updates elapsed time.
     * @param start if true, start timer. else, stop it
     */
    private void updateTimer(Task task, boolean start) {
        if (task == null) {
            return;
        }

        if(start) {
            if(task.getTimerStart() == 0) {
                task.setTimerStart(DateUtilities.now());
            }
        } else {
            // if this call comes from tasklist, then we need to fill in the gaps to handle this correctly
            // this is needed just for stopping a task
            if (!task.containsNonNullValue(Task.TIMER_START)) {
                task = taskDao.fetch(task.getId(), Task.ID, Task.TIMER_START, Task.ELAPSED_SECONDS);
            }
            if (task == null) {
                return;
            }

            if(task.getTimerStart() > 0) {
                int newElapsed = (int)((DateUtilities.now() - task.getTimerStart()) / 1000L);
                task.setTimerStart(0L);
                task.setElapsedSeconds(task.getElapsedSeconds() + newElapsed);
            }
        }
        taskDao.save(task);

        // update notification
        updateNotifications();
    }

    private void updateNotifications() {
        int count = taskDao.count(Query.select(Task.ID).
                where(Task.TIMER_START.gt(0)));
        if(count == 0) {
            notificationManager.cancel(Constants.NOTIFICATION_TIMER);
        } else {
            Filter filter = TimerFilterExposer.createFilter(context);
            Intent notifyIntent = TaskIntents.getTaskListIntent(context, filter);
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
                    .setSmallIcon(R.drawable.ic_timer_white_24dp)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .build();
            notificationManager.notify(Constants.NOTIFICATION_TIMER, notification);
        }
    }
}
