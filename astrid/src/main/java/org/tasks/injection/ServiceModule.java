package org.tasks.injection;

import com.todoroo.astrid.backup.BackupService;
import com.todoroo.astrid.gtasks.GtasksBackgroundService;
import com.todoroo.astrid.reminders.ReminderSchedulingService;
import com.todoroo.astrid.widget.WidgetUpdateService;

import org.tasks.widget.ScrollableWidgetUpdateService;

import dagger.Module;

@Module(addsTo = TasksModule.class,
        injects = {
        GtasksBackgroundService.class,
        ReminderSchedulingService.class,
        ScrollableWidgetUpdateService.class,
        WidgetUpdateService.class,
        BackupService.class
})
public class ServiceModule {
}
