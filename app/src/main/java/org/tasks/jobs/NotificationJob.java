package org.tasks.jobs;

import android.support.annotation.NonNull;
import java.util.List;
import org.tasks.BuildConfig;
import org.tasks.Notifier;
import org.tasks.preferences.Preferences;

public class NotificationJob extends com.evernote.android.job.Job {

  private final Preferences preferences;
  private final Notifier notifier;
  private final NotificationQueue notificationQueue;

  NotificationJob(Preferences preferences, Notifier notifier, NotificationQueue notificationQueue) {
    this.preferences = preferences;
    this.notifier = notifier;
    this.notificationQueue = notificationQueue;
  }

  @NonNull
  @Override
  protected Result onRunJob(@NonNull Params params) {
    if (!preferences.isCurrentlyQuietHours()) {
      List<? extends NotificationQueueEntry> overdueJobs = notificationQueue.getOverdueJobs();
      notifier.triggerTaskNotifications(overdueJobs);
      boolean success = notificationQueue.remove(overdueJobs);
      if (BuildConfig.DEBUG && !success) {
        throw new RuntimeException("Failed to remove jobs from queue");
      }
    }
    notificationQueue.scheduleNext();
    return Result.SUCCESS;
  }
}
