package org.tasks.injection;

import org.tasks.dashclock.DashClockExtension;

import dagger.Component;

@ApplicationScope
@Component(modules = ApplicationModule.class)
public interface ApplicationComponent extends BaseApplicationComponent {
    SyncAdapterComponent plus(SyncAdapterModule syncAdapterModule);

    void inject(DashClockExtension dashClockExtension);
}
