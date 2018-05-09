package org.tasks.notifications;

import static android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastNougat;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastOreo;

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
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.ReminderService;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
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
import org.tasks.time.DateTime;
import org.tasks.ui.CheckBoxes;
import timber.log.Timber;

@ApplicationScope
public class NotificationManager {

  public static final String NOTIFICATION_CHANNEL_DEFAULT = "notifications";
  public static final String NOTIFICATION_CHANNEL_TASKER = "notifications_tasker";
  public static final String NOTIFICATION_CHANNEL_CALLS = "notifications_calls";
  public static final String NOTIFICATION_CHANNEL_TIMERS = "notifications_timers";
  static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
  private static final String GROUP_KEY = "tasks";
  private static final int SUMMARY_NOTIFICATION_ID = 0;
  private final NotificationManagerCompat notificationManagerCompat;
  private final NotificationDao notificationDao;
  private final TaskDao taskDao;
  private final Context context;
  private final Preferences preferences;
  private final CheckBoxes checkBoxes;

  @Inject
  public NotificationManager(
      @ForApplication Context context,
      Preferences preferences,
      NotificationDao notificationDao,
      TaskDao taskDao,
      CheckBoxes checkBoxes) {
    this.context = context;
    this.preferences = preferences;
    this.notificationDao = notificationDao;
    this.taskDao = taskDao;
    this.checkBoxes = checkBoxes;
    notificationManagerCompat = NotificationManagerCompat.from(context);
    if (atLeastOreo()) {
      android.app.NotificationManager notificationManager =
          (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      notificationManager.createNotificationChannel(
          createNotificationChannel(NOTIFICATION_CHANNEL_DEFAULT, R.string.notifications));
      notificationManager.createNotificationChannel(
          createNotificationChannel(NOTIFICATION_CHANNEL_CALLS, R.string.missed_calls));
      notificationManager.createNotificationChannel(
          createNotificationChannel(NOTIFICATION_CHANNEL_TASKER, R.string.tasker_locale));
      notificationManager.createNotificationChannel(
          createNotificationChannel(NOTIFICATION_CHANNEL_TIMERS, R.string.TEA_timer_controls));
    }
  }

  @TargetApi(Build.VERSION_CODES.O)
  private NotificationChannel createNotificationChannel(String channelId, int nameResId) {
    String channelName = context.getString(nameResId);
    NotificationChannel notificationChannel =
        new NotificationChannel(
            channelId, channelName, android.app.NotificationManager.IMPORTANCE_HIGH);
    notificationChannel.enableLights(true);
    notificationChannel.enableVibration(true);
    notificationChannel.setBypassDnd(true);
    notificationChannel.setShowBadge(true);
    notificationChannel.setImportance(android.app.NotificationManager.IMPORTANCE_HIGH);
    return notificationChannel;
  }

  public void cancel(long id) {
    notificationManagerCompat.cancel((int) id);
    Completable.fromAction(
            () -> {
              if (id == SUMMARY_NOTIFICATION_ID) {
                List<Long> tasks = transform(notificationDao.getAll(), n -> n.taskId);
                for (Long task : tasks) {
                  notificationManagerCompat.cancel(task.intValue());
                }
                notificationDao.deleteAll(tasks);
              } else if (notificationDao.delete(id) > 0) {
                notifyTasks(Collections.emptyList(), false, false, false);
              }
            })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribe();
  }

  public void cancel(List<Long> ids) {
    Completable.fromAction(
            () -> {
              for (Long id : ids) {
                notificationManagerCompat.cancel(id.intValue());
              }
              if (notificationDao.deleteAll(ids) > 0) {
                notifyTasks(Collections.emptyList(), false, false, false);
              }
            })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribe();
  }

  public void restoreNotifications(boolean cancelExisting) {
    List<org.tasks.notifications.Notification> notifications = notificationDao.getAllOrdered();
    if (cancelExisting) {
      for (org.tasks.notifications.Notification notification : notifications) {
        notificationManagerCompat.cancel((int) notification.taskId);
      }
    }

    if (preferences.bundleNotifications() && notifications.size() > 1) {
      updateSummary(false, false, false, Collections.emptyList());
      createNotifications(notifications, false, false, false, true);
    } else {
      createNotifications(notifications, false, false, false, false);
      notificationManagerCompat.cancel(SUMMARY_NOTIFICATION_ID);
    }
  }

  public void notifyTasks(
      List<org.tasks.notifications.Notification> newNotifications,
      boolean alert,
      boolean nonstop,
      boolean fiveTimes) {
    List<org.tasks.notifications.Notification> existingNotifications =
        notificationDao.getAllOrdered();
    notificationDao.insertAll(newNotifications);
    int totalCount = existingNotifications.size() + newNotifications.size();
    if (totalCount == 0) {
      notificationManagerCompat.cancel(SUMMARY_NOTIFICATION_ID);
    } else if (totalCount == 1) {
      List<org.tasks.notifications.Notification> notifications =
          newArrayList(concat(existingNotifications, newNotifications));
      createNotifications(notifications, alert, nonstop, fiveTimes, false);
      notificationManagerCompat.cancel(SUMMARY_NOTIFICATION_ID);
    } else if (preferences.bundleNotifications()) {
      updateSummary(false, false, false, Collections.emptyList());

      if (existingNotifications.size() == 1) {
        createNotifications(existingNotifications, false, false, false, true);
      }

      if (atLeastNougat() && newNotifications.size() == 1) {
        createNotifications(newNotifications, alert, nonstop, fiveTimes, true);
      } else {
        createNotifications(newNotifications, false, false, false, true);
        updateSummary(alert, nonstop, fiveTimes, newNotifications);
      }
    } else {
      createNotifications(newNotifications, alert, nonstop, fiveTimes, false);
    }
  }

  private void createNotifications(
      List<org.tasks.notifications.Notification> notifications,
      boolean alert,
      boolean nonstop,
      boolean fiveTimes,
      boolean useGroupKey) {
    for (org.tasks.notifications.Notification notification : notifications) {
      NotificationCompat.Builder builder = getTaskNotification(notification);
      if (builder == null) {
        notificationManagerCompat.cancel((int) notification.taskId);
        notificationDao.delete(notification.taskId);
      } else {
        builder
            .setGroup(
                useGroupKey
                    ? GROUP_KEY
                    : (atLeastNougat() ? Long.toString(notification.taskId) : null))
            .setGroupAlertBehavior(
                alert
                    ? NotificationCompat.GROUP_ALERT_CHILDREN
                    : NotificationCompat.GROUP_ALERT_SUMMARY);
        notify(notification.taskId, builder, alert, nonstop, fiveTimes);
        alert = false;
      }
    }
  }

  public void notify(
      long notificationId,
      NotificationCompat.Builder builder,
      boolean alert,
      boolean nonstop,
      boolean fiveTimes) {
    if (!preferences.getBoolean(R.string.p_rmd_enabled, true)) {
      return;
    }
    int ringTimes = fiveTimes ? 5 : 1;
    if (alert) {
      builder
          .setSound(preferences.getRingtone())
          .setPriority(NotificationCompat.PRIORITY_HIGH)
          .setDefaults(preferences.getNotificationDefaults());
    } else {
      builder.setDefaults(0).setTicker(null);
    }
    Notification notification = builder.build();
    if (alert && nonstop) {
      notification.flags |= Notification.FLAG_INSISTENT;
      ringTimes = 1;
    }
    if (preferences.usePersistentReminders()) {
      notification.flags |= Notification.FLAG_NO_CLEAR;
    }
    Intent deleteIntent = new Intent(context, NotificationClearedReceiver.class);
    deleteIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
    notification.deleteIntent =
        PendingIntent.getBroadcast(
            context, (int) notificationId, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    for (int i = 0; i < ringTimes; i++) {
      notificationManagerCompat.notify((int) notificationId, notification);
    }
  }

  private void updateSummary(
      boolean notify,
      boolean nonStop,
      boolean fiveTimes,
      List<org.tasks.notifications.Notification> newNotifications) {
    List<Task> tasks = taskDao.activeNotifications();
    int taskCount = tasks.size();
    ArrayList<Long> taskIds = newArrayList(transform(tasks, Task::getId));
    Filter filter =
        new Filter(
            context.getString(R.string.notifications),
            new QueryTemplate().where(Task.ID.in(taskIds)));
    long when = notificationDao.latestTimestamp();
    int maxPriority = 3;
    String summaryTitle =
        context.getResources().getQuantityString(R.plurals.task_count, taskCount, taskCount);
    NotificationCompat.InboxStyle style =
        new NotificationCompat.InboxStyle().setBigContentTitle(summaryTitle);
    List<String> titles = newArrayList();
    List<String> ticker = newArrayList();
    for (Task task : tasks) {
      String title = task.getTitle();
      style.addLine(title);
      titles.add(title);
      maxPriority = Math.min(maxPriority, task.getPriority());
    }
    for (org.tasks.notifications.Notification notification : newNotifications) {
      Task task = tryFind(tasks, t -> t.getId() == notification.taskId).orNull();
      if (task == null) {
        continue;
      }
      ticker.add(task.getTitle());
    }
    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(context, NotificationManager.NOTIFICATION_CHANNEL_DEFAULT)
            .setContentTitle(summaryTitle)
            .setContentText(
                Joiner.on(context.getString(R.string.list_separator_with_space)).join(titles))
            .setShowWhen(true)
            .setWhen(when)
            .setSmallIcon(R.drawable.ic_done_all_white_24dp)
            .setStyle(style)
            .setColor(checkBoxes.getPriorityColor(maxPriority))
            .setOnlyAlertOnce(false)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    TaskIntents.getTaskListIntent(context, filter),
                    PendingIntent.FLAG_UPDATE_CURRENT))
            .setGroupSummary(true)
            .setGroup(GROUP_KEY)
            .setTicker(
                Joiner.on(context.getString(R.string.list_separator_with_space)).join(ticker))
            .setGroupAlertBehavior(
                notify
                    ? NotificationCompat.GROUP_ALERT_SUMMARY
                    : NotificationCompat.GROUP_ALERT_CHILDREN);

    Intent snoozeIntent = new Intent(context, SnoozeActivity.class);
    snoozeIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
    snoozeIntent.putExtra(SnoozeActivity.EXTRA_TASK_IDS, taskIds);
    builder.addAction(
        R.drawable.ic_snooze_white_24dp,
        context.getResources().getString(R.string.snooze_all),
        PendingIntent.getActivity(context, 0, snoozeIntent, PendingIntent.FLAG_CANCEL_CURRENT));

    notify(NotificationManager.SUMMARY_NOTIFICATION_ID, builder, notify, nonStop, fiveTimes);
  }

  public NotificationCompat.Builder getTaskNotification(
      org.tasks.notifications.Notification notification) {
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
    boolean dueInFuture =
        task.hasDueTime() && task.getDueDate() > DateUtilities.now()
            || !task.hasDueTime()
                && task.getDueDate() - DateUtilities.now() > DateUtilities.ONE_DAY;
    if ((type == ReminderService.TYPE_DUE || type == ReminderService.TYPE_OVERDUE)
        && (!task.hasDueDate() || dueInFuture)) {
      return null;
    }

    // read properties
    final String taskTitle = task.getTitle();
    final String taskDescription = task.getNotes();

    // update last reminder time
    task.setReminderLast(new DateTime(when).endOfMinute().getMillis());
    taskDao.save(task);

    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(context, NotificationManager.NOTIFICATION_CHANNEL_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentTitle(taskTitle)
            .setColor(checkBoxes.getPriorityColor(task.getPriority()))
            .setSmallIcon(R.drawable.ic_check_white_24dp)
            .setWhen(when)
            .setOnlyAlertOnce(false)
            .setShowWhen(true)
            .setTicker(taskTitle);

    final Intent intent = new Intent(context, NotificationActivity.class);
    intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK);
    intent.setAction("NOTIFY" + id); // $NON-NLS-1$
    intent.putExtra(NotificationActivity.EXTRA_TASK_ID, id);
    intent.putExtra(NotificationActivity.EXTRA_TITLE, taskTitle);
    builder.setContentIntent(
        PendingIntent.getActivity(context, (int) id, intent, PendingIntent.FLAG_UPDATE_CURRENT));

    if (!Strings.isNullOrEmpty(taskDescription)) {
      builder
          .setContentText(taskDescription)
          .setStyle(new NotificationCompat.BigTextStyle().bigText(taskDescription));
    }
    Intent completeIntent = new Intent(context, CompleteTaskReceiver.class);
    completeIntent.putExtra(CompleteTaskReceiver.TASK_ID, id);
    PendingIntent completePendingIntent =
        PendingIntent.getBroadcast(
            context, (int) id, completeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    NotificationCompat.Action completeAction =
        new NotificationCompat.Action.Builder(
                R.drawable.ic_check_white_24dp,
                context.getResources().getString(R.string.rmd_NoA_done),
                completePendingIntent)
            .build();

    Intent snoozeIntent = new Intent(context, SnoozeActivity.class);
    snoozeIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
    snoozeIntent.putExtra(SnoozeActivity.EXTRA_TASK_ID, id);
    PendingIntent snoozePendingIntent =
        PendingIntent.getActivity(
            context, (int) id, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    NotificationCompat.WearableExtender wearableExtender =
        new NotificationCompat.WearableExtender();
    wearableExtender.addAction(completeAction);
    for (final SnoozeOption snoozeOption : SnoozeDialog.getSnoozeOptions(preferences)) {
      final long timestamp = snoozeOption.getDateTime().getMillis();
      Intent wearableIntent = new Intent(context, SnoozeActivity.class);
      wearableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      wearableIntent.setAction(String.format("snooze-%s-%s", id, timestamp));
      wearableIntent.putExtra(SnoozeActivity.EXTRA_TASK_ID, id);
      wearableIntent.putExtra(SnoozeActivity.EXTRA_SNOOZE_TIME, timestamp);
      PendingIntent wearablePendingIntent =
          PendingIntent.getActivity(
              context, (int) id, wearableIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      wearableExtender.addAction(
          new NotificationCompat.Action.Builder(
                  R.drawable.ic_snooze_white_24dp,
                  context.getString(snoozeOption.getResId()),
                  wearablePendingIntent)
              .build());
    }

    return builder
        .addAction(completeAction)
        .addAction(
            R.drawable.ic_snooze_white_24dp,
            context.getResources().getString(R.string.rmd_NoA_snooze),
            snoozePendingIntent)
        .extend(wearableExtender);
  }
}
