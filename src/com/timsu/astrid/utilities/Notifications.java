package com.timsu.astrid.utilities;

import java.util.Date;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.AlarmManager;
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

    private static final String ID_KEY = "id";
    private static final int MIN_INTERVAL_SECONDS = 120;
    private static Random random = new Random();

    /** Something we can create a notification for */
    public interface Notifiable {
        public TaskIdentifier getTaskIdentifier();
        public Integer getNotificationIntervalSeconds();
        public Date getHiddenUntil();
    }

    @Override
    /** Startup intent */
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(ID_KEY, 0);
        Log.e("ALARM", "Alarm triggered id " + id);
        showNotification(context, id);
    }

    // --- alarm manager stuff

    public static void scheduleAllAlarms(Context context) {
        TaskController controller = new TaskController(context);
        controller.open();
        List<TaskModelForNotify> tasks = controller.getTasksWithNotifications();

        for(TaskModelForNotify task : tasks)
            scheduleNextAlarm(context, task);
    }

    /** Schedules the next notification for this task */
    public static void scheduleNextAlarm(Context context,
            Notifiable task) {
        if(task.getNotificationIntervalSeconds() == null ||
                task.getNotificationIntervalSeconds() == 0 ||
                task.getTaskIdentifier() == null)
            return;

        if(task.getHiddenUntil() != null && task.getHiddenUntil().after(new Date()))
            return;

        // compute, and add a fudge factor to mix things up a bit
        int interval = task.getNotificationIntervalSeconds();
        int currentSeconds = (int)(System.currentTimeMillis() / 1000);
        int untilNextInterval = interval - currentSeconds % interval;
        untilNextInterval *= 0.2f + random.nextFloat() * 0.6f;
        if(untilNextInterval < MIN_INTERVAL_SECONDS)
            untilNextInterval = MIN_INTERVAL_SECONDS;
        long when = System.currentTimeMillis() + untilNextInterval * 1000;
        scheduleAlarm(context, task.getTaskIdentifier().getId(), when);
    }

    /** Delete the given alarm */
    public static void deleteAlarm(Context context, long id) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, Notifications.class);
        intent.putExtra(ID_KEY, id);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

        am.cancel(sender);
    }

    /** Schedules a single alarm */
    public static void scheduleAlarm(Context context, long id, long when) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, Notifications.class);
        intent.putExtra(ID_KEY, id);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

        Log.e("ALARM", "Alarm set for " + new Date(when));
        am.set(AlarmManager.RTC, when, sender);
    }

    // --- notification manager stuff

    /** Clear notifications associated with this application */
    public static void clearAllNotifications(Context context, TaskIdentifier taskId) {
        NotificationManager nm = (NotificationManager)
            context.getSystemService(Activity.NOTIFICATION_SERVICE);
        nm.cancel((int)taskId.getId());
    }

    /** Schedule a new notification about the given task */
    public static void showNotification(Context context, long id) {

        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Resources r = context.getResources();

        Intent notifyIntent = new Intent(context, TaskView.class);
        notifyIntent.putExtra(TaskView.LOAD_INSTANCE_TOKEN, id);
        notifyIntent.putExtra(TaskView.FROM_NOTIFICATION_TOKEN, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, notifyIntent, PendingIntent.FLAG_ONE_SHOT);

        // notification text
        String appName = r.getString(R.string.app_name);
        String[] reminders = r.getStringArray(R.array.reminders);
        int next = random.nextInt(reminders.length);
        String reminder = reminders[next];

        Notification notification = new Notification(
                android.R.drawable.stat_notify_chat, reminder,
                System.currentTimeMillis());

        notification.setLatestEventInfo(context,
                appName,
                reminder,
                pendingIntent);

        notification.defaults = Notification.DEFAULT_ALL;
        notification.vibrate = new long[] { 300, 50, 50, 300, 100, 300, 100,
            100, 200 };

        Log.w("Notifications", "Logging notification: " + reminder);
        nm.notify((int)id, notification);
    }

}