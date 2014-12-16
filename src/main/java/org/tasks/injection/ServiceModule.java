package org.tasks.injection;

import com.todoroo.astrid.gtasks.GtasksBackgroundService;
import com.todoroo.astrid.widget.WidgetUpdateService;

import org.tasks.widget.ScrollableWidgetUpdateService;

import dagger.Module;

@Module(addsTo = TasksModule.class,
        injects = {
        GtasksBackgroundService.class,
        ScrollableWidgetUpdateService.class,
        WidgetUpdateService.class
})
public class ServiceModule {
}
