package org.tasks.injection;

import dagger.Subcomponent;
import org.tasks.jobs.AfterSaveIntentService;
import org.tasks.locale.receiver.TaskerIntentService;
import org.tasks.location.GeofenceTransitionsIntentService;
import org.tasks.scheduling.BackgroundScheduler;
import org.tasks.scheduling.CalendarNotificationIntentService;
import org.tasks.scheduling.GeofenceSchedulingIntentService;
import org.tasks.scheduling.NotificationSchedulerIntentService;

@Subcomponent(modules = IntentServiceModule.class)
public interface IntentServiceComponent {

  void inject(GeofenceSchedulingIntentService geofenceSchedulingIntentService);

  void inject(CalendarNotificationIntentService calendarNotificationIntentService);

  void inject(GeofenceTransitionsIntentService geofenceTransitionsIntentService);

  void inject(NotificationSchedulerIntentService notificationSchedulerIntentService);

  void inject(BackgroundScheduler backgroundScheduler);

  void inject(AfterSaveIntentService afterSaveIntentService);

  void inject(TaskerIntentService taskerIntentService);
}
