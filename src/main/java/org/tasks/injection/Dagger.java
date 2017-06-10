package org.tasks.injection;

import android.content.Context;

import org.tasks.locale.Locale;

import timber.log.Timber;

class Dagger {

    private static final Object lock = new Object();

    private static Dagger instance;

    public static Dagger get(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Dagger(context);
                }
            }
        }
        return instance;
    }

    private ApplicationComponent applicationComponent;

    private Dagger(Context context) {
        Context localeContext = context.getApplicationContext();
        try {
            localeContext = Locale.getInstance(localeContext)
                    .createConfigurationContext(localeContext);
        } catch (Exception e) {
            Timber.e(e.getMessage(), e);
        }

        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(localeContext))
                .build();
    }

    ApplicationComponent getApplicationComponent() {
        return applicationComponent;
    }
}
