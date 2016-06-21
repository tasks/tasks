package org.tasks.injection;

import android.support.multidex.MultiDexApplication;

public abstract class InjectingApplication extends MultiDexApplication {

    private ApplicationComponent applicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();

        inject(applicationComponent);
    }

    protected abstract void inject(ApplicationComponent component);

    public ApplicationComponent getComponent() {
        return applicationComponent;
    }


}
