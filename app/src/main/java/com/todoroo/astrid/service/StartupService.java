/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.service;

import android.content.Context;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.tags.TagService;
import dagger.Lazy;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.LocalBroadcastManager;
import org.tasks.analytics.Tracker;
import org.tasks.data.FilterDao;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.TagDao;
import org.tasks.data.TagDataDao;
import org.tasks.data.TaskAttachmentDao;
import org.tasks.data.UserActivityDao;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.scheduling.BackgroundScheduler;
import timber.log.Timber;

public class StartupService {

  private final Database database;
  private final Preferences preferences;
  private final Context context;
  private final Lazy<Upgrader> upgrader;

  @Inject
  public StartupService(
      Database database,
      Preferences preferences,
      Tracker tracker,
      TagDataDao tagDataDao,
      TagService tagService,
      LocalBroadcastManager localBroadcastManager,
      @ForApplication Context context,
      TagDao tagDao,
      FilterDao filterDao,
      DefaultFilterProvider defaultFilterProvider,
      GoogleTaskListDao googleTaskListDao,
      UserActivityDao userActivityDao,
      TaskAttachmentDao taskAttachmentDao,
      Lazy<Upgrader> upgrader) {
    this.database = database;
    this.preferences = preferences;
    this.context = context;
    this.upgrader = upgrader;
  }

  /** Called when this application is started up */
  public synchronized void onStartupApplication() {
    database.openForWriting();

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
