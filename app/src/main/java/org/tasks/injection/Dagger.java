package org.tasks.injection;

import android.content.Context;
import org.tasks.locale.Locale;
import timber.log.Timber;

class Dagger {

  private static final Object lock = new Object();

  private static Dagger instance;
  private final ApplicationComponent applicationComponent;

  private Dagger(Context context) {
    Context localeContext = context.getApplicationContext();
    try {
      localeContext = Locale.getInstance(localeContext).createConfigurationContext(localeContext);
    } catch (Exception e) {
      Timber.e(e);
    }

    applicationComponent =
        DaggerApplicationComponent.builder()
            .applicationModule(new ApplicationModule(localeContext))
            .build();
  }

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

  ApplicationComponent getApplicationComponent() {
    return applicationComponent;
  }
}
