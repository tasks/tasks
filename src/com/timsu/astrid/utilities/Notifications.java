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
import com.timsu.astrid.data.task.TaskModelForList;
import com.timsu.astrid.data.task.TaskModelForNotify;

public class Notifications extends BroadcastReceiver {

    private static final String ID_KEY               = "id";
    private static final int    MIN_INTERVAL_SECONDS = 300;

    private static final float  FUDGE_MIN            = 0.2f;
    private static final float  FUDGE_MAX            = 0.8f;

    private static Random       random               = new Random();

    /** Something we can create a notification for */
    public interface Notifiable {
        public TaskIdentifier getTaskIdentifier();
        public Integer getNotificationIntervalSeconds();
        public boolean isTaskCompleted();
        public Date getHiddenUntil();
    }

    @Override
    /** Alarm intent */
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(ID_KEY, 0);
        Log.e("ALARM", "Alarm triggered id " + id);
        if(!showNotification(context, id)) {
            deleteAlarm(context, id);
            NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel((int)id);
        }
    }

    // --- alarm manager stuff

    private static boolean isAlarmEnabled(Notifiable task) {
        if(task.getNotificationIntervalSeconds() == null ||
                task.getNotificationIntervalSeconds() == 0)
            return false;

        if(task.isTaskCompleted())
            return false;

        return true;
    }

    public static void scheduleAllAlarms(Context context) {
        TaskController controller = new TaskController(context);
        controller.open();
        List<TaskModelForNotify> tasks = controller.getTasksWithNotifications();

        for(TaskModelForNotify task : tasks)
            updateAlarm(context, task);

        controller.close();
    }

    /** Schedules the next notification for this task */
    public static void updateAlarm(Context context, Notifiable task) {
        if(task.getTaskIdentifier() == null)
            return;

        if(!isAlarmEnabled(task)) {
            deleteAlarm(context, task.getTaskIdentifier().getId());
            return;
        }

        // compute, and add a fudge factor to mix things up a bit
        int interval = task.getNotificationIntervalSeconds();
        int currentSeconds = (int)(System.currentTimeMillis() / 1000);
        int untilNextInterval = interval - currentSeconds % interval;
        untilNextInterval *= FUDGE_MIN + random.nextFloat() * (FUDGE_MAX - FUDGE_MIN);
        if(untilNextInterval < MIN_INTERVAL_SECONDS)
            untilNextInterval = MIN_INTERVAL_SECONDS;
        long when = System.currentTimeMillis() + untilNextInterval * 1000;
        scheduleAlarm(context, task.getTaskIdentifier().getId(), when,
                interval*1000);
    }

    private static PendingIntent createPendingIntent(Context context, long id) {
        Intent intent = new Intent(context, Notifications.class);
        intent.setType(Long.toString(id));
        intent.putExtra(ID_KEY, id);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

        return sender;
    }

    /** Delete the given alarm */
    public static void deleteAlarm(Context context, long id) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        am.cancel(createPendingIntent(context, id));
    }

    /** Schedules a recurring alarm for a single task */
    public static void scheduleAlarm(Context context, long id, long when, long interval) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Log.e("ALARM", "Alarm set for " + new Date(when));
        am.setRepeating(AlarmManager.RTC, when, interval, createPendingIntent(context, id));
    }

    // --- notification manager stuff

    /** Clear notifications associated with this application */
    public static void clearAllNotifications(Context context, TaskIdentifier taskId) {
        NotificationManager nm = (NotificationManager)
            context.getSystemService(Activity.NOTIFICATION_SERVICE);
        nm.cancel((int)taskId.getId());
    }

    /** Schedule a new notification about the given task. Returns false if there was
     * some sort of error or the alarm should be disabled. */
    public static boolean showNotification(Context context, long id) {

        TaskController controller = new TaskController(context);
        try {
            controller.open();
            TaskModelForList task = controller.fetchTaskForList(new TaskIdentifier(id));

            // you're working on it - don't delete
            if(task.getTimerStart() != null)
                return true;

            // you're done - delete
            if(task.isTaskCompleted())
                return false;

            // it's hidden - don't delete
            if(task.getHiddenUntil() != null && task.getHiddenUntil().after(new Date()))
                return true;

        } catch (Exception e) {
            // task could be deleted, for example
            Log.e(Notifications.class.getSimpleName(), "Error loading task", e);
            return false;
        } finally {
            controller.close();
        }

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

        Log.w("Notifications", "Logging notification: " + reminder);
        nm.notify((int)id, notification);

        return true;
    }

}