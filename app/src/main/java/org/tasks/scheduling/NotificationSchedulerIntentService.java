package org.tasks.scheduling;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastOreo;
import static org.tasks.notifications.NotificationManager.NOTIFICATION_CHANNEL_DEFAULT;
import static org.tasks.notifications.NotificationManager.NOTIFICATION_CHANNEL_MISCELLANEOUS;
import static org.tasks.notifications.NotificationManager.NOTIFICATION_CHANNEL_TASKER;
import static org.tasks.notifications.NotificationManager.NOTIFICATION_CHANNEL_TIMERS;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.JobIntentService;
import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.reminders.ReminderService;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingJobIntentService;
import org.tasks.injection.ServiceComponent;
import org.tasks.jobs.NotificationQueue;
import org.tasks.notifications.NotificationManager;
import timber.log.Timber;

public class NotificationSchedulerIntentService extends InjectingJobIntentService {

  private static final String EXTRA_CANCEL_EXISTING_NOTIFICATIONS =
      "extra_cancel_existing_notifications";
  @Inject @ForApplication Context context;
  @Inject AlarmService alarmService;
  @Inject ReminderService reminderService;
  @Inject NotificationQueue notificationQueue;
  @Inject NotificationManager notificationManager;

  public static void enqueueWork(Context context, boolean cancelNotifications) {
    Intent intent = new Intent(context, NotificationSchedulerIntentService.class);
    intent.putExtra(EXTRA_CANCEL_EXISTING_NOTIFICATIONS, cancelNotifications);
    JobIntentService.enqueueWork(
        context,
        NotificationSchedulerIntentService.class,
        InjectingJobIntentService.JOB_ID_NOTIFICATION_SCHEDULER,
        intent);
  }

  @Override
  protected void doWork(Intent intent) {
    Timber.d("onHandleWork(%s)", intent);

    createNotificationChannels();

    notificationQueue.clear();

    boolean cancelExistingNotifications =
        intent.getBooleanExtra(EXTRA_CANCEL_EXISTING_NOTIFICATIONS, false);

    notificationManager.restoreNotifications(cancelExistingNotifications);
    reminderService.scheduleAllAlarms();
    alarmService.scheduleAllAlarms();
  }

  private void createNotificationChannels() {
    if (atLeastOreo()) {
      android.app.NotificationManager notificationManager =
          (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      notificationManager.createNotificationChannel(
          createNotificationChannel(NOTIFICATION_CHANNEL_DEFAULT, R.string.notifications, true));
      notificationManager.createNotificationChannel(
          createNotificationChannel(NOTIFICATION_CHANNEL_TASKER, R.string.tasker_locale, true));
      notificationManager.createNotificationChannel(
          createNotificationChannel(
              NOTIFICATION_CHANNEL_TIMERS, R.string.TEA_timer_controls, true));
      notificationManager.createNotificationChannel(
          createNotificationChannel(
              NOTIFICATION_CHANNEL_MISCELLANEOUS, R.string.miscellaneous, false));
    }
  }

  @TargetApi(Build.VERSION_CODES.O)
  private NotificationChannel createNotificationChannel(
      String channelId, int nameResId, boolean alert) {
    String channelName = context.getString(nameResId);
    int importance =
        alert
            ? android.app.NotificationManager.IMPORTANCE_HIGH
            : android.app.NotificationManager.IMPORTANCE_LOW;
    NotificationChannel notificationChannel =
        new NotificationChannel(channelId, channelName, importance);
    notificationChannel.enableLights(alert);
    notificationChannel.enableVibration(alert);
    notificationChannel.setBypassDnd(alert);
    notificationChannel.setShowBadge(alert);
    return notificationChannel;
  }

  @Override
  protected void inject(ServiceComponent component) {
    component.inject(this);
  }
}
