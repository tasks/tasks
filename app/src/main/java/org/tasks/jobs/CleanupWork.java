package org.tasks.jobs;

import android.support.annotation.NonNull;
import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.timers.TimerPlugin;
import javax.inject.Inject;
import org.tasks.injection.InjectingWorker;
import org.tasks.injection.JobComponent;
import org.tasks.location.GeofenceService;
import org.tasks.notifications.NotificationManager;
import timber.log.Timber;

public class CleanupWork extends InjectingWorker {

  static final String EXTRA_TASK_IDS = "extra_task_ids";

  @Inject NotificationManager notificationManager;
  @Inject GeofenceService geofenceService;
  @Inject TimerPlugin timerPlugin;
  @Inject ReminderService reminderService;
  @Inject AlarmService alarmService;

  @NonNull
  @Override
  public Result doWork() {
    super.doWork();

    long[] tasks = getInputData().getLongArray(EXTRA_TASK_IDS);
    if (tasks == null) {
      Timber.e("No task ids provided");
      return Result.FAILURE;
    }
    for (long task : tasks) {
      alarmService.cancelAlarms(task);
      reminderService.cancelReminder(task);
      notificationManager.cancel(task);
      geofenceService.cancelGeofences(task);
    }
    timerPlugin.updateNotifications();
    return Result.SUCCESS;
  }

  @Override
  protected void inject(JobComponent component) {
    component.inject(this);
  }
}
