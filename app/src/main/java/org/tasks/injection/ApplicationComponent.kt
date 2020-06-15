package org.tasks.injection

import com.todoroo.astrid.gcal.CalendarAlarmReceiver
import com.todoroo.astrid.provider.Astrid2TaskProvider
import dagger.Component
import org.tasks.Tasks
import org.tasks.backup.TasksBackupAgent
import org.tasks.dashclock.DashClockExtension
import org.tasks.jobs.*
import org.tasks.locale.receiver.TaskerIntentService
import org.tasks.location.GeofenceTransitionsIntentService
import org.tasks.notifications.NotificationClearedReceiver
import org.tasks.receivers.BootCompletedReceiver
import org.tasks.receivers.CompleteTaskReceiver
import org.tasks.receivers.MyPackageReplacedReceiver
import org.tasks.receivers.RefreshReceiver
import org.tasks.scheduling.CalendarNotificationIntentService
import org.tasks.scheduling.NotificationSchedulerIntentService
import org.tasks.widget.ScrollableWidgetUpdateService
import org.tasks.widget.TasksWidget
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class, ProductionModule::class])
interface ApplicationComponent {
    operator fun plus(module: ActivityModule): ActivityComponent
    fun inject(dashClockExtension: DashClockExtension)
    fun inject(tasks: Tasks)
    fun inject(scrollableWidgetUpdateService: ScrollableWidgetUpdateService)
    fun inject(tasksBackupAgent: TasksBackupAgent)

    fun inject(broadcastReceiver: CalendarAlarmReceiver)
    fun inject(broadcastReceiver: MyPackageReplacedReceiver)
    fun inject(broadcastReceiver: CompleteTaskReceiver)
    fun inject(broadcastReceiver: BootCompletedReceiver)
    fun inject(broadcastReceiver: TasksWidget)
    fun inject(broadcastReceiver: NotificationClearedReceiver)

    fun inject(contentProvider: Astrid2TaskProvider)

    fun inject(service: CalendarNotificationIntentService)
    fun inject(service: GeofenceTransitionsIntentService)
    fun inject(service: NotificationSchedulerIntentService)
    fun inject(service: TaskerIntentService)
    fun inject(service: NotificationService)
    fun inject(service: RefreshReceiver)

    fun inject(work: SyncWork)
    fun inject(work: BackupWork)
    fun inject(work: RefreshWork)
    fun inject(work: CleanupWork)
    fun inject(work: MidnightRefreshWork)
    fun inject(work: AfterSaveWork)
    fun inject(work: DriveUploader)
    fun inject(work: ReverseGeocodeWork)
    fun inject(work: RemoteConfigWork)
}