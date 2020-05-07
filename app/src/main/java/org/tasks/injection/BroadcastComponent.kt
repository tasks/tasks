package org.tasks.injection

import com.todoroo.astrid.gcal.CalendarAlarmReceiver
import dagger.Subcomponent
import org.tasks.notifications.NotificationClearedReceiver
import org.tasks.receivers.BootCompletedReceiver
import org.tasks.receivers.CompleteTaskReceiver
import org.tasks.receivers.MyPackageReplacedReceiver
import org.tasks.widget.TasksWidget

@Subcomponent(modules = [BroadcastModule::class])
interface BroadcastComponent {
    fun inject(broadcastReceiver: CalendarAlarmReceiver)
    fun inject(broadcastReceiver: MyPackageReplacedReceiver)
    fun inject(broadcastReceiver: CompleteTaskReceiver)
    fun inject(broadcastReceiver: BootCompletedReceiver)
    fun inject(broadcastReceiver: TasksWidget)
    fun inject(broadcastReceiver: NotificationClearedReceiver)
}