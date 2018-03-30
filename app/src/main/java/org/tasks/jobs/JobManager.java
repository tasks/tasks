package org.tasks.jobs;

import static android.text.TextUtils.isEmpty;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.time.DateTimeUtils.printTimestamp;

import com.evernote.android.job.DailyJob;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.JobRequest.Builder;
import com.evernote.android.job.JobRequest.NetworkType;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.tasks.R;
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

  @Inject
  public JobManager(com.evernote.android.job.JobManager jobManager, Preferences preferences) {
    this.jobManager = jobManager;
    this.preferences = preferences;
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
    DailyJob.schedule(new Builder(JobCreator.TAG_BACKUP), 0,
        TimeUnit.HOURS.toMillis(24) - 1);
  }

  public void updateBackgroundSync() {
    updateBackgroundSync(false, false);
  }

  public void updateBackgroundSync(boolean forceAccountPresent, boolean forceBackgroundEnabled) {
    boolean backgroundEnabled =
        forceBackgroundEnabled || preferences.getBoolean(R.string.p_background_sync, true);
    boolean accountsPresent =
        forceAccountPresent || preferences.getBoolean(R.string.p_sync_caldav, false) ||
            !isEmpty(preferences.getStringValue(GtasksPreferenceService.PREF_USER_NAME));
    scheduleBackgroundSynchronization(backgroundEnabled && accountsPresent);
  }

  private void scheduleBackgroundSynchronization(boolean enabled) {
    Timber.d("background sync enabled: %s", enabled);
    if (enabled) {
      new JobRequest.Builder(JobCreator.TAG_SYNC)
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .setRequirementsEnforced(true)
          .setPeriodic(TimeUnit.HOURS.toMillis(1))
          .setUpdateCurrent(true)
          .build()
          .schedule();
    } else {
      jobManager.cancelAllForTag(JobCreator.TAG_SYNC);
    }
  }

  public void syncNow() {
    new JobRequest.Builder(JobCreator.TAG_SYNC)
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequirementsEnforced(true)
        .setExecutionWindow(1, 5000)
        .build()
        .schedule();
  }

  public void cancelNotifications() {
    Timber.d("cancelNotifications");
    jobManager.cancelAllForTag(JobCreator.TAG_NOTIFICATION);
  }

  public void cancelRefresh() {
    Timber.d("cancelRefresh");
    jobManager.cancelAllForTag(JobCreator.TAG_REFRESH);
  }

  private long calculateDelay(long time) {
    return Math.max(5000, time - currentTimeMillis());
  }

  public void addJobCreator(JobCreator jobCreator) {
    jobManager.addJobCreator(jobCreator);
  }
}
