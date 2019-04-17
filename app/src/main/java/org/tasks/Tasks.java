package org.tasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.JobIntentService;
import com.jakewharton.processphoenix.ProcessPhoenix;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.todoroo.astrid.service.Upgrader;
import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import org.tasks.billing.BillingClient;
import org.tasks.files.FileHelper;
import org.tasks.injection.ApplicationComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingApplication;
import org.tasks.injection.InjectingJobIntentService;
import org.tasks.jobs.WorkManager;
import org.tasks.location.GeofenceApi;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.RefreshReceiver;
import org.tasks.scheduling.CalendarNotificationIntentService;
import org.tasks.scheduling.NotificationSchedulerIntentService;
import org.tasks.scheduling.RefreshScheduler;
import org.tasks.themes.ThemeCache;
import timber.log.Timber;

public class Tasks extends InjectingApplication {

  @Inject @ForApplication Context context;
  @Inject Lazy<Upgrader> upgrader;
  @Inject Preferences preferences;
  @Inject BuildSetup buildSetup;
  @Inject ThemeCache themeCache;
  @Inject WorkManager workManager;
  @Inject RefreshScheduler refreshScheduler;
  @Inject GeofenceApi geofenceApi;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject BillingClient billingClient;

  @Override
  public void onCreate() {
    super.onCreate();

    if (!buildSetup.setup() || ProcessPhoenix.isPhoenixProcess(this)) {
      return;
    }

    upgrade();

    workManager.init();

    AndroidThreeTen.init(this);

    preferences.setSyncOngoing(false);

    themeCache.getThemeBase(preferences.getInt(R.string.p_theme, 0)).setDefaultNightMode();

    localBroadcastManager.registerRefreshReceiver(new RefreshBroadcastReceiver());

    Completable.fromAction(this::doInBackground).subscribeOn(Schedulers.io()).subscribe();
  }

  private void upgrade() {
    final int lastVersion = preferences.getLastSetVersion();
    final int currentVersion = BuildConfig.VERSION_CODE;

    Timber.i("Astrid Startup. %s => %s", lastVersion, currentVersion);

    // invoke upgrade service
    if (lastVersion != currentVersion) {
      upgrader.get().upgrade(lastVersion, currentVersion);
      preferences.setDefaults();
    }
  }

  private void doInBackground() {
    NotificationSchedulerIntentService.enqueueWork(context, false);
    CalendarNotificationIntentService.enqueueWork(context);
    refreshScheduler.scheduleAll();
    workManager.updateBackgroundSync();
    workManager.scheduleMidnightRefresh();
    workManager.scheduleBackup();
    geofenceApi.registerAll();
    FileHelper.delete(context, preferences.getCacheDirectory());
    billingClient.queryPurchases();
  }

  @Override
  protected void inject(ApplicationComponent component) {
    component.inject(this);
  }

  private class RefreshBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      JobIntentService.enqueueWork(
          context,
          RefreshReceiver.class,
          InjectingJobIntentService.JOB_ID_REFRESH_RECEIVER,
          intent);
    }
  }
}
