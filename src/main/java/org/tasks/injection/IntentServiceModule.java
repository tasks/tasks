package org.tasks.injection;

import org.tasks.location.GeofenceTransitionsIntentService;
import org.tasks.scheduling.*;

import dagger.Module;

@Module(addsTo = TasksModule.class,
        injects = {
                GeofenceSchedulingIntentService.class,
                BackupIntentService.class,
                GtasksBackgroundService.class,
                RefreshSchedulerIntentService.class,
                ReminderSchedulerIntentService.class,
                GeofenceTransitionsIntentService.class,
                CalendarNotificationIntentService.class
        })
public class IntentServiceModule {
}
