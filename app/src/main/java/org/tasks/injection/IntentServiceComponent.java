package org.tasks.injection;

import org.tasks.jobs.NotificationJob;
import org.tasks.jobs.BackupJob;
import org.tasks.jobs.MidnightRefreshJob;
import org.tasks.jobs.RefreshJob;
import org.tasks.jobs.AfterSaveIntentService;
import org.tasks.location.GeofenceTransitionsIntentService;
import org.tasks.scheduling.BackgroundScheduler;
import org.tasks.scheduling.CalendarNotificationIntentService;
import org.tasks.scheduling.GeofenceSchedulingIntentService;
import org.tasks.scheduling.NotificationSchedulerIntentService;

import dagger.Subcomponent;

@Subcomponent(modules = IntentServiceModule.class)
public interface IntentServiceComponent {
    void inject(GeofenceSchedulingIntentService geofenceSchedulingIntentService);

    void inject(CalendarNotificationIntentService calendarNotificationIntentService);

    void inject(GeofenceTransitionsIntentService geofenceTransitionsIntentService);

    void inject(NotificationSchedulerIntentService notificationSchedulerIntentService);

    void inject(NotificationJob notificationJob);

    void inject(BackupJob backupJob);

    void inject(MidnightRefreshJob midnightRefreshJob);

    void inject(RefreshJob refreshJob);

    void inject(BackgroundScheduler backgroundScheduler);

    void inject(AfterSaveIntentService afterSaveIntentService);
}
