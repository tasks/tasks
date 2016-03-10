package org.tasks.injection;

import android.app.Application;

public abstract class InjectingApplication extends Application {

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
