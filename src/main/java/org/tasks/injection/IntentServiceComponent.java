package org.tasks.injection;

import org.tasks.jobs.AlarmJob;
import org.tasks.jobs.BackupJob;
import org.tasks.jobs.MidnightRefreshJob;
import org.tasks.jobs.RefreshJob;
import org.tasks.jobs.ReminderJob;
import org.tasks.location.GeofenceTransitionsIntentService;
import org.tasks.scheduling.CalendarNotificationIntentService;
import org.tasks.scheduling.GeofenceSchedulingIntentService;
import org.tasks.scheduling.NotificationSchedulerIntentService;
import org.tasks.scheduling.SchedulerIntentService;

import dagger.Subcomponent;

@Subcomponent(modules = IntentServiceModule.class)
public interface IntentServiceComponent {
    void inject(SchedulerIntentService schedulerIntentService);

    void inject(GeofenceSchedulingIntentService geofenceSchedulingIntentService);

    void inject(CalendarNotificationIntentService calendarNotificationIntentService);

    void inject(GeofenceTransitionsIntentService geofenceTransitionsIntentService);

    void inject(NotificationSchedulerIntentService notificationSchedulerIntentService);

    void inject(AlarmJob alarmJob);

    void inject(BackupJob backupJob);

    void inject(MidnightRefreshJob midnightRefreshJob);

    void inject(RefreshJob refreshJob);

    void inject(ReminderJob reminderJob);
}
