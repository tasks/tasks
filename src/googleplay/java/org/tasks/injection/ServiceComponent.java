package org.tasks.injection;

import org.tasks.dashclock.DashClockExtension;

import dagger.Subcomponent;

@Subcomponent(modules = ServiceModule.class)
public interface ServiceComponent extends BaseServiceComponent {
    void inject(DashClockExtension dashClockExtension);
}
