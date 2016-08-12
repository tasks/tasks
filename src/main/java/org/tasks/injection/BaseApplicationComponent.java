package org.tasks.injection;

import org.tasks.Tasks;
import org.tasks.widget.ScrollableWidgetUpdateService;

public interface BaseApplicationComponent {
    void inject(Tasks tasks);

    void inject(ScrollableWidgetUpdateService scrollableWidgetUpdateService);

    ActivityComponent plus(ActivityModule module);

    BroadcastComponent plus(BroadcastModule module);

    IntentServiceComponent plus(IntentServiceModule module);
}
