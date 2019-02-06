package org.tasks.injection;

import dagger.Subcomponent;
import org.tasks.jobs.NotificationService;
import org.tasks.locale.receiver.TaskerIntentService;
import org.tasks.location.GeofenceTransitionsIntentService;
import org.tasks.receivers.RefreshReceiver;
import org.tasks.scheduling.CalendarNotificationIntentService;
import org.tasks.scheduling.NotificationSchedulerIntentService;

@Subcomponent(modules = ServiceModule.class)
public interface ServiceComponent {

  void inject(CalendarNotificationIntentService calendarNotificationIntentService);

  void inject(GeofenceTransitionsIntentService geofenceTransitionsIntentService);

  void inject(NotificationSchedulerIntentService notificationSchedulerIntentService);

  void inject(TaskerIntentService taskerIntentService);

  void inject(NotificationService notificationService);

  void inject(RefreshReceiver refreshReceiver);
}
