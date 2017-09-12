package org.tasks.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.intents.TaskIntents;
import org.tasks.preferences.Preferences;
import org.tasks.ui.CheckBoxes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastOreo;

@ApplicationScope
public class NotificationManager {

    public static final String NOTIFICATION_CHANNEL_DEFAULT = "notifications";
    public static final String NOTIFICATION_CHANNEL_TASKER = "notifications_tasker";
    public static final String NOTIFICATION_CHANNEL_CALLS = "notifications_calls";
    public static final String NOTIFICATION_CHANNEL_TIMERS = "notifications_timers";
    public static final String GROUP_KEY = "tasks";
    private static final int SUMMARY_NOTIFICATION_ID = 0;
    static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";

    private final android.app.NotificationManager notificationManager;
    private final NotificationDao notificationDao;
    private final TaskDao taskDao;
    private final Context context;
    private final Preferences preferences;
    private final CheckBoxes checkBoxes;

    @Inject
    public NotificationManager(@ForApplication Context context, Preferences preferences,
                               NotificationDao notificationDao, TaskDao taskDao, CheckBoxes checkBoxes) {
        this.context = context;
        this.preferences = preferences;
        this.notificationDao = notificationDao;
        this.taskDao = taskDao;
        this.checkBoxes = checkBoxes;
        notificationManager = (android.app.NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (atLeastOreo()) {
            notificationManager.createNotificationChannel(createNotificationChannel(NOTIFICATION_CHANNEL_DEFAULT, R.string.notifications));
            notificationManager.createNotificationChannel(createNotificationChannel(NOTIFICATION_CHANNEL_CALLS, R.string.missed_calls));
            notificationManager.createNotificationChannel(createNotificationChannel(NOTIFICATION_CHANNEL_TASKER, R.string.tasker_locale));
            notificationManager.createNotificationChannel(createNotificationChannel(NOTIFICATION_CHANNEL_TIMERS, R.string.TEA_timer_controls));
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
            notificationDao.delete(id);
            updateSummary(false, false, false);
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    public void notifyTasks(Map<org.tasks.notifications.Notification, NotificationCompat.Builder> notifications, boolean alert, boolean nonstop, boolean fiveTimes) {
        notificationDao.insertAll(newArrayList(notifications.keySet()));
        updateSummary(alert && notifications.size() > 1, nonstop, fiveTimes);
        ArrayList<Map.Entry<org.tasks.notifications.Notification, NotificationCompat.Builder>> entries = newArrayList(notifications.entrySet());

        int last = entries.size() - 1;
        for (int i = 0; i <= last; i++) {
            Map.Entry<org.tasks.notifications.Notification, NotificationCompat.Builder> entry = entries.get(i);
            long taskId = entry.getKey().taskId;
            Task task = taskDao.fetch(taskId);
            NotificationCompat.Builder builder = entry.getValue();
            builder.setColor(checkBoxes.getPriorityColor(task.getImportance()));
            if (i < last) {
                notify(taskId, builder, false, false, false);
            } else {
                notify(taskId, builder, alert, nonstop, fiveTimes);
            }
        }
    }

    public void notify(long notificationId, NotificationCompat.Builder builder, boolean alert, boolean nonstop, boolean fiveTimes) {
        Notification notification = builder.build();
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
        if (preferences.bundleNotifications()) {
            int taskCount = notificationDao.count();
            if (taskCount == 0) {
                notificationManager.cancel(SUMMARY_NOTIFICATION_ID);
            } else {
                List<org.tasks.notifications.Notification> notifications = notificationDao.getAllOrdered();
                Iterable<Long> notificationIds = transform(notifications, n -> n.taskId);
                QueryTemplate query = new QueryTemplate().where(Task.ID.in(notificationIds));
                Filter filter = new Filter(context.getString(R.string.notifications), query);
                List<Task> tasks = taskDao.toList(Query.select(Task.PROPERTIES)
                        .withQueryTemplate(query.toString()));
                long when = notificationDao.latestTimestamp();
                int maxPriority = 3;
                String summaryTitle = context.getString(R.string.task_count, taskCount);
                NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle()
                        .setBigContentTitle(summaryTitle);
                for (org.tasks.notifications.Notification notification : notifications) {
                    Task task = tryFind(tasks, t -> t.getId() == notification.taskId).orNull();
                    if (task == null) {
                        continue;
                    }
                    style.addLine(task.getTitle());
                    maxPriority = Math.min(maxPriority, task.getImportance());
                }
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationManager.NOTIFICATION_CHANNEL_DEFAULT)
                        .setContentTitle(summaryTitle)
                        .setContentText(context.getString(R.string.app_name))
                        .setGroupSummary(true)
                        .setGroup(GROUP_KEY)
                        .setShowWhen(true)
                        .setWhen(when)
                        .setSmallIcon(R.drawable.ic_done_all_white_24dp)
                        .setStyle(style)
                        .setColor(checkBoxes.getPriorityColor(maxPriority))
                        .setNumber(taskCount)
                        .setContentIntent(PendingIntent.getActivity(context, 0, TaskIntents.getTaskListIntent(context, filter), PendingIntent.FLAG_UPDATE_CURRENT));
                if (notify) {
                    builder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setSound(preferences.getRingtone());
                } else {
                    builder.setOnlyAlertOnce(true)
                            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN);
                }

                notify(NotificationManager.SUMMARY_NOTIFICATION_ID, builder, notify, nonStop, fiveTimes);
            }
        } else {
            notificationManager.cancel(NotificationManager.SUMMARY_NOTIFICATION_ID);
        }
    }

}
