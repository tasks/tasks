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
import android.database.Cursor;
import android.util.Log;

import com.timsu.astrid.R;
import com.timsu.astrid.activities.TaskViewNotifier;
import com.timsu.astrid.data.alerts.Alert;
import com.timsu.astrid.data.alerts.AlertController;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForList;
import com.timsu.astrid.data.task.TaskModelForNotify;

public class Notifications extends BroadcastReceiver {

    private static final String ID_KEY                  = "id";
    private static final String FLAGS_KEY               = "flags";

    // stuff for scheduling

    /** min # of seconds before a deadline to notify */
    private static final int    DEADLINE_NOTIFY_SECS    = 3600;
    /** # of seconds after deadline to repeat */
    private static final int    DEADLINE_REPEAT         = 300;

    // flags
    public static final int     FLAG_DEFINITE_DEADLINE  = 1;
    public static final int     FLAG_PREFERRED_DEADLINE = 2;
    public static final int     FLAG_OVERDUE            = 4;
    public static final int     FLAG_FIXED              = 8;

    private static Random       random                  = new Random();

    /** Something we can create a notification for */
    public interface Notifiable {
        public TaskIdentifier getTaskIdentifier();
        public Integer getNotificationIntervalSeconds();
        public boolean isTaskCompleted();
        public Date getHiddenUntil();
        public Date getDefiniteDueDate();
        public Date getPreferredDueDate();
        public Date getLastNotificationDate();
        public int getNotificationFlags();
        public Integer getEstimatedSeconds();
    }

    @Override
    /** Alarm intent */
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(ID_KEY, 0);
        int flags = intent.getIntExtra(FLAGS_KEY, 0);
        Log.e("ALARM", "Alarm triggered id " + id);

        Resources r = context.getResources();
        String reminder;
        if((flags & FLAG_DEFINITE_DEADLINE) > 0)
            reminder = r.getString(R.string.notif_definiteDueDate);
        else if((flags & FLAG_PREFERRED_DEADLINE) > 0)
            reminder = r.getString(R.string.notif_preferredDueDate);
        else
            reminder = getRandomReminder(r);

        if(!showNotification(context, id, flags, reminder)) {
            deleteAlarm(context, id);
            NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel((int)id);
        }
    }

    // --- alarm manager stuff

    private static boolean shouldDeleteAlarm(Notifiable task) {
        if(task.isTaskCompleted())
            return true;

        return false;
    }

    public static void scheduleAllAlarms(Context context) {
        TaskController taskController = new TaskController(context);
        taskController.open();
        AlertController alertController = new AlertController(context);
        alertController.open();

        List<TaskModelForNotify> tasks = taskController.getTasksWithNotifications();
        for(TaskModelForNotify task : tasks)
            updateAlarm(context, taskController, alertController, task);

        alertController.close();
        taskController.close();
    }

    /** Schedules the next notification for this task */
    public static void updateAlarm(Context context, TaskController taskController,
            AlertController alertController, Notifiable task) {
        if(task.getTaskIdentifier() == null)
            return;

        if(shouldDeleteAlarm(task)) {
            deleteAlarm(context, task.getTaskIdentifier().getId());
            return;
        }

        // periodic reminders
        if(task.getNotificationIntervalSeconds() > 0) {
            long interval = task.getNotificationIntervalSeconds() * 1000;

            long when;
            // get or make up a last notification time
            if(task.getLastNotificationDate() == null) {
                when = System.currentTimeMillis() +
                    (long)(interval * (0.3f + 0.7f * random.nextFloat()));
                taskController.setLastNotificationTime(task.getTaskIdentifier(),
                        new Date(when));
            } else {
                when = task.getLastNotificationDate().getTime();
            }

            if(when < System.currentTimeMillis())
                when += ((System.currentTimeMillis() - when)/interval + 1) * interval;
            scheduleRepeatingAlarm(context, task.getTaskIdentifier().getId(),
                    when, 0, interval);
        }

        // before, during, and after deadlines
        int estimatedDuration = DEADLINE_NOTIFY_SECS;
        if(task.getEstimatedSeconds() != null && task.getEstimatedSeconds() > DEADLINE_NOTIFY_SECS)
            estimatedDuration = (int)(task.getEstimatedSeconds() * 1.5f);
        if((task.getNotificationFlags() & TaskModelForList.NOTIFY_BEFORE_DEADLINE) > 0) {
            scheduleDeadline(context, task.getDefiniteDueDate(), -estimatedDuration,
                    0, FLAG_DEFINITE_DEADLINE, task);
            scheduleDeadline(context, task.getPreferredDueDate(), -estimatedDuration,
                    0, FLAG_PREFERRED_DEADLINE, task);
        }
        if((task.getNotificationFlags() & TaskModelForList.NOTIFY_AT_DEADLINE) > 0) {
            scheduleDeadline(context, task.getDefiniteDueDate(), 0,
                    0, FLAG_DEFINITE_DEADLINE | FLAG_OVERDUE, task);
            scheduleDeadline(context, task.getPreferredDueDate(), 0,
                    0, FLAG_PREFERRED_DEADLINE | FLAG_OVERDUE, task);
        }
        if((task.getNotificationFlags() & TaskModelForList.NOTIFY_AFTER_DEADLINE) > 0) {
            scheduleDeadline(context, task.getDefiniteDueDate(), DEADLINE_REPEAT,
                    DEADLINE_REPEAT, FLAG_DEFINITE_DEADLINE | FLAG_OVERDUE,
                    task);
        }

        // fixed alerts
        Cursor cursor = alertController.getTaskAlertsCursor(task.getTaskIdentifier());
        Date currentDate = new Date();
        while(cursor.getCount() > 0 && !cursor.isLast()) {
            cursor.moveToNext();
            Date alert = new Alert(cursor).getDate();
            if(alert.before(currentDate))
                continue;

            scheduleAlarm(context, task.getTaskIdentifier().getId(),
                    alert.getTime(), FLAG_FIXED);
        }
        cursor.close();
    }

    private static void scheduleDeadline(Context context, Date deadline, int
            offsetSeconds, int intervalSeconds, int flags, Notifiable task) {
        if(deadline == null)
            return;
        long when = deadline.getTime() + offsetSeconds * 1000;
        if(when < System.currentTimeMillis() && intervalSeconds == 0)
            return;

        if (intervalSeconds == 0)
            scheduleAlarm(context, task.getTaskIdentifier().getId(), when,
                    flags);
        else
            scheduleRepeatingAlarm(context, task.getTaskIdentifier().getId(),
                    when, flags, intervalSeconds * 1000);
    }

    private static PendingIntent createPendingIntent(Context context,
            long id, int flags) {
        Intent intent = new Intent(context, Notifications.class);
        intent.setType(Long.toString(id));
        intent.setAction(Integer.toString(flags));
        intent.putExtra(ID_KEY, id);
        intent.putExtra(FLAGS_KEY, flags);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

        return sender;
    }

    /** Delete the given alarm */
    public static void deleteAlarm(Context context, long id) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        am.cancel(createPendingIntent(context, id, 0));

        // clear current notifications too
        clearAllNotifications(context, new TaskIdentifier(id));
    }

    /** Schedules a single alarm for a single task */
    public static void scheduleAlarm(Context context, long id, long when,
            int flags) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Log.e("Astrid", "Alarm set for " + new Date(when));
        am.set(AlarmManager.RTC_WAKEUP, when, createPendingIntent(context, id, flags));
    }

    /** Schedules a recurring alarm for a single task */
    public static void scheduleRepeatingAlarm(Context context, long id, long when,
            int flags, long interval) {
        if(when < System.currentTimeMillis())
            return;

        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Log.e("Astrid", "Alarm set for " + new Date(when) + " every " + interval/1000 + " s");
        am.setRepeating(AlarmManager.RTC_WAKEUP, when, interval,
                createPendingIntent(context, id, flags));
    }

    // --- notification manager stuff

    /** Clear notifications associated with this application */
    public static void clearAllNotifications(Context context, TaskIdentifier taskId) {
        NotificationManager nm = (NotificationManager)
            context.getSystemService(Activity.NOTIFICATION_SERVICE);
        nm.cancel((int)taskId.getId());
    }

    private static String getRandomReminder(Resources r) {
        String[] reminders = r.getStringArray(R.array.reminders);
        int next = random.nextInt(reminders.length);
        String reminder = reminders[next];
        return reminder;
    }

    /** Schedule a new notification about the given task. Returns false if there was
     * some sort of error or the alarm should be disabled. */
    public static boolean showNotification(Context context, long id,
            int flags, String reminder) {

        String taskName;
        TaskController controller = new TaskController(context);
        try {
            controller.open();
            TaskModelForList task = controller.fetchTaskForList(new TaskIdentifier(id));

            // you're working on it - don't sound, don't delete
            if(task.getTimerStart() != null)
                return true;

            // you're done - don't sound, do delete
            if(task.isTaskCompleted())
                return false;

            // it's hidden - don't sound, don't delete
            if(task.getHiddenUntil() != null && task.getHiddenUntil().after(new Date()))
                return true;

            taskName = task.getName();
            controller.setLastNotificationTime(task.getTaskIdentifier(), new Date());

        } catch (Exception e) {
            // task could be deleted, for example
            Log.e(Notifications.class.getSimpleName(),
                    "Error loading task for notification", e);
            return false;
        } finally {
            controller.close();
        }

        // quiet hours?
        boolean quietHours = false;
        Integer quietHoursStart = Preferences.getQuietHourStart(context);
        Integer quietHoursEnd = Preferences.getQuietHourEnd(context);
        if(quietHoursStart != null && quietHoursEnd != null) {
            int hour = new Date().getHours();
            if(quietHoursStart < quietHoursEnd) {
                if(hour >= quietHoursStart && hour < quietHoursEnd)
                    quietHours = true;
            } else { // wrap across 24/hour boundary
                if(hour >= quietHoursStart || hour < quietHoursEnd)
                    quietHours = true;
            }
        }

        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Resources r = context.getResources();

        Intent notifyIntent = new Intent(context, TaskViewNotifier.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        notifyIntent.putExtra(TaskViewNotifier.LOAD_INSTANCE_TOKEN, id);
        notifyIntent.putExtra(TaskViewNotifier.FROM_NOTIFICATION_TOKEN, true);
        notifyIntent.putExtra(TaskViewNotifier.NOTIF_FLAGS_TOKEN, flags);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                (int)id, notifyIntent, PendingIntent.FLAG_ONE_SHOT);

        // create notification object
        String appName = r.getString(R.string.app_name);
        Notification notification = new Notification(
                android.R.drawable.stat_notify_chat, reminder,
                System.currentTimeMillis());
        notification.setLatestEventInfo(context,
                appName,
                reminder + " " + taskName,
                pendingIntent);
        if(!quietHours) {
            notification.vibrate = null;
            notification.sound = null;
        }

        Log.w("Astrid", "Logging notification: " + reminder);
        nm.notify((int)id, notification);

        return true;
    }

}