package org.tasks;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.service.StartupService;
import javax.inject.Inject;
import org.tasks.analytics.Tracker;
import org.tasks.injection.ApplicationComponent;
import org.tasks.injection.InjectingApplication;
import org.tasks.jobs.JobCreator;
import org.tasks.jobs.JobManager;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.Badger;
import org.tasks.themes.ThemeCache;

public class Tasks extends InjectingApplication {

  @Inject StartupService startupService;
  @Inject Preferences preferences;
  @Inject Tracker tracker;
  @Inject FlavorSetup flavorSetup;
  @Inject BuildSetup buildSetup;
  @Inject ThemeCache themeCache;
  @Inject Badger badger;
  @Inject JobManager jobManager;
  @Inject JobCreator jobCreator;
  @Inject GtasksPreferenceService gtasksPreferenceService;

  @Override
  public void onCreate() {
    super.onCreate();

    tracker.setTrackingEnabled(preferences.isTrackingEnabled());

    if (!buildSetup.setup()) {
      return;
    }

    AndroidThreeTen.init(this);

    preferences.setSyncOngoing(false);

    jobManager.addJobCreator(jobCreator);

    flavorSetup.setup();

    badger.setEnabled(preferences.getBoolean(R.string.p_badges_enabled, true));

    themeCache.getThemeBase(preferences.getInt(R.string.p_theme, 0)).setDefaultNightMode();

    startupService.onStartupApplication();

    jobManager.updateBackgroundSync();
    jobManager.scheduleMidnightRefresh();
    jobManager.scheduleBackup();
  }

  @Override
  protected void inject(ApplicationComponent component) {
    component.inject(this);
  }
}
