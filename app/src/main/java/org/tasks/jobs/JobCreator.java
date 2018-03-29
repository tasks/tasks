package org.tasks.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.evernote.android.job.Job;
import javax.inject.Inject;
import org.tasks.Notifier;
import org.tasks.backup.TasksJsonExporter;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

@ApplicationScope
public class JobCreator implements com.evernote.android.job.JobCreator {

  private final Context context;
  private final Preferences preferences;
  private final Notifier notifier;
  private final NotificationQueue notificationQueue;
  private final TasksJsonExporter tasksJsonExporter;

  @Inject
  public JobCreator(@ForApplication Context context, Preferences preferences, Notifier notifier,
      NotificationQueue notificationQueue, TasksJsonExporter tasksJsonExporter) {
    this.context = context;
    this.preferences = preferences;
    this.notifier = notifier;
    this.notificationQueue = notificationQueue;
    this.tasksJsonExporter = tasksJsonExporter;
  }

  @Nullable
  @Override
  public Job create(@NonNull String tag) {
    switch (tag) {
      case NotificationJob.TAG:
        return new NotificationJob(preferences, notifier, notificationQueue);
      case BackupJob.TAG:
        return new BackupJob(context, tasksJsonExporter, preferences);
      default:
        throw new IllegalArgumentException("Unhandled tag: " + tag);
    }
  }
}
