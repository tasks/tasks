package com.timsu.astrid.utilities;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;

import com.timsu.astrid.R;
import com.timsu.astrid.activities.TaskListNotify;
import com.timsu.astrid.activities.TaskListSubActivity;
import com.timsu.astrid.data.alerts.AlertController;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForList;
import com.timsu.astrid.data.task.TaskModelForNotify;
import com.timsu.astrid.data.task.TaskModelForReminder;

public class Notifications extends BroadcastReceiver {

    private static final String ID_KEY                  = "id";
    private static final String FLAGS_KEY               = "flags";
    private static final String REPEAT_KEY              = "repeat";
    private static final int TAG_ID_OFFSET              = 100000;

    // stuff for scheduling
    /** minimum # of seconds before a deadline to notify */
    private static final int    DEADLINE_NOTIFY_SECS    = 60 * 60;
    /** # of seconds after deadline to repeat reminder*/
    private static final int    DEADLINE_REPEAT         = 10 * 60;

    // flags
    public static final int     FLAG_DEFINITE_DEADLINE  = 1 << 0;
    public static final int     FLAG_PREFERRED_DEADLINE = 1 << 1;
    public static final int     FLAG_OVERDUE            = 1 << 2;
    public static final int     FLAG_PERIODIC           = 1 << 3;
    public static final int     FLAG_FIXED              = 1 << 4;
    public static final int     FLAG_SNOOZE             = 1 << 5;
    /** # of bits to shift the fixed alert ID */
    public static final int     FIXED_ID_SHIFT          = 6;

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

        Resources r = context.getResources();
        String reminder;
        if((flags & FLAG_DEFINITE_DEADLINE) > 0)
            reminder = r.getString(R.string.notif_definiteDueDate);
        else if((flags & FLAG_PREFERRED_DEADLINE) > 0)
            reminder = r.getString(R.string.notif_preferredDueDate);
        else
            reminder = getRandomReminder(r);

        long repeatInterval = intent.getLongExtra(REPEAT_KEY, 0);
        if(Constants.DEBUG)
            Log.e("ALARM", "Alarm triggered id " + id +", flags " + flags +
                ", repeat " + repeatInterval);

        if(!showNotification(context, id, flags, repeatInterval, reminder)) {
            deleteAlarm(context, intent, id);
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
        try {
            TaskController taskController = new TaskController(context);
            taskController.open();
            AlertController alertController = new AlertController(context);
            alertController.open();

            Set<TaskModelForNotify> tasks = taskController.getTasksWithNotifications();

            Set<TaskIdentifier> tasksWithAlerts = alertController.getTasksWithActiveAlerts();
            for(TaskIdentifier taskId : tasksWithAlerts) {
                try {
                    tasks.add(taskController.fetchTaskForNotify(taskId));
                } catch (Exception e) {
                    // task was deleted or something
                }
            }

            for(TaskModelForNotify task : tasks)
                updateAlarm(context, taskController, alertController, task);

            alertController.close();
            taskController.close();
        } catch (Exception e) {
            Log.e("astrid", "Error scheduling alarms", e);
        }
    }

    /** Schedules the next notification for this task */
    public static void updateAlarm(Context context, TaskController taskController,
            AlertController alertController, Notifiable task) {
        if(task.getTaskIdentifier() == null)
            return;

        // return if we don't need to go any further
        if(shouldDeleteAlarm(task)) {
        	deleteAlarm(context, null, task.getTaskIdentifier().getId());
        	return;
        }

        // periodic reminders
        if(task.getNotificationIntervalSeconds() > 0) {
            long interval = task.getNotificationIntervalSeconds() * 1000;

            long when;
            // get or make up a last notification time
            if(task.getLastNotificationDate() == null) {
                when = System.currentTimeMillis() -
                    (long)(interval * (0.7f * random.nextFloat()));
                taskController.setLastNotificationTime(task.getTaskIdentifier(),
                        new Date(when));
            } else {
                when = task.getLastNotificationDate().getTime();
            }

            if(when < System.currentTimeMillis())
                when += ((System.currentTimeMillis() - when)/interval + 1) * interval;
            scheduleRepeatingAlarm(context, task.getTaskIdentifier().getId(),
                    when, FLAG_PERIODIC, interval);
        }

        // notifications at deadlines
        int estimatedDuration = DEADLINE_NOTIFY_SECS;
        if(task.getEstimatedSeconds() != null && task.getEstimatedSeconds() > DEADLINE_NOTIFY_SECS)
            estimatedDuration = (int)(task.getEstimatedSeconds() * 1.5f);

        // we need to clear all alarms in case users removed a deadline
        clearAlarm(context, task.getTaskIdentifier().getId(), FLAG_DEFINITE_DEADLINE);
        clearAlarm(context, task.getTaskIdentifier().getId(), FLAG_PREFERRED_DEADLINE);
        clearAlarm(context, task.getTaskIdentifier().getId(), FLAG_DEFINITE_DEADLINE | FLAG_OVERDUE);
        clearAlarm(context, task.getTaskIdentifier().getId(), FLAG_PREFERRED_DEADLINE | FLAG_OVERDUE);

        // before, during, and after deadlines
        if((task.getNotificationFlags() & TaskModelForList.NOTIFY_BEFORE_DEADLINE) > 0) {
            scheduleDeadline(context, task.getDefiniteDueDate(), -estimatedDuration,
                    0, FLAG_DEFINITE_DEADLINE, task);
            scheduleDeadline(context, task.getPreferredDueDate(), -estimatedDuration,
                    0, FLAG_PREFERRED_DEADLINE, task);
        }
        if((task.getNotificationFlags() & TaskModelForList.NOTIFY_AT_DEADLINE) > 0) {
            if((task.getNotificationFlags() & TaskModelForList.NOTIFY_AFTER_DEADLINE) == 0)
                scheduleDeadline(context, task.getDefiniteDueDate(), 0,
                        0, FLAG_DEFINITE_DEADLINE | FLAG_OVERDUE, task);
            scheduleDeadline(context, task.getPreferredDueDate(), 0,
                    0, FLAG_PREFERRED_DEADLINE | FLAG_OVERDUE, task);
        }
        if((task.getNotificationFlags() & TaskModelForList.NOTIFY_AFTER_DEADLINE) > 0) {
            scheduleDeadline(context, task.getDefiniteDueDate(), 0,
                    DEADLINE_REPEAT, FLAG_DEFINITE_DEADLINE | FLAG_OVERDUE, task);
        }

        // fixed alerts
        List<Date> alerts = alertController.getTaskAlerts(task.getTaskIdentifier());
        scheduleFixedAlerts(context, task.getTaskIdentifier(), alerts);
    }

    /** Schedule a list of alerts for a task */
    public static void scheduleFixedAlerts(Context context, TaskIdentifier taskId,
            List<Date> alerts) {
        int alertId = 0;
        Date currentDate = new Date();
        for(Date alert : alerts) {
            if(alert.before(currentDate))
                continue;

            scheduleAlarm(context, taskId.getId(),
                    alert.getTime(), FLAG_FIXED | (alertId++ << FIXED_ID_SHIFT));
        }
    }

    /** Schedule an alert around a deadline
     *
     * @param context
     * @param deadline The deadline date. If null, does nothing.
     * @param offsetSeconds Offset from deadline to schedule
     * @param intervalSeconds How often to repeat, or zero
     * @param flags Flags for the alarm
     * @param task
     */
    private static void scheduleDeadline(Context context, Date deadline, int
            offsetSeconds, int intervalSeconds, int flags, Notifiable task) {
        long id = task.getTaskIdentifier().getId();
        if(deadline == null)
            return;
        long when = deadline.getTime() + offsetSeconds * 1000;
        if(when < System.currentTimeMillis() && intervalSeconds == 0)
            return;

        if (intervalSeconds == 0)
            scheduleAlarm(context, id, when,
                    flags);
        else
            scheduleRepeatingAlarm(context, id,
                    when, flags, intervalSeconds * 1000);
    }

    /** Create a 'snooze' reminder for this task */
    public static void createSnoozeAlarm(Context context, TaskIdentifier id,
            int secondsToSnooze, int flags, long repeatInterval) {
        // if this is a one-off alarm, just schedule a snooze-type alarm
        if(repeatInterval == 0)
            scheduleAlarm(context, id.getId(), System.currentTimeMillis() +
                secondsToSnooze * 1000, FLAG_SNOOZE);

        // else, reschedule our normal alarm
        else
            scheduleRepeatingAlarm(context, id.getId(), System.currentTimeMillis() +
                    secondsToSnooze * 1000, flags, repeatInterval);
    }

    /** Helper method to create a Intent for alarm from an ID & flags */
    private static Intent createAlarmIntent(Context context, long id, int flags) {
        Intent intent = new Intent(context, Notifications.class);
        intent.setType(Long.toString(id));
        intent.setAction(Integer.toString(flags));
        intent.putExtra(ID_KEY, id);
        intent.putExtra(FLAGS_KEY, flags);

        return intent;
    }

    /** Delete the given alarm */
    public static void deleteAlarm(Context context, Intent trigger, long id) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        if(trigger != null) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                trigger, 0);
            am.cancel(pendingIntent);
        }

        // clear current notifications too
        clearAllNotifications(context, new TaskIdentifier(id));
    }

    /** Clear the alarm given by the id and flags */
    public static void clearAlarm(Context context, long id, int flags) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                createAlarmIntent(context, id, flags), 0);
        am.cancel(pendingIntent);
    }

    /** Schedules a single alarm for a single task */
    public static void scheduleAlarm(Context context, long id, long when,
            int flags) {

        // if alarm occurs in the past, don't trigger it
        if(when < System.currentTimeMillis())
            return;

        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                createAlarmIntent(context, id, flags), 0);

        if(Constants.DEBUG)
            Log.e("Astrid", "Alarm (" + id + ", " + flags + ") set for " + new Date(when));
        am.set(AlarmManager.RTC_WAKEUP, when, pendingIntent);
    }

    /** Schedules a recurring alarm for a single task */
    public static void scheduleRepeatingAlarm(Context context, long id, long when,
            int flags, long interval) {

        // if alarm occurs in the past, trigger it in the future
        if(when < System.currentTimeMillis())
            when = (long)(System.currentTimeMillis() + when * (0.8 + 0.3 * random.nextDouble()));

        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = createAlarmIntent(context, id, flags);
        alarmIntent.putExtra(REPEAT_KEY, interval);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                alarmIntent, 0);

        if(Constants.DEBUG)
            Log.e("Astrid", "Alarm (" + id + ", " + flags + ") set for " +
                new Date(when) + " every " + interval/1000 + " s");
        am.setRepeating(AlarmManager.RTC_WAKEUP, when, interval, pendingIntent);
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
            int flags, long repeatInterval, String reminder) {

        String taskName;
        TaskController controller = new TaskController(context);
        boolean nonstopMode = false;
        try {
            controller.open();
            TaskModelForReminder task = controller.fetchTaskForReminder(new TaskIdentifier(id));

            // you're working on it - don't sound, don't delete
            if(task.getTimerStart() != null)
                return true;

            // you're done - don't sound, do delete
            if(task.isTaskCompleted())
                return false;

            // it's hidden - don't sound, don't delete
            if(task.getHiddenUntil() != null &&
                    task.getHiddenUntil().after(new Date()) &&
                    (flags & FLAG_PERIODIC) > 0)
                return true;

            taskName = task.getName();
            if((flags & FLAG_PERIODIC) > 0)
                controller.setLastNotificationTime(task.getTaskIdentifier(),
                        new Date());

            if((task.getNotificationFlags() & TaskModelForReminder.NOTIFY_NONSTOP) > 0)
                nonstopMode = true;

        } catch (Exception e) {
            // task might have been deleted
            Log.e(Notifications.class.getSimpleName(),
                    "Error loading task for notification", e);
            return false;
        } finally {
            controller.close();
        }

        // quiet hours? only for periodic reminders
        boolean quietHours = false;
        Integer quietHoursStart = Preferences.getQuietHourStart(context);
        Integer quietHoursEnd = Preferences.getQuietHourEnd(context);
        if(quietHoursStart != null && quietHoursEnd != null &&
                (flags & FLAG_PERIODIC) > 0) {
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

        Intent notifyIntent = new Intent(context, TaskListNotify.class);
        notifyIntent.putExtra(TaskListSubActivity.LOAD_INSTANCE_TOKEN, id);
        notifyIntent.putExtra(TaskListSubActivity.FROM_NOTIFICATION_TOKEN, true);
        notifyIntent.putExtra(TaskListSubActivity.NOTIF_FLAGS_TOKEN, flags);
        notifyIntent.putExtra(TaskListSubActivity.NOTIF_REPEAT_TOKEN, repeatInterval);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                (int)id, notifyIntent, PendingIntent.FLAG_ONE_SHOT);

        // set up properties (name and icon) for the notification
        String appName = r.getString(R.string.app_name);
        int icon;
        switch(Preferences.getNotificationIconTheme(context)) {
        case Preferences.ICON_SET_PINK:
            icon = R.drawable.notif_pink_alarm;
            break;
        case Preferences.ICON_SET_BORING:
            icon = R.drawable.notif_boring_alarm;
            break;
        default:
            icon = R.drawable.notif_astrid;
        }

        // create notification object
        Notification notification = new Notification(
                icon, reminder, System.currentTimeMillis());
        notification.setLatestEventInfo(context,
                appName,
                reminder + " " + taskName,
                pendingIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        if(Preferences.isPersistenceMode(context)) {
            notification.flags |= Notification.FLAG_NO_CLEAR |
                Notification.FLAG_SHOW_LIGHTS;
            notification.ledOffMS = 5000;
            notification.ledOnMS = 700;
            notification.ledARGB = Color.YELLOW;
        }
        else
            notification.defaults = Notification.DEFAULT_LIGHTS;

        AudioManager audioManager = (AudioManager)context.getSystemService(
                Context.AUDIO_SERVICE);

        // if nonstop mode is activated, set up the flags for insistent
        // notification, and increase the volume to full volume, so the user
        // will actually pay attention to the alarm
        if(nonstopMode && (flags & FLAG_PERIODIC) == 0) {
            notification.flags |= Notification.FLAG_INSISTENT;
            notification.audioStreamType = AudioManager.STREAM_ALARM;
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
        }

        if(quietHours) {
            notification.vibrate = null;
            notification.sound = null;
        } else {
            if(audioManager.getVibrateSetting(AudioManager.STREAM_RING) !=
                    AudioManager.VIBRATE_SETTING_OFF)
                notification.defaults |= Notification.DEFAULT_VIBRATE;
            else
                notification.vibrate = null;

            Uri notificationSound = Preferences.getNotificationRingtone(context);
            if(audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
                notification.sound = null;
            } else if(notificationSound != null &&
                    !notificationSound.toString().equals("")) {
                notification.sound = notificationSound;
            } else {
                notification.defaults |= Notification.DEFAULT_SOUND;
            }
        }

        if(Constants.DEBUG)
            Log.w("Astrid", "Logging notification: " + reminder);
        nm.notify((int)id, notification);

        return true;
    }

    /** Show a notification when a user is "on-the-clock" for a given task */
    public static boolean showTimingNotification(Context context,
    		TaskIdentifier taskId, String taskName) {

    	String text = context.getResources().getString(R.string.notif_timerStarted) +
    		" " + taskName;
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Resources r = context.getResources();

        Intent notifyIntent = new Intent(context, TaskListNotify.class);
        notifyIntent.putExtra(TaskListSubActivity.LOAD_INSTANCE_TOKEN, taskId.getId());
        notifyIntent.putExtra(TaskListSubActivity.FROM_NOTIFICATION_TOKEN, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                (int)taskId.getId(), notifyIntent, 0);

        // create notification object
        int icon;
        switch(Preferences.getNotificationIconTheme(context)) {
        case Preferences.ICON_SET_PINK:
            icon = R.drawable.notif_pink_working;
            break;
        case Preferences.ICON_SET_BORING:
            icon = R.drawable.notif_boring_working;
            break;
        default:
            icon = R.drawable.notif_astrid;
        }

        String appName = r.getString(R.string.app_name);
        Notification notification = new Notification(
                icon, text, System.currentTimeMillis());
        notification.setLatestEventInfo(context,
                appName,
                text,
                pendingIntent);
        notification.flags |= Notification.FLAG_ONGOING_EVENT |
            Notification.FLAG_NO_CLEAR;
        notification.flags &= ~Notification.FLAG_AUTO_CANCEL;

        if(Constants.DEBUG)
            Log.w("Astrid", "Logging timing notification: " + text);
        nm.notify((int)taskId.getId(), notification);

        return true;
    }

    /** Schedule a new notification about the given tag. */
    public static boolean showTagNotification(Context context, long tagId,
    		String reminder) {

        // quiet hours? only for periodic reminders
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

        Intent notifyIntent = new Intent(context, TaskListNotify.class);
        notifyIntent.putExtra(TaskListSubActivity.TAG_TOKEN, tagId);
        notifyIntent.putExtra(TaskListSubActivity.FROM_NOTIFICATION_TOKEN, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
        		TAG_ID_OFFSET + (int)tagId, notifyIntent, PendingIntent.FLAG_ONE_SHOT);

        // set up properties (name and icon) for the notification
        String appName = r.getString(R.string.app_name);
        int icon = R.drawable.notif_tag;

        // create notification object
        Notification notification = new Notification(
                icon, reminder, System.currentTimeMillis());
        notification.setLatestEventInfo(context,
                appName,
                reminder,
                pendingIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.ledARGB = Color.BLUE;
        notification.defaults = Notification.DEFAULT_LIGHTS;

        if(quietHours) {
            notification.vibrate = null;
            notification.sound = null;
        } else {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
            Uri notificationSound = Preferences.getNotificationRingtone(context);
            if(notificationSound != null &&
                    !notificationSound.toString().equals("")) {
                notification.sound = notificationSound;
            } else {
                notification.defaults |= Notification.DEFAULT_SOUND;
            }
        }

        if(Constants.DEBUG)
            Log.w("Astrid", "Logging tag notification: " + reminder);
        nm.notify(TAG_ID_OFFSET + (int)tagId, notification);

        return true;
    }
}