package org.tasks.injection;

import org.tasks.widget.ScrollableWidgetUpdateService;

import dagger.Module;

@Module(addsTo = TasksModule.class,
        injects = {
        ScrollableWidgetUpdateService.class
})
public class ServiceModule {
}
