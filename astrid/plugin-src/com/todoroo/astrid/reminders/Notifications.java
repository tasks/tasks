package com.todoroo.astrid.reminders;

import java.util.Date;
import java.util.Random;

import android.app.Activity;
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
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForReminder;
import com.timsu.astrid.utilities.Constants;
import com.timsu.astrid.utilities.Preferences;

public class Notifications extends BroadcastReceiver {

    static final String ID_KEY                  = "id";
    static final String TYPE_KEY               = "flags";
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

        // quiet hours? disabled if alarm clock
        boolean quietHours = false;
        Integer quietHoursStart = Preferences.getQuietHourStart(context);
        Integer quietHoursEnd = Preferences.getQuietHourEnd(context);
        if(quietHoursStart != null && quietHoursEnd != null && nonstopMode) {
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
        } else {
            notification.audioStreamType = AudioManager.STREAM_NOTIFICATION;
        }

        // quiet hours = no sound
        if(quietHours) {
            notification.sound = null;
        } else {
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

        // quiet hours + periodic = no vibrate
        if(quietHours && (flags & FLAG_PERIODIC) > 0) {
            notification.vibrate = null;
        } else {
            if (Preferences.shouldVibrate(context)
                    && audioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_NOTIFICATION)) {
                notification.vibrate = new long[] {0, 1000, 500, 1000, 500, 1000};
            } else {
                notification.vibrate = null;
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