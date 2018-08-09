package org.tasks.jobs;

import static com.todoroo.andlib.utility.DateUtilities.now;
import static org.tasks.date.DateTimeUtils.midnight;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.time.DateTimeUtils.printTimestamp;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OneTimeWorkRequest.Builder;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import com.google.common.primitives.Longs;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.injection.ApplicationScope;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

@ApplicationScope
public class WorkManager {

  private static final String TAG_BACKUP = "tag_backup";
  private static final String TAG_REFRESH = "tag_refresh";
  private static final String TAG_MIDNIGHT_REFRESH = "tag_midnight_refresh";
  private static final String TAG_NOTIFICATION = "tag_notification";
  private static final String TAG_SYNC = "tag_sync";
  private static final String TAG_BACKGROUND_SYNC = "tag_background_sync";

  private final Preferences preferences;
  private final GoogleTaskListDao googleTaskListDao;
  private final CaldavDao caldavDao;
  private androidx.work.WorkManager workManager;

  @Inject
  public WorkManager(
      Preferences preferences, GoogleTaskListDao googleTaskListDao, CaldavDao caldavDao) {
    this.preferences = preferences;
    this.googleTaskListDao = googleTaskListDao;
    this.caldavDao = caldavDao;
  }

  public void init() {
    workManager = androidx.work.WorkManager.getInstance();
  }

  public void cleanup(List<Long> ids) {
    workManager.enqueue(
        new OneTimeWorkRequest.Builder(CleanupWork.class)
            .setInputData(
                new Data.Builder()
                    .putLongArray(CleanupWork.EXTRA_TASK_IDS, Longs.toArray(ids))
                    .build())
            .build());
  }

  public void syncNow() {
    Constraints constraints =
        new Constraints.Builder()
            .setRequiredNetworkType(
                preferences.getBoolean(R.string.p_background_sync_unmetered_only, false)
                    ? NetworkType.UNMETERED
                    : NetworkType.CONNECTED)
            .build();
    OneTimeWorkRequest request =
        new OneTimeWorkRequest.Builder(SyncWork.class)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build();
    workManager.beginUniqueWork(TAG_SYNC, ExistingWorkPolicy.KEEP, request).enqueue();
  }

  public void updateBackgroundSync() {
    updateBackgroundSync(null, null, null);
  }

  public void updateBackgroundSync(
      @Nullable Boolean forceAccountPresent,
      @Nullable Boolean forceBackgroundEnabled,
      @Nullable Boolean forceOnlyOnUnmetered) {
    boolean backgroundEnabled =
        forceBackgroundEnabled == null
            ? preferences.getBoolean(R.string.p_background_sync, true)
            : forceBackgroundEnabled;
    boolean accountsPresent =
        forceAccountPresent == null
            ? (googleTaskListDao.getAccounts().size() > 0 || caldavDao.getAccounts().size() > 0)
            : forceAccountPresent;
    boolean onlyOnWifi =
        forceOnlyOnUnmetered == null
            ? preferences.getBoolean(R.string.p_background_sync_unmetered_only, false)
            : forceOnlyOnUnmetered;
    scheduleBackgroundSynchronization(backgroundEnabled && accountsPresent, onlyOnWifi);
  }

  private void scheduleBackgroundSynchronization(boolean enabled, boolean onlyOnUnmetered) {
    Timber.d("background sync enabled: %s, onlyOnUnmetered: %s", enabled, onlyOnUnmetered);
    if (enabled) {
      workManager.enqueueUniquePeriodicWork(
          TAG_BACKGROUND_SYNC,
          ExistingPeriodicWorkPolicy.KEEP,
          new PeriodicWorkRequest.Builder(SyncWork.class, 1, TimeUnit.HOURS)
              .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
              .setConstraints(
                  new Constraints.Builder()
                      .setRequiredNetworkType(
                          onlyOnUnmetered ? NetworkType.UNMETERED : NetworkType.CONNECTED)
                      .build())
              .build());
    } else {
      workManager.cancelUniqueWork(TAG_BACKGROUND_SYNC);
    }
  }

  public void scheduleRefresh(long time) {
    enqueueUnique(TAG_REFRESH, RefreshWork.class, time);
  }

  void scheduleMidnightRefresh() {
    enqueueUnique(TAG_MIDNIGHT_REFRESH, MidnightRefreshWork.class, midnight());
  }

  void scheduleNotification(long time) {
    enqueueUnique(TAG_NOTIFICATION, NotificationWork.class, time);
  }

  void scheduleBackup() {
    long lastBackup = preferences.getLong(R.string.p_last_backup, 0L);
    enqueueUnique(
        TAG_BACKUP,
        BackupWork.class,
        Math.min(newDateTime(lastBackup).plusDays(1).getMillis(), midnight()));
  }

  private void enqueueUnique(String key, Class<? extends Worker> c, long time) {
    long delay = time - now();
    OneTimeWorkRequest.Builder builder = new Builder(c);
    if (delay > 0) {
      builder.setInitialDelay(delay, TimeUnit.MILLISECONDS);
    }
    Timber.d("%s: %s (%sms)", key, printTimestamp(time), delay);
    workManager.beginUniqueWork(key, ExistingWorkPolicy.REPLACE, builder.build()).enqueue();
  }

  void cancelNotifications() {
    Timber.d("cancelNotifications");
    workManager.cancelAllWorkByTag(TAG_NOTIFICATION);
  }

  public void onStartup() {
    updateBackgroundSync();
    scheduleMidnightRefresh();
    scheduleBackup();
  }
}
