package org.tasks;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.voice.VoiceOutputAssistant;

import org.tasks.injection.ForApplication;
import org.tasks.notifications.AudioManager;
import org.tasks.notifications.NotificationManager;
import org.tasks.notifications.TelephonyManager;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.CompleteTaskReceiver;
import org.tasks.reminders.MissedCallActivity;
import org.tasks.reminders.NotificationActivity;
import org.tasks.reminders.SnoozeActivity;
import org.tasks.reminders.SnoozeDialog;
import org.tasks.reminders.SnoozeOption;

import java.io.InputStream;

import javax.inject.Inject;

import timber.log.Timber;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybean;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

public class Notifier {

    private static long lastNotificationSound = 0L;

    private final Context context;
    private final TaskDao taskDao;
    private final NotificationManager notificationManager;
    private final TaskService taskService;
    private final TelephonyManager telephonyManager;
    private final AudioManager audioManager;
    private final VoiceOutputAssistant voiceOutputAssistant;
    private final Preferences preferences;

    @Inject
    public Notifier(@ForApplication Context context, TaskDao taskDao,
                    NotificationManager notificationManager, TaskService taskService,
                    TelephonyManager telephonyManager, AudioManager audioManager,
                    VoiceOutputAssistant voiceOutputAssistant, Preferences preferences) {
        this.context = context;
        this.taskDao = taskDao;
        this.notificationManager = notificationManager;
        this.taskService = taskService;
        this.telephonyManager = telephonyManager;
        this.audioManager = audioManager;
        this.voiceOutputAssistant = voiceOutputAssistant;
        this.preferences = preferences;
    }

    public void triggerMissedCallNotification(final String name, final String number, long contactId) {
        final String title = context.getString(R.string.missed_call, TextUtils.isEmpty(name) ? number : name);

        Intent missedCallDialog = new Intent(context, MissedCallActivity.class) {{
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            putExtra(MissedCallActivity.EXTRA_NUMBER, number);
            putExtra(MissedCallActivity.EXTRA_NAME, name);
            putExtra(MissedCallActivity.EXTRA_TITLE, title);
        }};

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_check_white_24dp)
                .setTicker(title)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.app_name))
                .setWhen(currentTimeMillis())
                .setContentIntent(PendingIntent.getActivity(context, missedCallDialog.hashCode(), missedCallDialog, PendingIntent.FLAG_UPDATE_CURRENT));

        Bitmap contactImage = getContactImage(contactId);
        if (contactImage != null) {
            builder.setLargeIcon(contactImage);
        }

        if (preferences.useNotificationActions()) {
            Intent callNow = new Intent(context, MissedCallActivity.class) {{
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                putExtra(MissedCallActivity.EXTRA_NUMBER, number);
                putExtra(MissedCallActivity.EXTRA_NAME, name);
                putExtra(MissedCallActivity.EXTRA_TITLE, title);
                putExtra(MissedCallActivity.EXTRA_CALL_NOW, true);
            }};
            Intent callLater = new Intent(context, MissedCallActivity.class) {{
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                putExtra(MissedCallActivity.EXTRA_NUMBER, number);
                putExtra(MissedCallActivity.EXTRA_NAME, name);
                putExtra(MissedCallActivity.EXTRA_TITLE, title);
                putExtra(MissedCallActivity.EXTRA_CALL_LATER, true);
            }};
            builder
                    .addAction(R.drawable.ic_phone_white_24dp, context.getString(R.string.MCA_return_call), PendingIntent.getActivity(context, callNow.hashCode(), callNow, PendingIntent.FLAG_UPDATE_CURRENT))
                    .addAction(R.drawable.ic_add_white_24dp, context.getString(R.string.MCA_add_task), PendingIntent.getActivity(context, callLater.hashCode(), callLater, PendingIntent.FLAG_UPDATE_CURRENT));
        }

        activateNotification(1, number.hashCode(), builder.build(), null);
    }

    private Bitmap getContactImage(long contactId) {
        Bitmap b = null;
        if (contactId >= 0) {
            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
            InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);
            try {
                b = BitmapFactory.decodeStream(input);
            } catch (OutOfMemoryError e) {
                Timber.e(e, e.getMessage());
            }
        }
        return b;
    }

    @Deprecated
    public void triggerFilterNotification(final String title, final String query, final String valuesForNewTasks) {
        triggerFilterNotification(new Filter(title, query, AndroidUtilities.contentValuesFromSerializedString(valuesForNewTasks)));
    }

    public void triggerFilterNotification(final Filter filter) {
        String title = filter.listingTitle;
        String query = filter.getSqlQuery();
        TodorooCursor<Task> taskTodorooCursor = null;
        int count;
        try {
            taskTodorooCursor = taskService.fetchFiltered(query, null, Task.ID);
            if (taskTodorooCursor == null) {
                return;
            }
            count = taskTodorooCursor.getCount();
            if (count == 0) {
                return;
            }
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
            return;
        } finally {
            if (taskTodorooCursor != null) {
                taskTodorooCursor.close();
            }
        }

        String subtitle = context.getString(R.string.task_count, count);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, (title + query).hashCode(), new Intent(context, TaskListActivity.class) {{
            setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK);
            putExtra(TaskListActivity.OPEN_FILTER, filter);
        }}, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_check_white_24dp)
                .setTicker(title)
                .setWhen(currentTimeMillis())
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        activateNotification(1, (title + query).hashCode(), notification, null);
    }

    public void triggerTaskNotification(long id, int type) {
        if (!showNotification(id, type)) {
            notificationManager.cancel(id);
        }
    }

    private boolean showNotification(final long id, final int type) {
        Task task;
        try {
            task = taskDao.fetch(id, Task.ID, Task.TITLE, Task.HIDE_UNTIL, Task.COMPLETION_DATE,
                    Task.DUE_DATE, Task.DELETION_DATE, Task.REMINDER_FLAGS);
            if (task == null) {
                throw new IllegalArgumentException("cound not find item with id"); //$NON-NLS-1$
            }

        } catch (Exception e) {
            Timber.e(e, e.getMessage());
            return false;
        }

        // you're done, or not yours - don't sound, do delete
        if (task.isCompleted() || task.isDeleted()) {
            return false;
        }

        // new task edit in progress
        if (TextUtils.isEmpty(task.getTitle())) {
            return false;
        }

        // it's hidden - don't sound, don't delete
        if (task.isHidden() && type == ReminderService.TYPE_RANDOM) {
            return false;
        }

        // task due date was changed, but alarm wasn't rescheduled
        boolean dueInFuture = task.hasDueTime() && task.getDueDate() > DateUtilities.now() ||
                !task.hasDueTime() && task.getDueDate() - DateUtilities.now() > DateUtilities.ONE_DAY;
        if ((type == ReminderService.TYPE_DUE || type == ReminderService.TYPE_OVERDUE) &&
                (!task.hasDueDate() || dueInFuture)) {
            return false;
        }

        // read properties
        final String taskTitle = task.getTitle();
        boolean nonstopMode = task.isNotifyModeNonstop();
        boolean ringFiveMode = task.isNotifyModeFive();
        int ringTimes = nonstopMode ? -1 : (ringFiveMode ? 5 : 1);

        // update last reminder time
        task.setReminderLast(DateUtilities.now());
        taskDao.saveExisting(task);

        final String text = context.getString(R.string.app_name);

        final Intent intent = new Intent(context, NotificationActivity.class) {{
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            setAction("NOTIFY" + id); //$NON-NLS-1$
            putExtra(NotificationActivity.EXTRA_TASK_ID, id);
            putExtra(NotificationActivity.EXTRA_TITLE, taskTitle);
        }};

        // don't ring multiple times if random reminder
        if (type == ReminderService.TYPE_RANDOM) {
            ringTimes = 1;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_check_white_24dp)
                .setTicker(taskTitle)
                .setWhen(currentTimeMillis())
                .setContentTitle(taskTitle)
                .setContentText(text)
                .setContentIntent(PendingIntent.getActivity(context, (int) id, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        if (preferences.useNotificationActions()) {
            PendingIntent completeIntent = PendingIntent.getBroadcast(context, (int) id, new Intent(context, CompleteTaskReceiver.class) {{
                putExtra(CompleteTaskReceiver.TASK_ID, id);
            }}, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Action completeAction = new NotificationCompat.Action.Builder(
                    R.drawable.ic_check_white_24dp, context.getResources().getString(R.string.rmd_NoA_done), completeIntent).build();

            PendingIntent snoozePendingIntent = PendingIntent.getActivity(context, (int) id, new Intent(context, SnoozeActivity.class) {{
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                putExtra(SnoozeActivity.EXTRA_TASK_ID, id);
            }}, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();
            wearableExtender.addAction(completeAction);
            for (final SnoozeOption snoozeOption : SnoozeDialog.getSnoozeOptions(preferences)) {
                final long timestamp = snoozeOption.getDateTime().getMillis();
                PendingIntent snoozeIntent = PendingIntent.getActivity(context, (int) id, new Intent(context, SnoozeActivity.class) {{
                    setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    setAction(String.format("snooze-%s-%s", id, timestamp));
                    putExtra(SnoozeActivity.EXTRA_TASK_ID, id);
                    putExtra(SnoozeActivity.EXTRA_SNOOZE_TIME, timestamp);
                }}, PendingIntent.FLAG_UPDATE_CURRENT);
                wearableExtender.addAction(new NotificationCompat.Action.Builder(
                        R.drawable.ic_snooze_white_24dp, context.getString(snoozeOption.getResId()), snoozeIntent)
                        .build());
            }

            builder.addAction(completeAction)
                    .addAction(R.drawable.ic_snooze_white_24dp, context.getResources().getString(R.string.rmd_NoA_snooze), snoozePendingIntent)
                    .extend(wearableExtender);
        }

        activateNotification(ringTimes, (int) id, builder.build(), taskTitle);

        return true;
    }

    private void activateNotification(int ringTimes, int notificationId, Notification notification, String text) {
        if (preferences.getBoolean(R.string.p_rmd_persistent, true)) {
            notification.flags |= Notification.FLAG_NO_CLEAR;
        }
        if (preferences.getBoolean(R.string.p_disable_notification_light, false)) {
            notification.ledOffMS = 0;
            notification.ledOnMS = 0;
        } else {
            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
            notification.ledOffMS = 5000;
            notification.ledOnMS = 700;
            notification.ledARGB = Color.YELLOW;
        }

        if (atLeastJellybean()) {
            switch (preferences.getNotificationPriority()) {
                case 0:
                    notification.priority = NotificationCompat.PRIORITY_DEFAULT;
                    break;
                case -1:
                    notification.priority = NotificationCompat.PRIORITY_LOW;
                    break;
                default:
                    notification.priority = NotificationCompat.PRIORITY_HIGH;
                    break;
            }
        }

        boolean voiceReminder = preferences.getBoolean(R.string.p_voiceRemindersEnabled, false) && !isNullOrEmpty(text);

        // if multi-ring is activated and the setting p_rmd_maxvolume allows it, set up the flags for insistent
        // notification, and increase the volume to full volume, so the user
        // will actually pay attention to the alarm
        boolean maxOutVolumeForMultipleRingReminders = preferences.getBoolean(R.string.p_rmd_maxvolume, true);
        // remember it to set it to the old value after the alarm
        int previousAlarmVolume = audioManager.getAlarmVolume();
        if (ringTimes != 1) {
            notification.audioStreamType = android.media.AudioManager.STREAM_ALARM;
            if (maxOutVolumeForMultipleRingReminders) {
                audioManager.setMaxAlarmVolume();
            }

            // insistent rings until notification is disabled
            if (ringTimes < 0) {
                notification.flags |= Notification.FLAG_INSISTENT;
                voiceReminder = false;
            }

        } else {
            notification.audioStreamType = android.media.AudioManager.STREAM_NOTIFICATION;
        }

        boolean soundIntervalOk = checkLastNotificationSound();

        if (telephonyManager.callStateIdle()) {
            String notificationPreference = preferences.getStringValue(R.string.p_rmd_ringtone);
            if (audioManager.notificationsMuted()) {
                notification.sound = null;
                voiceReminder = false;
            } else if (notificationPreference != null) {
                if (notificationPreference.length() > 0 && soundIntervalOk) {
                    notification.sound = Uri.parse(notificationPreference);
                } else {
                    notification.sound = null;
                }
            } else if (soundIntervalOk) {
                notification.defaults |= Notification.DEFAULT_SOUND;
            }
        }

        if (preferences.getBoolean(R.string.p_rmd_vibrate, true) && soundIntervalOk) {
            notification.vibrate = preferences.getVibrationPattern();
        } else {
            notification.vibrate = null;
        }

        if (!telephonyManager.callStateIdle()) {
            notification.sound = null;
            notification.vibrate = null;
            voiceReminder = false;
        }

        for (int i = 0; i < Math.max(ringTimes, 1); i++) {
            notificationManager.notify(notificationId, notification);
            AndroidUtilities.sleepDeep(500);
        }
        if (voiceReminder || maxOutVolumeForMultipleRingReminders) {
            AndroidUtilities.sleepDeep(2000);
            for (int i = 0; i < 50; i++) {
                AndroidUtilities.sleepDeep(500);
                if (!audioManager.isRingtoneMode()) {
                    break;
                }
            }
            try {
                // first reset the Alarm-volume to the value before it was eventually maxed out
                if (maxOutVolumeForMultipleRingReminders) {
                    audioManager.setAlarmVolume(previousAlarmVolume);
                }
                if (voiceReminder) {
                    voiceOutputAssistant.speak(text);
                }
            } catch (VerifyError e) {
                // unavailable
                Timber.e(e, e.getMessage());
            }
        }
    }

    /**
     * @return true if notification should sound
     */
    private static boolean checkLastNotificationSound() {
        long now = DateUtilities.now();
        if (now - lastNotificationSound > 10000) {
            lastNotificationSound = now;
            return true;
        }
        return false;
    }
}
