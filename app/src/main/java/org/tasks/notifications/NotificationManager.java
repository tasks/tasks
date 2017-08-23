package org.tasks.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import org.tasks.R;
import org.tasks.db.AppDatabase;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastNougat;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastOreo;

@ApplicationScope
public class NotificationManager {

    public static final String NOTIFICATION_CHANNEL_DEFAULT = "notifications";
    public static final String NOTIFICATION_CHANNEL_TASKER = "notifications_tasker";
    public static final String NOTIFICATION_CHANNEL_CALLS = "notifications_calls";
    public static final String GROUP_KEY = "tasks";
    private static final int SUMMARY_NOTIFICATION_ID = 0;
    static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";

    private final android.app.NotificationManager notificationManager;
    private final AppDatabase appDatabase;
    private final Context context;
    private final Preferences preferences;

    @Inject
    public NotificationManager(@ForApplication Context context, Preferences preferences,
                               AppDatabase appDatabase) {
        this.context = context;
        this.preferences = preferences;
        notificationManager = (android.app.NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.appDatabase = appDatabase;
        if (atLeastOreo()) {
            notificationManager.createNotificationChannel(createNotificationChannel(NOTIFICATION_CHANNEL_DEFAULT, R.string.notifications));
            notificationManager.createNotificationChannel(createNotificationChannel(NOTIFICATION_CHANNEL_CALLS, R.string.missed_calls));
            notificationManager.createNotificationChannel(createNotificationChannel(NOTIFICATION_CHANNEL_TASKER, R.string.tasker_locale));
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private NotificationChannel createNotificationChannel(String channelId, int nameResId) {
        String channelName = context.getString(nameResId);
        NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, android.app.NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);
        notificationChannel.setBypassDnd(true);
        notificationChannel.setShowBadge(true);
        notificationChannel.setImportance(android.app.NotificationManager.IMPORTANCE_HIGH);
        return notificationChannel;
    }

    public void cancel(long id) {
        notificationManager.cancel((int) id);
        Completable.fromAction(() -> {
            appDatabase.notificationDao().delete(id);
            updateSummary(false, false, false);
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    public void notifyTasks(Map<org.tasks.notifications.Notification, Notification> notifications, boolean alert, boolean nonstop, boolean fiveTimes) {
        appDatabase.notificationDao().insertAll(newArrayList(notifications.keySet()));
        updateSummary(alert && notifications.size() > 1, nonstop, fiveTimes);
        ArrayList<Map.Entry<org.tasks.notifications.Notification, Notification>> entries = newArrayList(notifications.entrySet());

        int last = entries.size() - 1;
        for (int i = 0; i < last; i++) {
            Map.Entry<org.tasks.notifications.Notification, Notification> entry = entries.get(i);
            notify(entry.getKey().taskId, entry.getValue(), false, false, false);
        }
        Map.Entry<org.tasks.notifications.Notification, Notification> entry = entries.get(last);
        notify(entry.getKey().taskId, entry.getValue(), alert, nonstop, fiveTimes);
    }

    public void notify(long notificationId, Notification notification, boolean alert, boolean nonstop, boolean fiveTimes) {
        if (preferences.getBoolean(R.string.p_rmd_enabled, true)) {
            int ringTimes = 1;
            if (preferences.getBoolean(R.string.p_rmd_persistent, true)) {
                notification.flags |= Notification.FLAG_NO_CLEAR;
            }
            if (preferences.isLEDNotificationEnabled()) {
                notification.defaults |= Notification.DEFAULT_LIGHTS;
            }
            if (alert) {
                if (nonstop) {
                    notification.flags |= Notification.FLAG_INSISTENT;
                    ringTimes = 1;
                } else if (fiveTimes) {
                    ringTimes = 5;
                }
                if (preferences.isVibrationEnabled()) {
                    notification.defaults |= Notification.DEFAULT_VIBRATE;
                }
                notification.sound = preferences.getRingtone();
                notification.audioStreamType = Notification.STREAM_DEFAULT;
            }
            Intent deleteIntent = new Intent(context, NotificationClearedReceiver.class);
            deleteIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
            notification.deleteIntent = PendingIntent.getBroadcast(context, (int) notificationId, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            for (int i = 0 ; i < ringTimes ; i++) {
                notificationManager.notify((int) notificationId, notification);
            }
        }
    }

    private void updateSummary(boolean notify, boolean nonStop, boolean fiveTimes) {
        if (atLeastNougat()) {
            if (appDatabase.notificationDao().count() == 0) {
                notificationManager.cancel(SUMMARY_NOTIFICATION_ID);
            } else {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationManager.NOTIFICATION_CHANNEL_DEFAULT)
                        .setGroupSummary(true)
                        .setGroup(GROUP_KEY)
                        .setShowWhen(false)
                        .setSmallIcon(R.drawable.ic_done_all_white_24dp);

                if (notify) {
                    builder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setSound(preferences.getRingtone());

                } else {
                    builder.setOnlyAlertOnce(true)
                            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN);
                }

                notify(NotificationManager.SUMMARY_NOTIFICATION_ID, builder.build(), notify, nonStop, fiveTimes);
            }
        }
    }
}
