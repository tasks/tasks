package org.tasks;

import com.jakewharton.processphoenix.ProcessPhoenix;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.todoroo.astrid.service.StartupService;
import javax.inject.Inject;
import org.tasks.injection.ApplicationComponent;
import org.tasks.injection.InjectingApplication;
import org.tasks.jobs.WorkManager;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.Badger;
import org.tasks.themes.ThemeCache;

public class Tasks extends InjectingApplication {

  @Inject StartupService startupService;
  @Inject Preferences preferences;
  @Inject FlavorSetup flavorSetup;
  @Inject BuildSetup buildSetup;
  @Inject ThemeCache themeCache;
  @Inject Badger badger;
  @Inject WorkManager workManager;

  @Override
  public void onCreate() {
    super.onCreate();

    if (!buildSetup.setup() || ProcessPhoenix.isPhoenixProcess(this)) {
      return;
    }

    workManager.init();

    AndroidThreeTen.init(this);

    preferences.setSyncOngoing(false);

    flavorSetup.setup();

    badger.setEnabled(preferences.getBoolean(R.string.p_badges_enabled, true));

    themeCache.getThemeBase(preferences.getInt(R.string.p_theme, 0)).setDefaultNightMode();

    startupService.onStartupApplication();

    workManager.onStartup();
  }

  @Override
  protected void inject(ApplicationComponent component) {
    component.inject(this);
  }
}
