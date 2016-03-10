package org.tasks.injection;

import org.tasks.location.GeofenceTransitionsIntentService;
import org.tasks.scheduling.BackupIntentService;
import org.tasks.scheduling.CalendarNotificationIntentService;
import org.tasks.scheduling.GeofenceSchedulingIntentService;
import org.tasks.scheduling.GtasksBackgroundService;
import org.tasks.scheduling.RefreshSchedulerIntentService;
import org.tasks.scheduling.ReminderSchedulerIntentService;

import dagger.Subcomponent;

@Subcomponent(modules = IntentServiceModule.class)
public interface IntentServiceComponent {
    void inject(ReminderSchedulerIntentService reminderSchedulerIntentService);

    void inject(RefreshSchedulerIntentService refreshSchedulerIntentService);

    void inject(GeofenceSchedulingIntentService geofenceSchedulingIntentService);

    void inject(CalendarNotificationIntentService calendarNotificationIntentService);

    void inject(BackupIntentService backupIntentService);

    void inject(GeofenceTransitionsIntentService geofenceTransitionsIntentService);

    void inject(GtasksBackgroundService gtasksBackgroundService);
}
