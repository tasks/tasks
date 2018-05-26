package org.tasks.jobs;

import static org.tasks.jobs.CleanupJob.EXTRA_TASK_IDS;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.time.DateTimeUtils.printTimestamp;

import com.evernote.android.job.DailyJob;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.JobRequest.Builder;
import com.evernote.android.job.JobRequest.NetworkType;
import com.evernote.android.job.util.support.PersistableBundleCompat;
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
public class JobManager {

  public static final int JOB_ID_BACKGROUND_SCHEDULER = 2;
  public static final int JOB_ID_GEOFENCE_TRANSITION = 4;
  public static final int JOB_ID_GEOFENCE_SCHEDULING = 5;
  public static final int JOB_ID_TASK_STATUS_CHANGE = 8;
  public static final int JOB_ID_NOTIFICATION_SCHEDULER = 9;
  public static final int JOB_ID_CALENDAR_NOTIFICATION = 10;
  public static final int JOB_ID_TASKER = 11;

  private final com.evernote.android.job.JobManager jobManager;
  private final Preferences preferences;
  private final CaldavDao caldavDao;
  private final GoogleTaskListDao googleTaskListDao;

  @Inject
  public JobManager(
      com.evernote.android.job.JobManager jobManager,
      Preferences preferences,
      CaldavDao caldavDao,
      GoogleTaskListDao googleTaskListDao) {
    this.jobManager = jobManager;
    this.preferences = preferences;
    this.caldavDao = caldavDao;
    this.googleTaskListDao = googleTaskListDao;
  }

  public void cleanup(List<Long> ids) {
    PersistableBundleCompat extras = new PersistableBundleCompat();
    extras.putLongArray(EXTRA_TASK_IDS, Longs.toArray(ids));
    new JobRequest.Builder(JobCreator.TAG_CLEANUP).setExtras(extras).startNow().build().schedule();
  }

  public void scheduleNotification(long time) {
    Timber.d("schedule notification: %s", printTimestamp(time));
    new JobRequest.Builder(JobCreator.TAG_NOTIFICATION)
        .setExact(calculateDelay(time))
        .setUpdateCurrent(true)
        .build()
        .schedule();
  }

  public void scheduleRefresh(long time) {
    Timber.d("schedule refresh: %s", printTimestamp(time));
    new JobRequest.Builder(JobCreator.TAG_REFRESH)
        .setExact(calculateDelay(time))
        .setUpdateCurrent(true)
        .build()
        .schedule();
  }

  public void scheduleMidnightRefresh() {
    DailyJob.schedule(new Builder(JobCreator.TAG_MIDNIGHT_REFRESH), 0, 0);
  }

  public void scheduleBackup() {
    DailyJob.schedule(new Builder(JobCreator.TAG_BACKUP), 0, TimeUnit.HOURS.toMillis(24) - 1);
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
      new JobRequest.Builder(JobCreator.TAG_BACKGROUND_SYNC)
          .setPeriodic(TimeUnit.HOURS.toMillis(1))
          .setRequiredNetworkType(onlyOnUnmetered ? NetworkType.UNMETERED : NetworkType.CONNECTED)
          .setRequirementsEnforced(true)
          .setUpdateCurrent(true)
          .build()
          .schedule();
    } else {
      jobManager.cancelAllForTag(JobCreator.TAG_BACKGROUND_SYNC);
    }
  }

  public void syncNow() {
    new JobRequest.Builder(JobCreator.TAG_SYNC)
        .setUpdateCurrent(true)
        .startNow()
        .build()
        .schedule();
  }

  public void cancelNotifications() {
    Timber.d("cancelNotifications");
    jobManager.cancelAllForTag(JobCreator.TAG_NOTIFICATION);
  }

  private long calculateDelay(long time) {
    return Math.max(5000, time - currentTimeMillis());
  }

  public void addJobCreator(JobCreator jobCreator) {
    jobManager.addJobCreator(jobCreator);
  }
}
