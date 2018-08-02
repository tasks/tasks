package org.tasks.jobs;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.time.DateTimeUtils.printTimestamp;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
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
import org.tasks.time.DateTime;
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
    workManager.enqueue(
        new OneTimeWorkRequest.Builder(SyncWork.class)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .addTag(TAG_SYNC)
            .build());
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
    cancelAllForTag(TAG_BACKGROUND_SYNC);
    Timber.d("background sync enabled: %s, onlyOnUnmetered: %s", enabled, onlyOnUnmetered);
    if (enabled) {
      workManager.enqueue(
          new PeriodicWorkRequest.Builder(SyncWork.class, 1, TimeUnit.HOURS)
              .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
              .setConstraints(
                  new Constraints.Builder()
                      .setRequiredNetworkType(
                          onlyOnUnmetered ? NetworkType.UNMETERED : NetworkType.CONNECTED)
                      .build())
              .build());
    }
  }

  public void scheduleRefresh(long time) {
    enqueue(RefreshWork.class, time, TAG_REFRESH);
  }

  void scheduleMidnightRefresh() {
    enqueue(
        MidnightRefreshWork.class,
        new DateTime(currentTimeMillis()).plusDays(1).startOfDay().getMillis(),
        TAG_MIDNIGHT_REFRESH);
  }

  void scheduleNotification(long time, boolean cancelCurrent) {
    if (cancelCurrent) {
      cancelNotifications();
    }
    enqueue(NotificationWork.class, time, TAG_NOTIFICATION);
  }

  void scheduleBackup() {
    enqueue(
        BackupWork.class,
        new DateTime(currentTimeMillis()).plusDays(1).startOfDay().getMillis(),
        TAG_BACKUP);
  }

  private void enqueue(Class<? extends Worker> c, long time, String tag) {
    long delay = calculateDelay(time);
    Timber.d("enqueue %s: %s (%sms)", tag, printTimestamp(time), delay);
    workManager.enqueue(
        new OneTimeWorkRequest.Builder(c)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(tag)
            .build());
  }

  public void cancelRefresh() {
    cancelAllForTag(TAG_REFRESH);
  }

  void cancelNotifications() {
    cancelAllForTag(TAG_NOTIFICATION);
  }

  private void cancelAllForTag(String tag) {
    Timber.d("cancelAllWorkByTag(%s)", tag);
    workManager.cancelAllWorkByTag(tag);
  }

  private long calculateDelay(long time) {
    return Math.max(5000, time - currentTimeMillis());
  }

  public void onStartup() {
    updateBackgroundSync();
    cancelAllForTag(TAG_MIDNIGHT_REFRESH);
    scheduleMidnightRefresh();
    cancelAllForTag(TAG_BACKUP);
    scheduleBackup();
  }
}
