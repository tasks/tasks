package com.todoroo.astrid.reminders;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.voice.VoiceOutputAssistant;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingIntentService;
import org.tasks.notifications.AudioManager;
import org.tasks.notifications.NotificationManager;
import org.tasks.notifications.TelephonyManager;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.CompleteTaskReceiver;
import org.tasks.reminders.SnoozeActivity;

import javax.inject.Inject;

import static org.tasks.date.DateTimeUtils.currentTimeMillis;

/**
 * Receives requests to show an Astrid notification if they were not intercepted and handled
 * by the in-app reminders in AstridActivity.
 *
 * @author Sam
 */
public class TaskNotificationIntentService extends InjectingIntentService {

    private static final Logger log = LoggerFactory.getLogger(TaskNotificationIntentService.class);

    private static long lastNotificationSound = 0L;

    /**
     * Action name for broadcast intent notifying that task was created from repeating template
     */
    public static final String EXTRAS_CUSTOM_INTENT = "intent"; //$NON-NLS-1$
    public static final String EXTRAS_NOTIF_ID = "notifId"; //$NON-NLS-1$

    /**
     * notification type extra
     */
    public static final String EXTRAS_TITLE = "title"; //$NON-NLS-1$
    public static final String EXTRAS_TEXT = "text"; //$NON-NLS-1$
    public static final String EXTRAS_RING_TIMES = "ringTimes"; //$NON-NLS-1$
    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRAS_TYPE = "type"; //$NON-NLS-1$

    @Inject NotificationManager notificationManager;
    @Inject TelephonyManager telephonyManager;
    @Inject Preferences preferences;
    @Inject VoiceOutputAssistant voiceOutputAssistant;
    @Inject AudioManager audioManager;
    @Inject @ForApplication Context context;

    public TaskNotificationIntentService() {
        super(TaskNotificationIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        showNotification(
                context,
                intent.getIntExtra(EXTRAS_NOTIF_ID, 0),
                intent.getLongExtra(EXTRA_TASK_ID, 0L),
                intent.<PendingIntent>getParcelableExtra(EXTRAS_CUSTOM_INTENT),
                intent.getIntExtra(EXTRAS_TYPE, 0),
                intent.getStringExtra(EXTRAS_TITLE),
                intent.getStringExtra(EXTRAS_TEXT),
                intent.getIntExtra(EXTRAS_RING_TIMES, 1));
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

    /**
     * @return whether we're in quiet hours
     */
    static boolean isQuietHours(Preferences preferences) {
        boolean quietHoursEnabled = preferences.quietHoursEnabled();
        if (quietHoursEnabled) {
            long quietHoursStart = new DateTime().withMillisOfDay(preferences.getInt(R.string.p_rmd_quietStart)).getMillis();
            long quietHoursEnd = new DateTime().withMillisOfDay(preferences.getInt(R.string.p_rmd_quietEnd)).getMillis();
            long now = currentTimeMillis();
            if (quietHoursStart <= quietHoursEnd) {
                if (now >= quietHoursStart && now < quietHoursEnd) {
                    return true;
                }
            } else { // wrap across 24/hour boundary
                if (now >= quietHoursStart || now < quietHoursEnd) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Shows an Astrid notification. Pulls in ring tone and quiet hour settings
     * from preferences. You can make it say anything you like.
     *
     * @param ringTimes number of times to ring (-1 = nonstop)
     */
    private void showNotification(Context context, int notificationId, final long taskId, final PendingIntent pendingIntent, int type, String title,
                                  String text, int ringTimes) {
        // don't ring multiple times if random reminder
        if (type == ReminderService.TYPE_RANDOM) {
            ringTimes = 1;
        }

        // quiet hours? unless alarm clock
        boolean quietHours = isQuietHours(preferences);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.notif_astrid)
                .setTicker(title)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent);
        if (preferences.useNotificationActions()) {
            PendingIntent completeIntent = PendingIntent.getBroadcast(context, notificationId, new Intent(context, CompleteTaskReceiver.class) {{
                putExtra(CompleteTaskReceiver.TASK_ID, taskId);
            }}, PendingIntent.FLAG_UPDATE_CURRENT);

            PendingIntent snoozePendingIntent = PendingIntent.getActivity(context, notificationId, new Intent(context, SnoozeActivity.class) {{
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                putExtra(SnoozeActivity.EXTRA_TASK_ID, taskId);
            }}, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.addAction(R.drawable.ic_check_white_24dp, context.getResources().getString(R.string.rmd_NoA_done), completeIntent)
                    .addAction(R.drawable.ic_snooze_white_24dp, context.getResources().getString(R.string.rmd_NoA_snooze), snoozePendingIntent);
        }

        Notification notification = builder.build();
        if (preferences.getBoolean(R.string.p_rmd_persistent, true)) {
            notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_SHOW_LIGHTS;
            notification.ledOffMS = 5000;
            notification.ledOnMS = 700;
            notification.ledARGB = Color.YELLOW;
        } else {
            notification.defaults = Notification.DEFAULT_LIGHTS;
        }

        boolean voiceReminder = preferences.getBoolean(R.string.p_voiceRemindersEnabled, false);

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

        if (!quietHours && telephonyManager.callStateIdle()) {
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
            notification.vibrate = new long[]{0, 1000, 500, 1000, 500, 1000};
        } else {
            notification.vibrate = null;
        }

        if (quietHours || !telephonyManager.callStateIdle()) {
            notification.sound = null;
            notification.vibrate = null;
            voiceReminder = false;
        }

        for (int i = 0; i < Math.max(ringTimes, 1); i++) {
            notificationManager.notify(notificationId, notification);
            AndroidUtilities.sleepDeep(500);
        }
        Flags.set(Flags.REFRESH); // Forces a reload when app launches
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
                log.error(e.getMessage(), e);
            }
        }
    }
}
