package org.tasks.injection;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;

import org.tasks.R;
import org.tasks.locale.Locale;

public abstract class InjectingApplication extends MultiDexApplication {

    private ApplicationComponent applicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String language = prefs.getString(getString(R.string.p_language), null);
        int directionOverride = Integer.parseInt(prefs.getString(getString(R.string.p_layout_direction), "-1"));
        Locale.INSTANCE = new Locale(java.util.Locale.getDefault(), language, directionOverride);
        java.util.Locale.setDefault(Locale.INSTANCE.getLocale());
        Context context = Locale.INSTANCE.createConfigurationContext(getApplicationContext());

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
