package com.todoroo.astrid.timers;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.api.Addon;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.utility.Constants;

public class TimerPlugin extends BroadcastReceiver {

    static final String IDENTIFIER = "timer"; //$NON-NLS-1$

    @Override
    @SuppressWarnings("nls")
    public void onReceive(Context context, Intent intent) {
        Addon plugin = new Addon(IDENTIFIER, "Timer", "Todoroo",
                "Lets you time how long it takes to complete tasks.");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ADDONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    /**
     * stops timer and sets elapsed time. you still need to save the task.
     * @param task
     * @param start if true, start timer. else, stop it
     */
    public static void updateTimer(Context context, Task task, boolean start) {
        if(start) {
            if(task.getValue(Task.TIMER_START) == 0)
                task.setValue(Task.TIMER_START, DateUtilities.now());
        } else {
            if(task.getValue(Task.TIMER_START) > 0) {
                int newElapsed = (int)((DateUtilities.now() - task.getValue(Task.TIMER_START)) / 1000L);
                task.setValue(Task.TIMER_START, 0L);
                task.setValue(Task.ELAPSED_SECONDS,
                        task.getValue(Task.ELAPSED_SECONDS) + newElapsed);
            }
        }
        PluginServices.getTaskService().save(task, true);
        TimerDecorationExposer.removeFromCache(task.getId());

        // transmit new intents
        Intent intent = new Intent(AstridApiConstants.BROADCAST_REQUEST_ACTIONS);
        intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
        new TimerDecorationExposer().onReceive(context, intent);
        new TimerActionExposer().onReceive(context, intent);

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
                    0, notifyIntent, 0);

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
