package org.tasks.injection;

import com.todoroo.astrid.widget.WidgetUpdateService;

import org.tasks.widget.ScrollableWidgetUpdateService;

import dagger.Module;

@Module(addsTo = TasksModule.class,
        injects = {
        ScrollableWidgetUpdateService.class,
        WidgetUpdateService.class
})
public class ServiceModule {
}
