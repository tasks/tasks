package org.tasks.injection;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

import org.tasks.locale.Locale;

public abstract class InjectingApplication extends MultiDexApplication {

    private ApplicationComponent applicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        Context context = Locale.getInstance(this).createConfigurationContext(getApplicationContext());

        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(context))
                .build();

        inject(applicationComponent);
    }

    protected abstract void inject(ApplicationComponent component);

    public ApplicationComponent getComponent() {
        return applicationComponent;
    }
}
