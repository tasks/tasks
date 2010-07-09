package com.todoroo.astrid.reminders;

import java.util.Date;

import android.app.Activity;
import android.app.Notification;
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
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.utilities.Constants;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.utility.Preferences;

public class Notifications extends BroadcastReceiver {

    // --- constants

    /** task id extra */
    static final String ID_KEY = "id"; //$NON-NLS-1$

    /** notification type extra */
    static final String TYPE_KEY = "type"; //$NON-NLS-1$

    /** preference values */
    public static final int ICON_SET_PINK = 0;
    public static final int ICON_SET_BORING = 1;
    public static final int ICON_SET_ASTRID = 2;

    // --- instance variables

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private ExceptionService exceptionService;

    public static NotificationManager notificationManager = null;

    // --- alarm handling

    static {
        AstridDependencyInjector.initialize();
    }

    public Notifications() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    /** Alarm intent */
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);

        long id = intent.getLongExtra(ID_KEY, 0);
        int type = intent.getIntExtra(TYPE_KEY, (byte) 0);

        Resources r = context.getResources();
        String reminder;
        if(type == ReminderService.TYPE_DUE || type == ReminderService.TYPE_OVERDUE)
            reminder = getRandomReminder(r.getStringArray(R.array.reminders_due));
        else if(type == ReminderService.TYPE_SNOOZE)
            reminder = getRandomReminder(r.getStringArray(R.array.reminders_snooze));
        else
            reminder = getRandomReminder(r.getStringArray(R.array.reminders));

        if(!showNotification(id, type, reminder)) {
            notificationManager.cancel((int)id);
        }
    }

    // --- notification creation

    /** Clear notifications associated with this application */
    public static void clearAllNotifications(Context context, TaskIdentifier taskId) {
        NotificationManager nm = (NotificationManager)
            context.getSystemService(Activity.NOTIFICATION_SERVICE);
        nm.cancel((int)taskId.getId());
    }

    /** @return a random reminder string */
    static String getRandomReminder(String[] reminders) {
        int next = ReminderService.random.nextInt(reminders.length);
        String reminder = reminders[next];
        return reminder;
    }

    /**
     * Schedule a new notification about the given task. Returns false if there was
     * some sort of error or the alarm should be disabled.
     */
    public boolean showNotification(long id, int type, String reminder) {
        Context context = ContextManager.getContext();
        if(notificationManager == null)
            notificationManager = new AndroidNotificationManager(context);

        Task task;
        try {
            task = taskDao.fetch(id, Task.TITLE, Task.HIDE_UNTIL, Task.COMPLETION_DATE,
                    Task.DELETION_DATE, Task.REMINDER_FLAGS);
            if(task == null)
                throw new IllegalArgumentException("cound not find item with id"); //$NON-NLS-1$

        } catch (Exception e) {
            exceptionService.reportError("show-notif", e); //$NON-NLS-1$
            return false;
        }

        // you're done - don't sound, do delete
        if(task.isCompleted() || task.isDeleted())
            return false;

        // it's hidden - don't sound, don't delete
        if(task.isHidden() && type == ReminderService.TYPE_RANDOM)
            return true;

        // read properties
        String taskTitle = task.getValue(Task.TITLE);
        boolean nonstopMode = task.getFlag(Task.REMINDER_FLAGS, Task.NOTIFY_NONSTOP);

        // update last reminder time
        task.setValue(Task.REMINDER_LAST, DateUtilities.now());
        taskDao.saveExisting(task);

        // quiet hours? unless alarm clock
        boolean quietHours = false;
        Integer quietHoursStart = Preferences.getIntegerFromString(R.string.p_rmd_quietStart);
        Integer quietHoursEnd = Preferences.getIntegerFromString(R.string.p_rmd_quietEnd);
        if(quietHoursStart != null && quietHoursEnd != null && !nonstopMode) {
            int hour = new Date().getHours();
            if(quietHoursStart <= quietHoursEnd) {
                if(hour >= quietHoursStart && hour < quietHoursEnd)
                    quietHours = true;
            } else { // wrap across 24/hour boundary
                if(hour >= quietHoursStart || hour < quietHoursEnd)
                    quietHours = true;
            }
        }

        Resources r = context.getResources();

        Intent notifyIntent = new Intent(context, NotificationActivity.class);
        notifyIntent.putExtra(NotificationActivity.TOKEN_ID, id);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                (int)id, notifyIntent, PendingIntent.FLAG_ONE_SHOT);

        // set up properties (name and icon) for the notification
        String appName = r.getString(R.string.app_name);
        Integer iconPreference = Preferences.getIntegerFromString(R.string.p_rmd_icon);
        if(iconPreference == null)
            iconPreference = ICON_SET_ASTRID;
        int icon;
        switch(iconPreference) {
        case ICON_SET_PINK:
            icon = R.drawable.notif_pink_alarm;
            break;
        case ICON_SET_BORING:
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
                reminder + " " + taskTitle, //$NON-NLS-1$
                pendingIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        if(Preferences.getBoolean(R.string.p_rmd_persistent, true)) {
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

        // if non-stop mode is activated, set up the flags for insistent
        // notification, and increase the volume to full volume, so the user
        // will actually pay attention to the alarm
        if(nonstopMode && (type != ReminderService.TYPE_RANDOM)) {
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
            String notificationPreference = Preferences.getStringValue(R.string.p_rmd_ringtone);
            if(audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
                notification.sound = null;
            } else if(notificationPreference != null) {
                if(notificationPreference.length() > 0) {
                    Uri notificationSound = Uri.parse(notificationPreference);
                    notification.sound = notificationSound;
                } else {
                    notification.sound = null;
                }
            } else {
                notification.defaults |= Notification.DEFAULT_SOUND;
            }
        }

        // quiet hours + periodic = no vibrate
        if(quietHours && (type == ReminderService.TYPE_RANDOM)) {
            notification.vibrate = null;
        } else {
            if (Preferences.getBoolean(R.string.p_rmd_vibrate, true)
                    && audioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_NOTIFICATION)) {
                notification.vibrate = new long[] {0, 1000, 500, 1000, 500, 1000};
            } else {
                notification.vibrate = null;
            }
        }

        if(Constants.DEBUG)
            Log.w("Astrid", "Logging notification: " + reminder); //$NON-NLS-1$ //$NON-NLS-2$

        notificationManager.notify((int)id, notification);

        return true;
    }

    // --- notification manager

    public static void setNotificationManager(
            NotificationManager notificationManager) {
        Notifications.notificationManager = notificationManager;
    }

}