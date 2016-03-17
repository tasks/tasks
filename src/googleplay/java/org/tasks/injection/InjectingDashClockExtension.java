package org.tasks.injection;

import com.google.android.apps.dashclock.api.DashClockExtension;

public abstract class InjectingDashClockExtension extends DashClockExtension {
    @Override
    public void onCreate() {
        super.onCreate();

        inject(((InjectingApplication) getApplication())
                .getComponent()
                .plus(new ServiceModule()));
    }

    protected abstract void inject(ServiceComponent component);
}
