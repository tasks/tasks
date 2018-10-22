package org.tasks.jobs;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;
import java.util.List;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.Notifier;
import org.tasks.injection.JobComponent;
import org.tasks.preferences.Preferences;

public class NotificationWork extends RepeatingWorker {

  @Inject Preferences preferences;
  @Inject Notifier notifier;
  @Inject NotificationQueue notificationQueue;

  public NotificationWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
  }

  @NonNull
  @Override
  public Result run() {
    if (!preferences.isCurrentlyQuietHours()) {
      List<? extends NotificationQueueEntry> overdueJobs = notificationQueue.getOverdueJobs();
      notifier.triggerTaskNotifications(overdueJobs);
      boolean success = notificationQueue.remove(overdueJobs);
      if (BuildConfig.DEBUG && !success) {
        throw new RuntimeException("Failed to remove jobs from queue");
      }
    }
    return Result.SUCCESS;
  }

  @Override
  protected void inject(JobComponent component) {
    component.inject(this);
  }

  @Override
  protected void scheduleNext() {
    notificationQueue.scheduleNext();
  }
}
