package org.tasks;

import com.evernote.android.job.JobManager;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.todoroo.astrid.service.StartupService;
import javax.inject.Inject;
import org.tasks.analytics.Tracker;
import org.tasks.injection.ApplicationComponent;
import org.tasks.injection.InjectingApplication;
import org.tasks.jobs.JobCreator;
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

  @Override
  public void onCreate() {
    super.onCreate();

    tracker.setTrackingEnabled(preferences.isTrackingEnabled());

    if (!buildSetup.setup()) {
      return;
    }

    AndroidThreeTen.init(this);

    flavorSetup.setup();

    badger.setEnabled(preferences.getBoolean(R.string.p_badges_enabled, true));

    themeCache.getThemeBase(preferences.getInt(R.string.p_theme, 0)).setDefaultNightMode();

    startupService.onStartupApplication();

    jobManager.addJobCreator(jobCreator);
  }

  @Override
  protected void inject(ApplicationComponent component) {
    component.inject(this);
  }
}
