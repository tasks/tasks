/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.voice.VoiceOutputService;

public class Notifications extends BroadcastReceiver {

    // --- constants

    /** task id extra */
    public static final String ID_KEY = "id"; //$NON-NLS-1$


    /** preference values */
    public static final int ICON_SET_PINK = 0;
    public static final int ICON_SET_BORING = 1;
    public static final int ICON_SET_ASTRID = 2;

    /**
     * Action name for broadcast intent notifying that task was created from repeating template
     */
    public static final String BROADCAST_IN_APP_NOTIFY = Constants.PACKAGE + ".IN_APP_NOTIFY"; //$NON-NLS-1$
    public static final String EXTRAS_CUSTOM_INTENT = "intent"; //$NON-NLS-1$
    public static final String EXTRAS_NOTIF_ID = "notifId"; //$NON-NLS-1$

    /** notification type extra */
    public static final String EXTRAS_TYPE = "type"; //$NON-NLS-1$
    public static final String EXTRAS_TITLE = "title"; //$NON-NLS-1$
    public static final String EXTRAS_TEXT = "text"; //$NON-NLS-1$
    public static final String EXTRAS_RING_TIMES = "ringTimes"; //$NON-NLS-1$

    // --- instance variables

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private ExceptionService exceptionService;

    public static NotificationManager notificationManager = null;
    private static boolean forceNotificationManager = false;

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
        int type = intent.getIntExtra(EXTRAS_TYPE, (byte) 0);

        Resources r = context.getResources();
        String reminder;

        if(type == ReminderService.TYPE_ALARM)
            reminder = getRandomReminder(r.getStringArray(R.array.reminders_alarm));
        else if(Preferences.getBoolean(R.string.p_rmd_nagging, true)) {
            if(type == ReminderService.TYPE_DUE || type == ReminderService.TYPE_OVERDUE)
                reminder = getRandomReminder(r.getStringArray(R.array.reminders_due));
            else if(type == ReminderService.TYPE_SNOOZE)
                reminder = getRandomReminder(r.getStringArray(R.array.reminders_snooze));
            else
                reminder = getRandomReminder(r.getStringArray(R.array.reminders));
        } else
            reminder = ""; //$NON-NLS-1$

        synchronized(Notifications.class) {
            if(notificationManager == null)
                notificationManager = new AndroidNotificationManager(context);
        }

        if(!showTaskNotification(id, type, reminder)) {
            notificationManager.cancel((int)id);
        }

        try {
            VoiceOutputService.getVoiceOutputInstance().onDestroy();
        } catch (VerifyError e) {
            // unavailable
        }
    }

    // --- notification creation

    /** @return a random reminder string */
    public static String getRandomReminder(String[] reminders) {
        int next = ReminderService.random.nextInt(reminders.length);
        String reminder = reminders[next];
        return reminder;
    }

    /**
     * Show a new notification about the given task. Returns false if there was
     * some sort of error or the alarm should be disabled.
     */
    public boolean showTaskNotification(long id, int type, String reminder) {
        Task task;
        try {
            task = taskDao.fetch(id, Task.ID, Task.TITLE, Task.HIDE_UNTIL, Task.COMPLETION_DATE,
                        Task.DUE_DATE, Task.DELETION_DATE, Task.REMINDER_FLAGS, Task.USER_ID);
            if(task == null)
                throw new IllegalArgumentException("cound not find item with id"); //$NON-NLS-1$

        } catch (Exception e) {
            exceptionService.reportError("show-notif", e); //$NON-NLS-1$
            return false;
        }

        if (!Preferences.getBoolean(R.string.p_rmd_enabled, true))
            return false;
        // you're done, or not yours - don't sound, do delete
        if(task.isCompleted() || task.isDeleted() || !Task.USER_ID_SELF.equals(task.getValue(Task.USER_ID)))
            return false;

        // new task edit in progress
        if(TextUtils.isEmpty(task.getValue(Task.TITLE)))
            return false;

        // it's hidden - don't sound, don't delete
        if(task.isHidden() && type == ReminderService.TYPE_RANDOM)
            return true;

        // task due date was changed, but alarm wasn't rescheduled
        boolean dueInFuture = task.hasDueTime() && task.getValue(Task.DUE_DATE) > DateUtilities.now() ||
            !task.hasDueTime() && task.getValue(Task.DUE_DATE) - DateUtilities.now() > DateUtilities.ONE_DAY;
        if((type == ReminderService.TYPE_DUE || type == ReminderService.TYPE_OVERDUE) &&
                (!task.hasDueDate() || dueInFuture))
            return true;

        // read properties
        String taskTitle = task.getValue(Task.TITLE);
        boolean nonstopMode = task.getFlag(Task.REMINDER_FLAGS, Task.NOTIFY_MODE_NONSTOP);
        boolean ringFiveMode = task.getFlag(Task.REMINDER_FLAGS, Task.NOTIFY_MODE_FIVE);
        int ringTimes = nonstopMode ? -1 : (ringFiveMode ? 5 : 1);

        // update last reminder time
        task.setValue(Task.REMINDER_LAST, DateUtilities.now());
        task.setValue(Task.SOCIAL_REMINDER, Task.REMINDER_SOCIAL_UNSEEN);
        taskDao.saveExisting(task);

        Context context = ContextManager.getContext();
        String title = context.getString(R.string.app_name);
        String text = reminder + " " + taskTitle; //$NON-NLS-1$

        Intent notifyIntent = new Intent(context, TaskListActivity.class);
        FilterWithCustomIntent itemFilter = new FilterWithCustomIntent(context.getString(R.string.rmd_NoA_filter),
                context.getString(R.string.rmd_NoA_filter),
                new QueryTemplate().where(TaskCriteria.byId(id)),
                null);
        Bundle customExtras = new Bundle();
        customExtras.putLong(NotificationFragment.TOKEN_ID, id);
        customExtras.putString(EXTRAS_TEXT, text);
        itemFilter.customExtras = customExtras;
        itemFilter.customTaskList = new ComponentName(context, NotificationFragment.class);

        notifyIntent.setAction("NOTIFY" + id); //$NON-NLS-1$
        notifyIntent.putExtra(TaskListFragment.TOKEN_FILTER, itemFilter);
        notifyIntent.putExtra(NotificationFragment.TOKEN_ID, id);
        notifyIntent.putExtra(EXTRAS_TEXT, text);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        notifyIntent.putExtra(TaskListActivity.TOKEN_SOURCE, Constants.SOURCE_NOTIFICATION);

        requestNotification((int)id, notifyIntent, type, title, text, ringTimes);
        return true;
    }

    private static void requestNotification(long taskId, Intent intent, int type, String title, String text, int ringTimes) {
        Context context = ContextManager.getContext();
        Intent inAppNotify = new Intent(BROADCAST_IN_APP_NOTIFY);
        inAppNotify.putExtra(EXTRAS_NOTIF_ID, (int)taskId);
        inAppNotify.putExtra(NotificationFragment.TOKEN_ID, taskId);
        inAppNotify.putExtra(EXTRAS_CUSTOM_INTENT, intent);
        inAppNotify.putExtra(EXTRAS_TYPE, type);
        inAppNotify.putExtra(EXTRAS_TITLE, title);
        inAppNotify.putExtra(EXTRAS_TEXT, text);
        inAppNotify.putExtra(EXTRAS_RING_TIMES, ringTimes);

        if(forceNotificationManager)
            new ShowNotificationReceiver().onReceive(ContextManager.getContext(), inAppNotify);
        else
            context.sendOrderedBroadcast(inAppNotify, AstridApiConstants.PERMISSION_READ);
    }

    /**
     * Receives requests to show an Astrid notification if they were not intercepted and handled
     * by the in-app reminders in AstridActivity.
     * @author Sam
     *
     */
    public static class ShowNotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int notificationId = intent.getIntExtra(EXTRAS_NOTIF_ID, 0);
            Intent customIntent = intent.getParcelableExtra(EXTRAS_CUSTOM_INTENT);
            int type = intent.getIntExtra(EXTRAS_TYPE, 0);
            String title = intent.getStringExtra(EXTRAS_TITLE);
            String text = intent.getStringExtra(EXTRAS_TEXT);
            int ringTimes = intent.getIntExtra(EXTRAS_RING_TIMES, 1);
            showNotification(notificationId, customIntent, type, title, text, ringTimes);
        }
    }

    private static long lastNotificationSound = 0L;

    /**
     * @returns true if notification should sound
     */
    private static boolean checkLastNotificationSound() {
        long now = DateUtilities.now();
        if (now - lastNotificationSound > 10000 || forceNotificationManager) {
            lastNotificationSound = now;
            return true;
        }
        return false;
    }

    /**
     * Shows an Astrid notification. Pulls in ring tone and quiet hour settings
     * from preferences. You can make it say anything you like.
     * @param ringTimes number of times to ring (-1 = nonstop)
     */
    public static void showNotification(int notificationId, Intent intent, int type, String title,
            String text, int ringTimes) {
        Context context = ContextManager.getContext();

        if(notificationManager == null)
            notificationManager = new AndroidNotificationManager(context);

        // don't ring multiple times if random reminder
        if(type == ReminderService.TYPE_RANDOM)
            ringTimes = 1;

        // quiet hours? unless alarm clock
        boolean quietHours = (type == ReminderService.TYPE_ALARM || type == ReminderService.TYPE_DUE) ? false : isQuietHours();

        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // set up properties (name and icon) for the notification
        int icon;
        switch(Preferences.getIntegerFromString(R.string.p_rmd_icon,
                ICON_SET_ASTRID)) {
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
        final Notification notification = new Notification(
                icon, text, System.currentTimeMillis());
        notification.setLatestEventInfo(context,
                title,
                text,
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

        // detect call state
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int callState = tm.getCallState();

        boolean voiceReminder = Preferences.getBoolean(R.string.p_voiceRemindersEnabled, false);

        // if multi-ring is activated and the setting p_rmd_maxvolume allows it, set up the flags for insistent
        // notification, and increase the volume to full volume, so the user
        // will actually pay attention to the alarm
        boolean maxOutVolumeForMultipleRingReminders = Preferences.getBoolean(R.string.p_rmd_maxvolume, true);
        // remember it to set it to the old value after the alarm
        int previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        if (ringTimes != 1 && (type != ReminderService.TYPE_RANDOM)) {
            notification.audioStreamType = AudioManager.STREAM_ALARM;
            if (maxOutVolumeForMultipleRingReminders) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
            }

            // insistent rings until notification is disabled
            if(ringTimes < 0) {
                notification.flags |= Notification.FLAG_INSISTENT;
                voiceReminder = false;
            }

        } else {
            notification.audioStreamType = AudioManager.STREAM_NOTIFICATION;
        }

        boolean soundIntervalOk = checkLastNotificationSound();

        // quiet hours = no sound
        if(quietHours || callState != TelephonyManager.CALL_STATE_IDLE) {
            notification.sound = null;
            voiceReminder = false;
        } else {
            String notificationPreference = Preferences.getStringValue(R.string.p_rmd_ringtone);
            if(audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) == 0) {
                notification.sound = null;
                voiceReminder = false;
            } else if(notificationPreference != null) {
                if(notificationPreference.length() > 0 && soundIntervalOk) {
                    Uri notificationSound = Uri.parse(notificationPreference);
                    notification.sound = notificationSound;
                } else {
                    notification.sound = null;
                }
            } else if (soundIntervalOk) {
                notification.defaults |= Notification.DEFAULT_SOUND;
            }
        }

        // quiet hours && ! due date or snooze = no vibrate
        if(quietHours && !(type == ReminderService.TYPE_DUE || type == ReminderService.TYPE_SNOOZE)) {
            notification.vibrate = null;
        } else if(callState != TelephonyManager.CALL_STATE_IDLE) {
            notification.vibrate = null;
        } else {
            if (Preferences.getBoolean(R.string.p_rmd_vibrate, true)
                    && audioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_NOTIFICATION) && soundIntervalOk) {
                notification.vibrate = new long[] {0, 1000, 500, 1000, 500, 1000};
            } else {
                notification.vibrate = null;
            }
        }

        if(Constants.DEBUG)
            Log.w("Astrid", "Logging notification: " + text); //$NON-NLS-1$ //$NON-NLS-2$

        singleThreadVoicePool.submit(new NotificationRunnable(ringTimes, notificationId, notification, voiceReminder,
                maxOutVolumeForMultipleRingReminders, audioManager, previousAlarmVolume, text));
        if (forceNotificationManager)
            try {
                singleThreadVoicePool.awaitTermination(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                //
            }
    }

    private static class NotificationRunnable implements Runnable {
        private final int ringTimes;
        private final int notificationId;
        private final Notification notification;
        private final boolean voiceReminder;
        private final boolean maxOutVolumeForMultipleRingReminders;
        private final AudioManager audioManager;
        private final int previousAlarmVolume;
        private final String text;

        public NotificationRunnable(int ringTimes, int notificationId, Notification notification, boolean voiceReminder,
                boolean maxOutVolume, AudioManager audioManager, int previousAlarmVolume, String text) {
            this.ringTimes = ringTimes;
            this.notificationId = notificationId;
            this.notification = notification;
            this.voiceReminder = voiceReminder;
            this.maxOutVolumeForMultipleRingReminders = maxOutVolume;
            this.audioManager = audioManager;
            this.previousAlarmVolume = previousAlarmVolume;
            this.text = text;
        }

        @Override
        public void run() {
            for(int i = 0; i < Math.max(ringTimes, 1); i++) {
                notificationManager.notify(notificationId, notification);
                AndroidUtilities.sleepDeep(500);
            }
            Flags.set(Flags.REFRESH); // Forces a reload when app launches
            if ((voiceReminder || maxOutVolumeForMultipleRingReminders)) {
                AndroidUtilities.sleepDeep(2000);
                for(int i = 0; i < 50; i++) {
                    AndroidUtilities.sleepDeep(500);
                    if(audioManager.getMode() != AudioManager.MODE_RINGTONE)
                        break;
                }
                try {
                    // first reset the Alarm-volume to the value before it was eventually maxed out
                    if (maxOutVolumeForMultipleRingReminders)
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, 0);
                    if (voiceReminder)
                        VoiceOutputService.getVoiceOutputInstance().queueSpeak(text);
                } catch (VerifyError e) {
                    // unavailable
                }
            }
        }
    }

    private static ExecutorService singleThreadVoicePool = Executors.newSingleThreadExecutor();

    /**
     * @return whether we're in quiet hours
     */
    public static boolean isQuietHours() {
        int quietHoursStart = Preferences.getIntegerFromString(R.string.p_rmd_quietStart, -1);
        int quietHoursEnd = Preferences.getIntegerFromString(R.string.p_rmd_quietEnd, -1);
        if(quietHoursStart != -1 && quietHoursEnd != -1) {
            int hour = new Date().getHours();
            if(quietHoursStart <= quietHoursEnd) {
                if(hour >= quietHoursStart && hour < quietHoursEnd)
                    return true;
            } else { // wrap across 24/hour boundary
                if(hour >= quietHoursStart || hour < quietHoursEnd)
                    return true;
            }
        }
        return false;
    }

    /**
     * Schedules alarms for a single task
     *
     * @param shouldPerformPropertyCheck
     *            whether to check if task has requisite properties
     */
    public static void cancelNotifications(long taskId) {
        if(notificationManager == null)
            synchronized(Notifications.class) {
                if(notificationManager == null)
                    notificationManager = new AndroidNotificationManager(
                            ContextManager.getContext());
            }

        notificationManager.cancel((int)taskId);
    }

    // --- notification manager

    public static void setNotificationManager(
            NotificationManager notificationManager) {
        Notifications.notificationManager = notificationManager;
    }

    public static void forceNotificationManager(boolean status) {
        forceNotificationManager = status;
    }

}
