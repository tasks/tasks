package org.tasks;

import static android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.google.common.collect.Lists.transform;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.voice.VoiceOutputAssistant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.injection.ForApplication;
import org.tasks.jobs.NotificationQueueEntry;
import org.tasks.notifications.AudioManager;
import org.tasks.notifications.NotificationManager;
import org.tasks.notifications.TelephonyManager;
import org.tasks.preferences.Preferences;
import org.tasks.ui.CheckBoxes;
import timber.log.Timber;

public class Notifier {

  private final Context context;
  private final TaskDao taskDao;
  private final NotificationManager notificationManager;
  private final TelephonyManager telephonyManager;
  private final AudioManager audioManager;
  private final VoiceOutputAssistant voiceOutputAssistant;
  private final Preferences preferences;
  private final CheckBoxes checkBoxes;

  @Inject
  public Notifier(
      @ForApplication Context context,
      TaskDao taskDao,
      NotificationManager notificationManager,
      TelephonyManager telephonyManager,
      AudioManager audioManager,
      VoiceOutputAssistant voiceOutputAssistant,
      Preferences preferences,
      CheckBoxes checkBoxes) {
    this.context = context;
    this.taskDao = taskDao;
    this.notificationManager = notificationManager;
    this.telephonyManager = telephonyManager;
    this.audioManager = audioManager;
    this.voiceOutputAssistant = voiceOutputAssistant;
    this.preferences = preferences;
    this.checkBoxes = checkBoxes;
  }

  public void triggerFilterNotification(final Filter filter) {
    List<Task> tasks = taskDao.fetchFiltered(filter);
    int count = tasks.size();
    if (count == 0) {
      return;
    }

    Intent intent = new Intent(context, MainActivity.class);
    intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK);
    intent.putExtra(MainActivity.OPEN_FILTER, filter);
    PendingIntent pendingIntent =
        PendingIntent.getActivity(
            context, filter.listingTitle.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

    String summaryTitle =
        context.getResources().getQuantityString(R.plurals.task_count, count, count);
    NotificationCompat.InboxStyle style =
        new NotificationCompat.InboxStyle().setBigContentTitle(summaryTitle);
    int maxPriority = 3;
    for (Task task : tasks) {
      style.addLine(task.getTitle());
      maxPriority = Math.min(maxPriority, task.getPriority());
    }

    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(context, NotificationManager.NOTIFICATION_CHANNEL_TASKER)
            .setSmallIcon(R.drawable.ic_done_all_white_24dp)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setTicker(summaryTitle)
            .setContentTitle(summaryTitle)
            .setContentText(filter.listingTitle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setWhen(currentTimeMillis())
            .setShowWhen(true)
            .setColor(checkBoxes.getPriorityColor(maxPriority))
            .setGroupSummary(true)
            .setGroup(filter.listingTitle)
            .setStyle(style);

    notificationManager.notify(filter.listingTitle.hashCode(), builder, true, false, false);
  }

  public void triggerTaskNotification(long id, int type) {
    org.tasks.notifications.Notification notification = new org.tasks.notifications.Notification();
    notification.taskId = id;
    notification.type = type;
    notification.timestamp = currentTimeMillis();
    triggerNotifications(Collections.singletonList(notification), true);
  }

  public void triggerTaskNotifications(List<? extends NotificationQueueEntry> entries) {
    triggerNotifications(transform(entries, NotificationQueueEntry::toNotification), true);
  }

  private void triggerNotifications(
      List<org.tasks.notifications.Notification> entries, boolean alert) {
    List<org.tasks.notifications.Notification> notifications = new ArrayList<>();
    boolean ringFiveTimes = false;
    boolean ringNonstop = false;
    for (int i = 0; i < entries.size(); i++) {
      org.tasks.notifications.Notification entry = entries.get(i);
      Task task = taskDao.fetch(entry.taskId);
      if (task == null) {
        continue;
      }
      if (entry.type != ReminderService.TYPE_RANDOM) {
        ringFiveTimes |= task.isNotifyModeFive();
        ringNonstop |= task.isNotifyModeNonstop();
      }
      NotificationCompat.Builder notification = notificationManager.getTaskNotification(entry);
      if (notification != null) {
        notifications.add(entry);
      }
    }

    if (notifications.isEmpty()) {
      return;
    } else {
      Timber.d("Triggering %s", notifications);
    }

    notificationManager.notifyTasks(notifications, alert, ringNonstop, ringFiveTimes);

    if (alert
        && preferences.getBoolean(R.string.p_voiceRemindersEnabled, false)
        && !ringNonstop
        && !audioManager.notificationsMuted()
        && telephonyManager.callStateIdle()) {
      for (org.tasks.notifications.Notification notification : notifications) {
        AndroidUtilities.sleepDeep(2000);
        voiceOutputAssistant.speak(
            notificationManager.getTaskNotification(notification).build().tickerText.toString());
      }
    }
  }
}
