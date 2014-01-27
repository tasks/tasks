/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.api.Addon;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.utility.Constants;

import org.tasks.R;

public class TimerPlugin extends BroadcastReceiver {

    static final String IDENTIFIER = "timer"; //$NON-NLS-1$

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        Addon plugin = new Addon(IDENTIFIER, "Timer", "Todoroo",
                "Lets you time how long it takes to complete tasks.");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ADDONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    /**
     * toggles timer and updates elapsed time.
     * @param start if true, start timer. else, stop it
     */
    public static void updateTimer(Context context, Task task, boolean start) {
        // if this call comes from tasklist, then we need to fill in the gaps to handle this correctly
        // this is needed just for stopping a task
        if (!task.containsNonNullValue(Task.TIMER_START)) {
            task = PluginServices.getTaskService().fetchById(task.getId(), Task.ID, Task.TIMER_START, Task.ELAPSED_SECONDS);
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
        PluginServices.getTaskService().save(task);
        new TimerDecorationExposer().updateDecoration(context, task);

        // update notification
        TimerPlugin.updateNotifications(context);
    }

    private static void updateNotifications(Context context) {
        NotificationManager nm = new AndroidNotificationManager(context);

        int count = PluginServices.getTaskService().count(Query.select(Task.ID).
                where(Task.TIMER_START.gt(0)));
        if(count == 0) {
            nm.cancel(Constants.NOTIFICATION_TIMER);
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

            nm.notify(Constants.NOTIFICATION_TIMER, notification);
        }
    }

}
