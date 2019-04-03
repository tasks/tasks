package org.tasks.jobs;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;
import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.timers.TimerPlugin;
import javax.inject.Inject;
import org.tasks.data.TaskAttachment;
import org.tasks.data.TaskAttachmentDao;
import org.tasks.data.UserActivity;
import org.tasks.data.UserActivityDao;
import org.tasks.files.FileHelper;
import org.tasks.injection.InjectingWorker;
import org.tasks.injection.JobComponent;
import org.tasks.location.GeofenceApi;
import org.tasks.notifications.NotificationManager;
import timber.log.Timber;

public class CleanupWork extends InjectingWorker {

  static final String EXTRA_TASK_IDS = "extra_task_ids";
  private final Context context;
  @Inject NotificationManager notificationManager;
  @Inject GeofenceApi geofenceApi;
  @Inject TimerPlugin timerPlugin;
  @Inject ReminderService reminderService;
  @Inject AlarmService alarmService;
  @Inject TaskAttachmentDao taskAttachmentDao;
  @Inject UserActivityDao userActivityDao;

  public CleanupWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
    this.context = context;
  }

  @NonNull
  @Override
  public Result run() {
    long[] tasks = getInputData().getLongArray(EXTRA_TASK_IDS);
    if (tasks == null) {
      Timber.e("No task ids provided");
      return Result.failure();
    }
    for (long task : tasks) {
      alarmService.cancelAlarms(task);
      reminderService.cancelReminder(task);
      notificationManager.cancel(task);
      geofenceApi.cancel(task);
      for (TaskAttachment attachment : taskAttachmentDao.getAttachments(task)) {
        FileHelper.delete(context, attachment.parseUri());
        taskAttachmentDao.delete(attachment);
      }
      for (UserActivity comment : userActivityDao.getComments(task)) {
        FileHelper.delete(context, comment.getPictureUri());
        userActivityDao.delete(comment);
      }
    }
    timerPlugin.updateNotifications();
    return Result.success();
  }

  @Override
  protected void inject(JobComponent component) {
    component.inject(this);
  }
}
