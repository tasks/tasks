package org.tasks.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;

import com.google.common.base.Strings;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.ReminderService;

import org.tasks.R;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.intents.TaskIntents;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.CompleteTaskReceiver;
import org.tasks.reminders.NotificationActivity;
import org.tasks.reminders.SnoozeActivity;
import org.tasks.reminders.SnoozeDialog;
import org.tasks.reminders.SnoozeOption;
import org.tasks.ui.CheckBoxes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybean;
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

    private final NotificationManagerCompat notificationManagerCompat;
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
        notificationManagerCompat = NotificationManagerCompat.from(context);
        if (atLeastOreo()) {
            android.app.NotificationManager notificationManager = (android.app.NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
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
        notificationManagerCompat.cancel((int) id);
        Completable.fromAction(() -> {
            if (id == SUMMARY_NOTIFICATION_ID) {
                List<Long> tasks = transform(notificationDao.getAll(), n -> n.taskId);
                for (Long task : tasks) {
                    notificationManagerCompat.cancel(task.intValue());
                }
                notificationDao.deleteAll(tasks);
            } else if (notificationDao.delete(id) > 0) {
                if (notificationDao.count() == 1) {
                    List<org.tasks.notifications.Notification> notifications = notificationDao.getAll();
                    org.tasks.notifications.Notification notification = notifications.get(0);
                    NotificationCompat.Builder builder = getTaskNotification(notification)
                            .setGroup(null)
                            .setOnlyAlertOnce(true)
                            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY);
                    notify(notification.taskId, builder, false, false, false);
                }
                updateSummary(false, false, false);
            }
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    public void notifyTasks(Map<org.tasks.notifications.Notification, NotificationCompat.Builder> notifications, boolean alert, boolean nonstop, boolean fiveTimes) {
        List<org.tasks.notifications.Notification> existing = notificationDao.getAll();
        if (existing.size() == 1) {
            org.tasks.notifications.Notification notification = existing.get(0);
            NotificationCompat.Builder builder = getTaskNotification(notification)
                    .setGroup(GROUP_KEY)
                    .setOnlyAlertOnce(true)
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY);
            notify(notification.taskId, builder, false, false, false);
        }
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
                notificationManagerCompat.notify((int) notificationId, notification);
            }
        }
    }

    private void updateSummary(boolean notify, boolean nonStop, boolean fiveTimes) {
        if (preferences.bundleNotifications()) {
            int taskCount = notificationDao.count();
            if (taskCount <= 1) {
                notificationManagerCompat.cancel(SUMMARY_NOTIFICATION_ID);
            } else {
                List<org.tasks.notifications.Notification> notifications = notificationDao.getAllOrdered();
                List<Long> notificationIds = transform(notifications, n -> n.taskId);
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
            notificationManagerCompat.cancel(NotificationManager.SUMMARY_NOTIFICATION_ID);
        }
    }

    public NotificationCompat.Builder getTaskNotification(org.tasks.notifications.Notification notification) {
        long id = notification.taskId;
        int type = notification.type;
        long when = notification.timestamp;
        Task task = taskDao.fetch(id);
        if (task == null) {
            Timber.e("Could not find %s", id);
            return null;
        }

        // you're done, or not yours - don't sound, do delete
        if (task.isCompleted() || task.isDeleted()) {
            return null;
        }

        // new task edit in progress
        if (TextUtils.isEmpty(task.getTitle())) {
            return null;
        }

        // it's hidden - don't sound, don't delete
        if (task.isHidden() && type == ReminderService.TYPE_RANDOM) {
            return null;
        }

        // task due date was changed, but alarm wasn't rescheduled
        boolean dueInFuture = task.hasDueTime() && task.getDueDate() > DateUtilities.now() ||
                !task.hasDueTime() && task.getDueDate() - DateUtilities.now() > DateUtilities.ONE_DAY;
        if ((type == ReminderService.TYPE_DUE || type == ReminderService.TYPE_OVERDUE) &&
                (!task.hasDueDate() || dueInFuture)) {
            return null;
        }

        // read properties
        final String taskTitle = task.getTitle();
        final String taskDescription = task.getNotes();

        // update last reminder time
        task.setReminderLast(when);
        taskDao.saveExisting(task);

        final String appName = context.getString(R.string.app_name);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationManager.NOTIFICATION_CHANNEL_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setTicker(taskTitle)
                .setContentTitle(taskTitle)
                .setContentText(appName)
                .setGroup(GROUP_KEY)
                .setSmallIcon(R.drawable.ic_check_white_24dp)
                .setWhen(when)
                .setShowWhen(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (atLeastJellybean()) {
            builder.setContentIntent(PendingIntent.getActivity(context, (int) id, TaskIntents.getEditTaskIntent(context, null, id), PendingIntent.FLAG_UPDATE_CURRENT));
        } else {
            final Intent intent = new Intent(context, NotificationActivity.class);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK);
            intent.setAction("NOTIFY" + id); //$NON-NLS-1$
            intent.putExtra(NotificationActivity.EXTRA_TASK_ID, id);
            intent.putExtra(NotificationActivity.EXTRA_TITLE, taskTitle);
            builder.setContentIntent(PendingIntent.getActivity(context, (int) id, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        }

        if (!Strings.isNullOrEmpty(taskDescription)) {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(taskDescription));
        }
        Intent completeIntent = new Intent(context, CompleteTaskReceiver.class);
        completeIntent.putExtra(CompleteTaskReceiver.TASK_ID, id);
        PendingIntent completePendingIntent = PendingIntent.getBroadcast(context, (int) id, completeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action completeAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_check_white_24dp, context.getResources().getString(R.string.rmd_NoA_done), completePendingIntent).build();

        Intent snoozeIntent = new Intent(context, SnoozeActivity.class);
        snoozeIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        snoozeIntent.putExtra(SnoozeActivity.EXTRA_TASK_ID, id);
        PendingIntent snoozePendingIntent = PendingIntent.getActivity(context, (int) id, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();
        wearableExtender.addAction(completeAction);
        for (final SnoozeOption snoozeOption : SnoozeDialog.getSnoozeOptions(preferences)) {
            final long timestamp = snoozeOption.getDateTime().getMillis();
            Intent wearableIntent = new Intent(context, SnoozeActivity.class);
            wearableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            wearableIntent.setAction(String.format("snooze-%s-%s", id, timestamp));
            wearableIntent.putExtra(SnoozeActivity.EXTRA_TASK_ID, id);
            wearableIntent.putExtra(SnoozeActivity.EXTRA_SNOOZE_TIME, timestamp);
            PendingIntent wearablePendingIntent = PendingIntent.getActivity(context, (int) id, wearableIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            wearableExtender.addAction(new NotificationCompat.Action.Builder(
                    R.drawable.ic_snooze_white_24dp, context.getString(snoozeOption.getResId()), wearablePendingIntent)
                    .build());
        }

        return builder.addAction(completeAction)
                .addAction(R.drawable.ic_snooze_white_24dp, context.getResources().getString(R.string.rmd_NoA_snooze), snoozePendingIntent)
                .extend(wearableExtender);
    }
}
