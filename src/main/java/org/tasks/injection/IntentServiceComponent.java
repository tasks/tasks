package org.tasks.injection;

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
}
