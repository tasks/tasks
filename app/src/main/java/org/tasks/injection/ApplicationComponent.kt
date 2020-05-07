package org.tasks.injection

import dagger.Component
import org.tasks.Tasks
import org.tasks.backup.TasksBackupAgent
import org.tasks.dashclock.DashClockExtension
import org.tasks.widget.ScrollableWidgetUpdateService

@ApplicationScope
@Component(modules = [ApplicationModule::class, ProductionModule::class])
interface ApplicationComponent {
    operator fun plus(module: ActivityModule): ActivityComponent
    operator fun plus(module: BroadcastModule): BroadcastComponent
    operator fun plus(module: ServiceModule): ServiceComponent
    operator fun plus(module: WorkModule): JobComponent
    fun inject(dashClockExtension: DashClockExtension)
    fun inject(tasks: Tasks)
    fun inject(scrollableWidgetUpdateService: ScrollableWidgetUpdateService)
    fun inject(tasksBackupAgent: TasksBackupAgent)
}