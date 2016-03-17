package org.tasks.injection;

import org.tasks.widget.ScrollableWidgetUpdateService;

import dagger.Subcomponent;

public interface BaseServiceComponent {
    void inject(ScrollableWidgetUpdateService scrollableWidgetUpdateService);
}
