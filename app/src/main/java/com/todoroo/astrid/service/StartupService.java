/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.service;

import android.content.Context;
import dagger.Lazy;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.analytics.Tracker;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import org.tasks.scheduling.BackgroundScheduler;
import timber.log.Timber;

public class StartupService {

  private final Preferences preferences;
  private final Context context;
  private final Lazy<Upgrader> upgrader;

  @Inject
  public StartupService(
      Preferences preferences,
      Tracker tracker,
      @ForApplication Context context,
      Lazy<Upgrader> upgrader) {
    this.preferences = preferences;
    this.context = context;
    this.upgrader = upgrader;
  }

  /** Called when this application is started up */
  public synchronized void onStartupApplication() {
    // read current version
    final int lastVersion = preferences.getLastSetVersion();
    final int currentVersion = BuildConfig.VERSION_CODE;

    Timber.i("Astrid Startup. %s => %s", lastVersion, currentVersion);

    // invoke upgrade service
    if (lastVersion != currentVersion) {
      upgrader.get().upgrade(lastVersion, currentVersion);
      preferences.setDefaults();
    }

    BackgroundScheduler.enqueueWork(context);
  }
}
