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

    private static final String ID_KEY                  = "id";
    private static final String FLAGS_KEY               = "flags";

    // stuff for scheduling
    private static final int    MIN_INTERVAL_SECONDS    = 300;
    private static final float  FUDGE_MIN               = 0.2f;
    private static final float  FUDGE_MAX               = 0.8f;
    /** # of seconds before a deadline to notify */
    private static final int    DEADLINE_NOTIFY_SECS    = 3600;
    /** # of seconds after now, if a deadline is in the past */
    private static final int    TIME_IN_PAST_OFFSET     = 60;
    /** # of seconds after first deadline reminder to repeat */
    private static final int    DEADLINE_REPEAT         = 600;

    // flags
    public static final int    FLAG_DEFINITE_DEADLINE   = 1;
    public static final int    FLAG_PREFERRED_DEADLINE  = 2;

    private static Random       random                  = new Random();

    /** Something we can create a notification for */
    public interface Notifiable {
        public TaskIdentifier getTaskIdentifier();
        public Integer getNotificationIntervalSeconds();
        public boolean isTaskCompleted();
        public Date getHiddenUntil();
        public Date getDefiniteDueDate();
        public Date getPreferredDueDate();
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
            return false;

        return true;
    }

    public static void scheduleAllAlarms(Context context) {
        TaskController controller = new TaskController(context);
        controller.open();
        List<TaskModelForNotify> tasks = controller.getTasksWithNotifications();

        for(TaskModelForNotify task : tasks)
            updateAlarm(context, task, false);

        controller.close();
    }

    /** Schedules the next notification for this task */
    public static void updateAlarm(Context context, Notifiable task,
            boolean shouldSnooze) {
        if(task.getTaskIdentifier() == null)
            return;

        if(!shouldDeleteAlarm(task)) {
            deleteAlarm(context, task.getTaskIdentifier().getId());
            return;
        }

        Long when = null;    // when to schedule alarm (ms)
        Integer interval = null; // how often to repeat (s)

        if(task.getNotificationIntervalSeconds() > 0) {
            // compute, and add a fudge factor to mix things up a bit
            interval = task.getNotificationIntervalSeconds();
            int currentSeconds = (int)(System.currentTimeMillis() / 1000);
            int untilNextInterval = interval - currentSeconds % interval;
            untilNextInterval = 60;
            untilNextInterval *= FUDGE_MIN + random.nextFloat() * (FUDGE_MAX - FUDGE_MIN);
            if(untilNextInterval < MIN_INTERVAL_SECONDS)
                untilNextInterval = MIN_INTERVAL_SECONDS;
            when = System.currentTimeMillis() + untilNextInterval * 1000;
        }

        // if deadlines come before, do that instead
        int flags = 0;
        if(task.getDefiniteDueDate() != null) {
            long deadlineWhen = task.getDefiniteDueDate().getTime() -
                DEADLINE_NOTIFY_SECS * 1000;
            if(when == null || deadlineWhen < when) {
                when = deadlineWhen;
                interval = DEADLINE_REPEAT;
                flags = FLAG_DEFINITE_DEADLINE;
            }
        }
        if(task.getPreferredDueDate() != null) {
            long deadlineWhen = task.getPreferredDueDate().getTime() -
                DEADLINE_NOTIFY_SECS * 1000;
            if(when == null || deadlineWhen < when) {
                when = deadlineWhen;
                interval = DEADLINE_REPEAT;
                flags = FLAG_PREFERRED_DEADLINE;
            }
        }

        if(when == null) {
            deleteAlarm(context, task.getTaskIdentifier().getId());
            return;
        }

        // snooze if the user just interacted with this item
        if(shouldSnooze) {
            long snoozeWhen = System.currentTimeMillis() +
                DEADLINE_REPEAT * 1000;
            if(when < snoozeWhen)
                when = snoozeWhen;
        } else if(when < System.currentTimeMillis())
            when = System.currentTimeMillis() + TIME_IN_PAST_OFFSET*1000;

        if(interval == null)
            scheduleAlarm(context, task.getTaskIdentifier().getId(), when, flags);
        else
            scheduleRepeatingAlarm(context, task.getTaskIdentifier().getId(),
                    when, flags, interval*1000);
    }

    private static PendingIntent createPendingIntent(Context context,
            long id, int flags) {
        Intent intent = new Intent(context, Notifications.class);
        intent.setType(Long.toString(id));
        intent.putExtra(ID_KEY, id);
        intent.putExtra(FLAGS_KEY, flags);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

        return sender;
    }

    /** Delete the given alarm */
    public static void deleteAlarm(Context context, long id) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        am.cancel(createPendingIntent(context, id, 0));
    }

    /** Schedules a single alarm for a single task */
    public static void scheduleAlarm(Context context, long id, long when,
            int flags) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Log.e("ALARM", "Alarm set for " + new Date(when));
        am.set(AlarmManager.RTC_WAKEUP, when, createPendingIntent(context, id, flags));
    }

    /** Schedules a recurring alarm for a single task */
    public static void scheduleRepeatingAlarm(Context context, long id, long when,
            int flags, long interval) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Log.e("ALARM", "Alarm set for " + new Date(when) + " every " + interval);
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
    public static boolean showNotification(Context context, long id, int flags,
            String reminder) {

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

            // it's hidden - don't sound, do don't delete
            if(task.getHiddenUntil() != null && task.getHiddenUntil().after(new Date()))
                return true;

            taskName = task.getName();

        } catch (Exception e) {
            // task could be deleted, for example
            Log.e(Notifications.class.getSimpleName(),
                    "Error loading task for notification", e);
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
        notifyIntent.putExtra(TaskView.NOTIF_FLAGS_TOKEN, flags);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, notifyIntent, PendingIntent.FLAG_ONE_SHOT);

        // notification text
        String appName = r.getString(R.string.app_name);

        Notification notification = new Notification(
                android.R.drawable.stat_notify_chat, reminder,
                System.currentTimeMillis());

        notification.setLatestEventInfo(context,
                appName + ": " + taskName,
                reminder,
                pendingIntent);

        notification.defaults = Notification.DEFAULT_ALL;

        Log.w("Notifications", "Logging notification: " + reminder);
        nm.notify((int)id, notification);

        return true;
    }

}