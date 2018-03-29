package org.tasks.jobs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.evernote.android.job.Job;
import javax.inject.Inject;
import org.tasks.Notifier;
import org.tasks.injection.ApplicationScope;
import org.tasks.preferences.Preferences;

@ApplicationScope
public class JobCreator implements com.evernote.android.job.JobCreator {

  private final Preferences preferences;
  private final Notifier notifier;
  private final NotificationQueue notificationQueue;

  @Inject
  public JobCreator(Preferences preferences, Notifier notifier, NotificationQueue notificationQueue) {
    this.preferences = preferences;
    this.notifier = notifier;
    this.notificationQueue = notificationQueue;
  }

  @Nullable
  @Override
  public Job create(@NonNull String tag) {
    switch (tag) {
      case NotificationJob.TAG:
        return new NotificationJob(preferences, notifier, notificationQueue);
      default:
        throw new IllegalArgumentException("Unhandled tag: " + tag);
    }
  }
}
