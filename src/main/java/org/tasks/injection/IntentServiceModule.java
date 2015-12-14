package org.tasks.injection;

import org.tasks.location.GeofenceTransitionsIntentService;
import org.tasks.scheduling.*;

import dagger.Module;

@Module(addsTo = TasksModule.class,
        injects = {
                AlarmSchedulingIntentService.class,
                BackupIntentService.class,
                GtasksBackgroundService.class,
                MidnightRefreshService.class,
                RefreshSchedulerIntentService.class,
                ReminderSchedulerIntentService.class,
                GeofenceTransitionsIntentService.class,
                CalendarNotificationIntentService.class
        })
public class IntentServiceModule {
}
