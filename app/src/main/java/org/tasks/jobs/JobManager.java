package org.tasks.jobs;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.time.DateTimeUtils.nextMidnight;
import static org.tasks.time.DateTimeUtils.printTimestamp;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.evernote.android.job.DailyJob;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.JobRequest.Builder;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.scheduling.AlarmManager;
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
  static final int JOB_ID_REFRESH = 1;
  static final int JOB_ID_MIDNIGHT_REFRESH = 6;
  private final Context context;
  private final AlarmManager alarmManager;
  private final com.evernote.android.job.JobManager jobManager;

  @Inject
  public JobManager(@ForApplication Context context, AlarmManager alarmManager,
      com.evernote.android.job.JobManager jobManager) {
    this.context = context;
    this.alarmManager = alarmManager;
    this.jobManager = jobManager;
  }

  public void scheduleNotification(long time) {
    Timber.d("schedule notification: %s", printTimestamp(time));
    new JobRequest.Builder(NotificationJob.TAG)
        .setExact(calculateDelay(time))
        .build()
        .schedule();
  }

  public void scheduleRefresh(long time) {
    Timber.d("%s: %s", RefreshJob.TAG, printTimestamp(time));
    alarmManager.noWakeup(adjust(time), getPendingBroadcast(RefreshJob.Broadcast.class));
  }

  public void scheduleMidnightRefresh() {
    long time = nextMidnight();
    Timber.d("%s: %s", MidnightRefreshJob.TAG, printTimestamp(time));
    alarmManager.noWakeup(adjust(time), getPendingBroadcast(MidnightRefreshJob.Broadcast.class));
  }

  public void scheduleBackup() {
    DailyJob.schedule(new Builder(BackupJob.TAG), 0, TimeUnit.HOURS.toMillis(24) - 1);
  }

  public void cancelNotifications() {
    Timber.d("cancelNotifications");
    jobManager.cancelAllForTag(NotificationJob.TAG);
  }

  public void cancelRefresh() {
    Timber.d("cancelRefresh");
    alarmManager.cancel(getPendingIntent(RefreshJob.TAG));
  }

  private long calculateDelay(long time) {
    return Math.max(5000, time - currentTimeMillis());
  }

  private long adjust(long time) {
    return Math.max(time, currentTimeMillis() + 5000);
  }

  private PendingIntent getPendingIntent(String tag) {
    switch (tag) {
      case RefreshJob.TAG:
        return getPendingBroadcast(RefreshJob.Broadcast.class);
      default:
        throw new RuntimeException("Unexpected tag: " + tag);
    }
  }

  private <T> PendingIntent getPendingBroadcast(Class<T> c) {
    return PendingIntent.getBroadcast(context, 0, new Intent(context, c), 0);
  }
}
