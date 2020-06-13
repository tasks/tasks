package org.tasks.notifications;

import static android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static androidx.core.app.NotificationCompat.FLAG_INSISTENT;
import static androidx.core.app.NotificationCompat.FLAG_NO_CLEAR;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastNougat;
import static com.todoroo.andlib.utility.AndroidUtilities.preOreo;
import static com.todoroo.astrid.reminders.ReminderService.TYPE_GEOFENCE_ENTER;
import static com.todoroo.astrid.reminders.ReminderService.TYPE_GEOFENCE_EXIT;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.tasks.Strings.isNullOrEmpty;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.common.base.Joiner;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.ReminderService;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.data.LocationDao;
import org.tasks.data.Place;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ApplicationContext;
import org.tasks.intents.TaskIntents;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.CompleteTaskReceiver;
import org.tasks.reminders.NotificationActivity;
import org.tasks.reminders.SnoozeActivity;
import org.tasks.reminders.SnoozeDialog;
import org.tasks.reminders.SnoozeOption;
import org.tasks.themes.ColorProvider;
import org.tasks.time.DateTime;
import timber.log.Timber;

@ApplicationScope
public class NotificationManager {

  public static final String NOTIFICATION_CHANNEL_DEFAULT = "notifications";
  public static final String NOTIFICATION_CHANNEL_TASKER = "notifications_tasker";
  public static final String NOTIFICATION_CHANNEL_TIMERS = "notifications_timers";
  public static final String NOTIFICATION_CHANNEL_MISCELLANEOUS = "notifications_miscellaneous";
  public static final int MAX_NOTIFICATIONS = 21;
  static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
  static final int SUMMARY_NOTIFICATION_ID = 0;
  private static final String GROUP_KEY = "tasks";
  private static final int NOTIFICATIONS_PER_SECOND = 4;
  private final NotificationManagerCompat notificationManagerCompat;
  private final ColorProvider colorProvider;
  private final LocalBroadcastManager localBroadcastManager;
  private final LocationDao locationDao;
  private final NotificationDao notificationDao;
  private final TaskDao taskDao;
  private final Context context;
  private final Preferences preferences;
  private final Throttle throttle = new Throttle(NOTIFICATIONS_PER_SECOND);
  private final NotificationLimiter queue = new NotificationLimiter(MAX_NOTIFICATIONS);

  @Inject
  public NotificationManager(
      @ApplicationContext Context context,
      Preferences preferences,
      NotificationDao notificationDao,
      TaskDao taskDao,
      LocationDao locationDao,
      LocalBroadcastManager localBroadcastManager) {
    this.context = context;
    this.preferences = preferences;
    this.notificationDao = notificationDao;
    this.taskDao = taskDao;
    this.locationDao = locationDao;
    this.localBroadcastManager = localBroadcastManager;
    this.colorProvider = new ColorProvider(context, preferences);
    notificationManagerCompat = NotificationManagerCompat.from(context);
  }

  @SuppressLint("CheckResult")
  public void cancel(long id) {
    if (id == SUMMARY_NOTIFICATION_ID) {
      //noinspection ResultOfMethodCallIgnored
      Single.fromCallable(() -> concat(notificationDao.getAll(), singletonList(id)))
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(this::cancel);
    } else {
      cancel(singletonList(id));
    }
  }

  @SuppressLint("CheckResult")
  public void cancel(Iterable<Long> ids) {
    for (Long id : ids) {
      notificationManagerCompat.cancel(id.intValue());
      queue.remove(id);
    }

    //noinspection ResultOfMethodCallIgnored
    Completable.fromAction(() -> notificationDao.deleteAll(newArrayList(ids)))
        .subscribeOn(Schedulers.io())
        .subscribe(() -> notifyTasks(emptyList(), false, false, false));
  }

  public void restoreNotifications(boolean cancelExisting) {
    List<Notification> notifications = notificationDao.getAllOrdered();
    if (cancelExisting) {
      for (Notification notification : notifications) {
        notificationManagerCompat.cancel((int) notification.getTaskId());
      }
    }

    if (preferences.bundleNotifications() && notifications.size() > 1) {
      updateSummary(false, false, false, Collections.emptyList());
      createNotifications(notifications, false, false, false, true);
    } else {
      createNotifications(notifications, false, false, false, false);
      cancelSummaryNotification();
    }
  }

  public void notifyTasks(
      List<Notification> newNotifications, boolean alert, boolean nonstop, boolean fiveTimes) {
    List<Notification> existingNotifications = notificationDao.getAllOrdered();
    notificationDao.insertAll(newNotifications);
    int totalCount = existingNotifications.size() + newNotifications.size();
    if (totalCount == 0) {
      cancelSummaryNotification();
    } else if (totalCount == 1) {
      List<Notification> notifications =
          newArrayList(concat(existingNotifications, newNotifications));
      createNotifications(notifications, alert, nonstop, fiveTimes, false);
      cancelSummaryNotification();
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

    localBroadcastManager.broadcastRefresh();
  }

  private void createNotifications(
      List<Notification> notifications,
      boolean alert,
      boolean nonstop,
      boolean fiveTimes,
      boolean useGroupKey) {
    for (Notification notification : notifications) {
      NotificationCompat.Builder builder = getTaskNotification(notification);
      if (builder == null) {
        notificationManagerCompat.cancel((int) notification.getTaskId());
        notificationDao.delete(notification.getTaskId());
      } else {
        builder
            .setGroup(
                useGroupKey
                    ? GROUP_KEY
                    : (atLeastNougat() ? Long.toString(notification.getTaskId()) : null))
            .setGroupAlertBehavior(
                alert
                    ? NotificationCompat.GROUP_ALERT_CHILDREN
                    : NotificationCompat.GROUP_ALERT_SUMMARY);
        notify(notification.getTaskId(), builder, alert, nonstop, fiveTimes);
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
    builder.setLocalOnly(!preferences.getBoolean(R.string.p_wearable_notifications, true));
    if (preOreo()) {
      if (alert) {
        builder
            .setSound(preferences.getRingtone())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(preferences.getNotificationDefaults());
      } else {
        builder.setDefaults(0).setTicker(null);
      }
    }
    android.app.Notification notification = builder.build();
    int ringTimes = fiveTimes ? 5 : 1;
    if (alert && nonstop) {
      notification.flags |= FLAG_INSISTENT;
      ringTimes = 1;
    }
    if (preferences.usePersistentReminders()) {
      notification.flags |= FLAG_NO_CLEAR;
    }
    Intent deleteIntent = new Intent(context, NotificationClearedReceiver.class);
    deleteIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
    notification.deleteIntent =
        PendingIntent.getBroadcast(
            context, (int) notificationId, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    List<Long> evicted = queue.add(notificationId);
    if (evicted.size() > 0) {
      cancel(evicted);
    }

    for (int i = 0; i < ringTimes; i++) {
      throttle.run(() -> notificationManagerCompat.notify((int) notificationId, notification));
    }
  }

  private void updateSummary(
      boolean notify, boolean nonStop, boolean fiveTimes, List<Notification> newNotifications) {
    List<Task> tasks = taskDao.activeNotifications();
    int taskCount = tasks.size();
    if (taskCount == 0) {
      cancelSummaryNotification();
      return;
    }
    ArrayList<Long> taskIds = new ArrayList<>(transform(tasks, Task::getId));
    Filter filter =
        new Filter(
            context.getString(R.string.notifications),
            new QueryTemplate()
                .join(Join.inner(Notification.TABLE, Task.ID.eq(Notification.TASK))));
    long when = notificationDao.latestTimestamp();
    int maxPriority = 3;
    String summaryTitle =
        context.getResources().getQuantityString(R.plurals.task_count, taskCount, taskCount);
    NotificationCompat.InboxStyle style =
        new NotificationCompat.InboxStyle().setBigContentTitle(summaryTitle);
    List<String> titles = new ArrayList<>();
    List<String> ticker = new ArrayList<>();
    for (Task task : tasks) {
      String title = task.getTitle();
      style.addLine(title);
      titles.add(title);
      maxPriority = Math.min(maxPriority, task.getPriority());
    }
    for (Notification notification : newNotifications) {
      Task task = tryFind(tasks, t -> t.getId() == notification.getTaskId()).orNull();
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
            .setColor(colorProvider.getPriorityColor(maxPriority, true))
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
        context.getString(R.string.snooze_all),
        PendingIntent.getActivity(context, 0, snoozeIntent, PendingIntent.FLAG_CANCEL_CURRENT));

    notify(NotificationManager.SUMMARY_NOTIFICATION_ID, builder, notify, nonStop, fiveTimes);
  }

  public NotificationCompat.Builder getTaskNotification(Notification notification) {
    long id = notification.getTaskId();
    int type = notification.getType();
    long when = notification.getTimestamp();
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
    if (isNullOrEmpty(task.getTitle())) {
      return null;
    }

    // it's hidden - don't sound, don't delete
    if (task.isHidden() && type == ReminderService.TYPE_RANDOM) {
      return null;
    }

    // task due date was changed, but alarm wasn't rescheduled
    boolean dueInFuture =
        task.hasDueTime()
                && new DateTime(task.getDueDate()).startOfMinute().getMillis() > DateUtilities.now()
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
    long reminderTime = new DateTime(when).endOfMinute().getMillis();
    if (reminderTime != task.getReminderLast()) {
      task.setReminderLast(reminderTime);
      taskDao.save(task);
    }

    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(context, NotificationManager.NOTIFICATION_CHANNEL_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentTitle(taskTitle)
            .setColor(colorProvider.getPriorityColor(task.getPriority(), true))
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

    if (type == TYPE_GEOFENCE_ENTER || type == TYPE_GEOFENCE_EXIT) {
      Place place = locationDao.getPlace(notification.getLocation());
      if (place != null) {
        builder.setContentText(
            context.getString(
                type == TYPE_GEOFENCE_ENTER
                    ? R.string.location_arrived
                    : R.string.location_departed,
                place.getDisplayName()));
      }
    } else if (!isNullOrEmpty(taskDescription)) {
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
                context.getString(R.string.rmd_NoA_done),
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
            context.getString(R.string.rmd_NoA_snooze),
            snoozePendingIntent)
        .extend(wearableExtender);
  }

  private void cancelSummaryNotification() {
    notificationManagerCompat.cancel(SUMMARY_NOTIFICATION_ID);
  }
}
