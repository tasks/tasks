package org.tasks.injection;

import dagger.Component;
import org.tasks.Tasks;
import org.tasks.dashclock.DashClockExtension;
import org.tasks.widget.ScrollableWidgetUpdateService;

@ApplicationScope
@Component(modules = {ApplicationModule.class, ProductionModule.class})
public interface ApplicationComponent {

  void inject(DashClockExtension dashClockExtension);

  void inject(Tasks tasks);

  void inject(ScrollableWidgetUpdateService scrollableWidgetUpdateService);

  ActivityComponent plus(ActivityModule module);

  BroadcastComponent plus(BroadcastModule module);

  ServiceComponent plus(ServiceModule module);

  JobComponent plus(WorkModule module);
}
