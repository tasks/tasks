package org.tasks.injection;

import org.tasks.widget.ScrollableWidgetUpdateService;

import dagger.Subcomponent;

@Subcomponent(modules = RemoteViewsServiceModule.class)
public interface RemoteViewsServiceComponent {
    void inject(ScrollableWidgetUpdateService scrollableWidgetUpdateService);
}
