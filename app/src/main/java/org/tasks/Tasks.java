package org.tasks;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.todoroo.astrid.service.StartupService;

import org.tasks.analytics.Tracker;
import org.tasks.injection.ApplicationComponent;
import org.tasks.injection.InjectingApplication;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.TeslaUnreadReceiver;
import org.tasks.themes.ThemeCache;

import javax.inject.Inject;

public class Tasks extends InjectingApplication {

    @Inject StartupService startupService;
    @Inject Preferences preferences;
    @Inject Tracker tracker;
    @Inject FlavorSetup flavorSetup;
    @Inject BuildSetup buildSetup;
    @Inject ThemeCache themeCache;
    @Inject TeslaUnreadReceiver teslaUnreadReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        tracker.setTrackingEnabled(preferences.isTrackingEnabled());

        if (!buildSetup.setup()) {
            return;
        }

        AndroidThreeTen.init(this);

        flavorSetup.setup();

        teslaUnreadReceiver.setEnabled(preferences.getBoolean(R.string.p_tesla_unread_enabled, false));

        themeCache.getThemeBase(preferences.getInt(R.string.p_theme, 0)).setDefaultNightMode();

        startupService.onStartupApplication();
    }

    @Override
    protected void inject(ApplicationComponent component) {
        component.inject(this);
    }
}
