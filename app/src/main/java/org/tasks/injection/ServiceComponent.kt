package org.tasks.injection

import dagger.Subcomponent
import org.tasks.jobs.NotificationService
import org.tasks.locale.receiver.TaskerIntentService
import org.tasks.location.GeofenceTransitionsIntentService
import org.tasks.receivers.RefreshReceiver
import org.tasks.scheduling.CalendarNotificationIntentService
import org.tasks.scheduling.NotificationSchedulerIntentService

@Subcomponent(modules = [ServiceModule::class])
interface ServiceComponent {
    fun inject(service: CalendarNotificationIntentService)
    fun inject(service: GeofenceTransitionsIntentService)
    fun inject(service: NotificationSchedulerIntentService)
    fun inject(service: TaskerIntentService)
    fun inject(service: NotificationService)
    fun inject(service: RefreshReceiver)
}