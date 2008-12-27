package com.timsu.astrid.utilities;

import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

import com.timsu.astrid.R;
import com.timsu.astrid.activities.TaskView;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForNotify;

public class Notifications extends BroadcastReceiver {

    private static final int MIN_INTERVAL_SECONDS = 60;
    private static Random random = new Random();

    @Override
    /** Startup intent */
    public void onReceive(Context context, Intent intent) {
        NotificationManager nm = (NotificationManager) context.
            getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = new Notification(
                android.R.drawable.stat_notify_chat, "started up",
                System.currentTimeMillis());

        nm.notify(0, notification);

        TaskController controller = new TaskController(context);
        controller.open();
        List<TaskModelForNotify> tasks = controller.getTasksWithNotifications();

        for(TaskModelForNotify task : tasks)
            scheduleNextNotification(context, task);
    }

    public interface Notifiable {
        public TaskIdentifier getTaskIdentifier();
        public Integer getNotificationIntervalSeconds();
    }

    /** Schedules the next notification for this task */
    public static void scheduleNextNotification(Context context,
            Notifiable task) {
        if(task.getNotificationIntervalSeconds() == null ||
                task.getNotificationIntervalSeconds() == 0 ||
                task.getTaskIdentifier() == null)
            return;

        // TODO if task is hidden, disregard

        // add a fudge factor to mix things up a bit
        int nextSeconds = (int)((random.nextFloat() * 0.2f + 0.8f) *
            task.getNotificationIntervalSeconds()/60); // TODO remove /60
        if(nextSeconds < MIN_INTERVAL_SECONDS)
            nextSeconds = MIN_INTERVAL_SECONDS;
        long when = System.currentTimeMillis() + nextSeconds * 1000;
        scheduleNotification(context, task.getTaskIdentifier(), when);
    }


    /** Clear notifications associated with this application */
    public static void clearAllNotifications(Context context, TaskIdentifier taskId) {
        NotificationManager nm = (NotificationManager)
            context.getSystemService(Activity.NOTIFICATION_SERVICE);
        nm.cancel((int)taskId.getId());
    }

    /** Schedule a new notification about the given task */
    public static void scheduleNotification(Context context,
            TaskIdentifier taskId, long when) {

        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Resources r = context.getResources();

        Intent notifyIntent = new Intent(context, TaskView.class);
        notifyIntent.putExtra(TaskView.LOAD_INSTANCE_TOKEN,
                taskId.getId());
        notifyIntent.putExtra(TaskView.FROM_NOTIFICATION_TOKEN, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, notifyIntent, PendingIntent.FLAG_ONE_SHOT);

        // notification text
        String appName = r.getString(R.string.app_name);
        String[] reminders = r.getStringArray(R.array.reminders);
        int next = random.nextInt(reminders.length);
        String reminder = reminders[next];

        Notification notification = new Notification(
                android.R.drawable.stat_notify_chat, reminder, when);

        notification.setLatestEventInfo(context,
                appName,
                reminder,
                pendingIntent);

        notification.defaults = Notification.DEFAULT_ALL;
        notification.vibrate = new long[] { 300, 50, 50, 300, 100, 300, 100,
            100, 200 };

        Log.w("Notifications", "Logging notification: " + reminder + " for " +
                (when - System.currentTimeMillis())/1000 + " seconds from now");
        nm.notify((int)taskId.getId(), notification);
    }

}