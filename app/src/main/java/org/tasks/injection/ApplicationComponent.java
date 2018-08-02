package org.tasks.injection;

import dagger.Component;
import org.tasks.Tasks;
import org.tasks.dashclock.DashClockExtension;
import org.tasks.widget.ScrollableWidgetUpdateService;

@ApplicationScope
@Component(modules = ApplicationModule.class)
public interface ApplicationComponent {

  void inject(DashClockExtension dashClockExtension);

  void inject(Tasks tasks);

  void inject(ScrollableWidgetUpdateService scrollableWidgetUpdateService);

  ActivityComponent plus(ActivityModule module);

  BroadcastComponent plus(BroadcastModule module);

  IntentServiceComponent plus(IntentServiceModule module);

  JobComponent plus(WorkModule module);
}
