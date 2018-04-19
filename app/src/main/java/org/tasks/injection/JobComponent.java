package org.tasks.injection;

import dagger.Subcomponent;
import org.tasks.jobs.AfterSaveIntentService;
import org.tasks.jobs.BackupJob;
import org.tasks.jobs.CleanupJob;
import org.tasks.jobs.NotificationJob;
import org.tasks.jobs.RefreshJob;
import org.tasks.jobs.SyncJob;
import org.tasks.locale.receiver.TaskerIntentService;
import org.tasks.location.GeofenceTransitionsIntentService;
import org.tasks.scheduling.BackgroundScheduler;
import org.tasks.scheduling.CalendarNotificationIntentService;
import org.tasks.scheduling.GeofenceSchedulingIntentService;
import org.tasks.scheduling.NotificationSchedulerIntentService;

@Subcomponent(modules = JobModule.class)
public interface JobComponent {

  void inject(SyncJob syncJob);

  void inject(NotificationJob notificationJob);

  void inject(BackupJob backupJob);

  void inject(RefreshJob refreshJob);

  void inject(CleanupJob cleanupJob);
}
