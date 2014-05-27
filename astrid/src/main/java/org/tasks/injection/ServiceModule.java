package org.tasks.injection;

import com.todoroo.astrid.gtasks.GtasksBackgroundService;
import com.todoroo.astrid.reminders.ReminderSchedulingService;

import org.tasks.widget.ScrollableWidgetUpdateService;

import dagger.Module;

@Module(library = true,
        injects = {
                GtasksBackgroundService.class,
                ReminderSchedulingService.class,
                ScrollableWidgetUpdateService.class
        })
public class ServiceModule {
}
