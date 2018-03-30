package org.tasks.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.evernote.android.job.Job;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.Notifier;
import org.tasks.backup.TasksJsonExporter;
import org.tasks.caldav.CaldavSynchronizer;
import org.tasks.gtasks.GoogleTaskSynchronizer;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import org.tasks.scheduling.RefreshScheduler;

@ApplicationScope
public class JobCreator implements com.evernote.android.job.JobCreator {

  private final Context context;
  private final Preferences preferences;
  private final Notifier notifier;
  private final NotificationQueue notificationQueue;
  private final TasksJsonExporter tasksJsonExporter;
  private final RefreshScheduler refreshScheduler;
  private final LocalBroadcastManager localBroadcastManager;
  private final CaldavSynchronizer caldavSynchronizer;
  private final GoogleTaskSynchronizer googleTaskSynchronizer;

  static final String TAG_BACKUP = "tag_backup";
  static final String TAG_REFRESH = "tag_refresh";
  static final String TAG_MIDNIGHT_REFRESH = "tag_midnight_refresh";
  static final String TAG_NOTIFICATION = "tag_notification";
  static final String TAG_SYNC = "tag_sync";

  @Inject
  public JobCreator(@ForApplication Context context, Preferences preferences, Notifier notifier,
      NotificationQueue notificationQueue, TasksJsonExporter tasksJsonExporter,
      RefreshScheduler refreshScheduler, LocalBroadcastManager localBroadcastManager,
      CaldavSynchronizer caldavSynchronizer, GoogleTaskSynchronizer googleTaskSynchronizer) {
    this.context = context;
    this.preferences = preferences;
    this.notifier = notifier;
    this.notificationQueue = notificationQueue;
    this.tasksJsonExporter = tasksJsonExporter;
    this.refreshScheduler = refreshScheduler;
    this.localBroadcastManager = localBroadcastManager;
    this.caldavSynchronizer = caldavSynchronizer;
    this.googleTaskSynchronizer = googleTaskSynchronizer;
  }

  @Nullable
  @Override
  public Job create(@NonNull String tag) {
    switch (tag) {
      case TAG_NOTIFICATION:
        return new NotificationJob(preferences, notifier, notificationQueue);
      case TAG_SYNC:
        return new SyncJob(caldavSynchronizer, googleTaskSynchronizer);
      case TAG_BACKUP:
        return new BackupJob(context, tasksJsonExporter, preferences);
      case TAG_MIDNIGHT_REFRESH:
      case TAG_REFRESH:
        return new RefreshJob(refreshScheduler, localBroadcastManager);
      default:
        throw new IllegalArgumentException("Unhandled tag: " + tag);
    }
  }
}
