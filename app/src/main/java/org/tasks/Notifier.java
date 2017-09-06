package org.tasks;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.google.common.base.Strings;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.voice.VoiceOutputAssistant;

import org.tasks.db.AppDatabase;
import org.tasks.injection.ForApplication;
import org.tasks.intents.TaskIntents;
import org.tasks.jobs.JobQueueEntry;
import org.tasks.notifications.AudioManager;
import org.tasks.notifications.NotificationManager;
import org.tasks.notifications.TelephonyManager;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.CompleteTaskReceiver;
import org.tasks.reminders.NotificationActivity;
import org.tasks.reminders.SnoozeActivity;
import org.tasks.reminders.SnoozeDialog;
import org.tasks.reminders.SnoozeOption;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import timber.log.Timber;

import static android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybean;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastNougat;
import static org.tasks.notifications.NotificationManager.GROUP_KEY;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

public class Notifier {

    private final Context context;
    private final TaskDao taskDao;
    private final NotificationManager notificationManager;
    private final TelephonyManager telephonyManager;
    private final AudioManager audioManager;
    private final VoiceOutputAssistant voiceOutputAssistant;
    private final Preferences preferences;
    private final AppDatabase appDatabase;

    @Inject
    public Notifier(@ForApplication Context context, TaskDao taskDao,
                    NotificationManager notificationManager, TelephonyManager telephonyManager,
                    AudioManager audioManager, VoiceOutputAssistant voiceOutputAssistant,
                    Preferences preferences, AppDatabase appDatabase) {
        this.context = context;
        this.taskDao = taskDao;
        this.notificationManager = notificationManager;
        this.telephonyManager = telephonyManager;
        this.audioManager = audioManager;
        this.voiceOutputAssistant = voiceOutputAssistant;
        this.preferences = preferences;
        this.appDatabase = appDatabase;
    }

    public void triggerFilterNotification(final Filter filter) {
        String title = filter.listingTitle;
        String query = filter.getSqlQuery();
        int count = taskDao.count(filter);
        if (count == 0) {
            return;
        }

        String subtitle = context.getString(R.string.task_count, count);

        Intent intent = new Intent(context, TaskListActivity.class);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(TaskListActivity.OPEN_FILTER, filter);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (title + query).hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, NotificationManager.NOTIFICATION_CHANNEL_TASKER)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setTicker(title)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(currentTimeMillis())
                .setShowWhen(true);

        notificationManager.notify(
                (title + query).hashCode(),
                notification.build(),
                true,
                false,
                false);
    }

    public void triggerTaskNotification(long id, int type) {
        org.tasks.notifications.Notification notification = new org.tasks.notifications.Notification();
        notification.taskId = id;
        notification.type = type;
        notification.timestamp = currentTimeMillis();
        triggerNotifications(Collections.singletonList(notification), true);
    }

    private NotificationCompat.Builder getTaskNotification(org.tasks.notifications.Notification notification) {
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

    public void restoreNotifications() {
        triggerNotifications(appDatabase.notificationDao().getAll(), false);
    }

    public void triggerTaskNotifications(List<? extends JobQueueEntry> entries) {
        triggerNotifications(transform(entries, JobQueueEntry::toNotification), true);
    }

    public void triggerNotifications(List<org.tasks.notifications.Notification> entries, boolean alert) {
        Map<org.tasks.notifications.Notification, Notification> notifications = new LinkedHashMap<>();
        boolean ringFiveTimes = false;
        boolean ringNonstop = false;
        for (int i = 0 ; i < entries.size() ; i++) {
            org.tasks.notifications.Notification entry = entries.get(i);
            Task task = taskDao.fetch(entry.taskId);
            if (task == null) {
                continue;
            }
            if (entry.type != ReminderService.TYPE_RANDOM) {
                ringFiveTimes |= task.isNotifyModeFive();
                ringNonstop |= task.isNotifyModeNonstop();
            }
            NotificationCompat.Builder notification = getTaskNotification(entry);
            if (notification != null) {
                notification.setGroupAlertBehavior(alert && (atLeastNougat() ? entries.size() == 1 : i == entries.size() - 1)
                        ? NotificationCompat.GROUP_ALERT_CHILDREN
                        : NotificationCompat.GROUP_ALERT_SUMMARY);
                notifications.put(entry, notification.build());
            }
        }

        if (notifications.isEmpty()) {
            return;
        } else {
            Timber.d("Triggering %s", notifications.keySet());
        }

        notificationManager.notifyTasks(notifications, alert, ringNonstop, ringFiveTimes);

        if (alert &&
                preferences.getBoolean(R.string.p_voiceRemindersEnabled, false) &&
                !ringNonstop &&
                !audioManager.notificationsMuted() &&
                telephonyManager.callStateIdle()) {
            for (Notification notification : notifications.values()) {
                AndroidUtilities.sleepDeep(2000);
                voiceOutputAssistant.speak(notification.tickerText.toString());
            }
        }
    }
}
