package org.tasks.scheduling;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.JobIntentService;
import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.reminders.ReminderService;
import javax.inject.Inject;
import org.tasks.injection.InjectingJobIntentService;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.jobs.NotificationQueue;
import org.tasks.notifications.NotificationManager;
import timber.log.Timber;

public class NotificationSchedulerIntentService extends InjectingJobIntentService {

  private static final String EXTRA_CANCEL_EXISTING_NOTIFICATIONS =
      "extra_cancel_existing_notifications";
  @Inject AlarmService alarmService;
  @Inject ReminderService reminderService;
  @Inject NotificationQueue notificationQueue;
  @Inject NotificationManager notificationManager;

  public static void enqueueWork(Context context, boolean cancelNotifications) {
    Intent intent = new Intent();
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

    notificationQueue.clear();

    boolean cancelExistingNotifications =
        intent.getBooleanExtra(EXTRA_CANCEL_EXISTING_NOTIFICATIONS, false);

    notificationManager.restoreNotifications(cancelExistingNotifications);
    reminderService.scheduleAllAlarms();
    alarmService.scheduleAllAlarms();
  }

  @Override
  protected void inject(IntentServiceComponent component) {
    component.inject(this);
  }
}
