package org.tasks.jobs;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastKitKat;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastMarshmallow;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastOreo;
import static com.todoroo.andlib.utility.DateUtilities.now;
import static io.reactivex.Single.just;
import static io.reactivex.Single.zip;
import static org.tasks.date.DateTimeUtils.midnight;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.time.DateTimeUtils.printTimestamp;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.Nullable;
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
import com.todoroo.astrid.data.Task;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

@ApplicationScope
public class WorkManager {

  private static final String TAG_BACKUP = "tag_backup";
  private static final String TAG_REFRESH = "tag_refresh";
  private static final String TAG_MIDNIGHT_REFRESH = "tag_midnight_refresh";
  private static final String TAG_SYNC = "tag_sync";
  private static final String TAG_BACKGROUND_SYNC = "tag_background_sync";

  private final Context context;
  private final Preferences preferences;
  private final GoogleTaskListDao googleTaskListDao;
  private final CaldavDao caldavDao;
  private final AlarmManager alarmManager;
  private androidx.work.WorkManager workManager;

  @Inject
  public WorkManager(
      @ForApplication Context context,
      Preferences preferences,
      GoogleTaskListDao googleTaskListDao,
      CaldavDao caldavDao) {
    this.context = context;
    this.preferences = preferences;
    this.googleTaskListDao = googleTaskListDao;
    this.caldavDao = caldavDao;
    alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
  }

  public void init() {
    workManager = androidx.work.WorkManager.getInstance();
  }

  public void afterSave(Task current, Task original) {
    workManager.enqueue(
        new OneTimeWorkRequest.Builder(AfterSaveWork.class)
            .setInputData(AfterSaveWork.getInputData(current, original))
            .build());
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

  public void sync(boolean immediate) {
    Constraints constraints =
        new Constraints.Builder()
            .setRequiredNetworkType(
                !immediate && preferences.getBoolean(R.string.p_background_sync_unmetered_only, false)
                    ? NetworkType.UNMETERED
                    : NetworkType.CONNECTED)
            .build();
    Builder builder =
        new Builder(SyncWork.class)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .setConstraints(constraints);
    if (!immediate) {
      builder.setInitialDelay(1, TimeUnit.MINUTES);
    }
    OneTimeWorkRequest request = builder.build();
    workManager.beginUniqueWork(TAG_SYNC, ExistingWorkPolicy.REPLACE, request).enqueue();
  }

  public void updateBackgroundSync() {
    updateBackgroundSync(null, null, null);
  }

  @SuppressLint("CheckResult")
  public void updateBackgroundSync(
      @Nullable Boolean forceAccountPresent,
      @Nullable Boolean forceBackgroundEnabled,
      @Nullable Boolean forceOnlyOnUnmetered) {
    boolean backgroundEnabled =
        forceBackgroundEnabled == null
            ? preferences.getBoolean(R.string.p_background_sync, true)
            : forceBackgroundEnabled;
    boolean onlyOnWifi =
        forceOnlyOnUnmetered == null
            ? preferences.getBoolean(R.string.p_background_sync_unmetered_only, false)
            : forceOnlyOnUnmetered;

    //noinspection ResultOfMethodCallIgnored
    (forceAccountPresent == null
            ? zip(
                googleTaskListDao.accountCount(),
                caldavDao.accountCount(),
                (googleCount, caldavCount) -> googleCount > 0 || caldavCount > 0)
            : just(forceAccountPresent))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            accountsPresent ->
                scheduleBackgroundSync(backgroundEnabled && accountsPresent, onlyOnWifi));
  }

  private void scheduleBackgroundSync(boolean enabled, boolean onlyOnUnmetered) {
    Timber.d("background sync enabled: %s, onlyOnUnmetered: %s", enabled, onlyOnUnmetered);
    if (enabled) {
      workManager.enqueueUniquePeriodicWork(
          TAG_BACKGROUND_SYNC,
          ExistingPeriodicWorkPolicy.KEEP,
          new PeriodicWorkRequest.Builder(SyncWork.class, 1, TimeUnit.HOURS)
              .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
              .setConstraints(getNetworkConstraints(onlyOnUnmetered))
              .build());
    } else {
      workManager.cancelUniqueWork(TAG_BACKGROUND_SYNC);
    }
  }

  public void scheduleRefresh(long time) {
    enqueueUnique(TAG_REFRESH, RefreshWork.class, time);
  }

  public void scheduleMidnightRefresh() {
    enqueueUnique(TAG_MIDNIGHT_REFRESH, MidnightRefreshWork.class, midnight());
  }

  @SuppressWarnings("WeakerAccess")
  public void scheduleNotification(long time) {
    if (time < currentTimeMillis()) {
      Intent intent = getNotificationIntent();
      if (atLeastOreo()) {
        context.startForegroundService(intent);
      } else {
        context.startService(intent);
      }
    } else {
      PendingIntent pendingIntent = getNotificationPendingIntent();
      if (atLeastMarshmallow()) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
      } else if (atLeastKitKat()) {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
      } else {
        alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
      }
    }
  }

  public void scheduleBackup() {
    long lastBackup = preferences.getLong(R.string.p_last_backup, 0L);
    enqueueUnique(
        TAG_BACKUP,
        BackupWork.class,
        Math.min(newDateTime(lastBackup).plusDays(1).getMillis(), midnight()));
  }

  public void scheduleDriveUpload(Uri uri, boolean purge) {
    if (!preferences.getBoolean(R.string.p_google_drive_backup, false)) {
      return;
    }

    Builder builder =
        new Builder(DriveUploader.class)
            .setInputData(DriveUploader.getInputData(uri, purge))
            .setConstraints(getNetworkConstraints());
    if (purge) {
      builder.setInitialDelay(new Random().nextInt(3600), TimeUnit.SECONDS);
    }
    workManager.enqueue(builder.build());
  }

  private Constraints getNetworkConstraints() {
    return getNetworkConstraints(
        preferences.getBoolean(R.string.p_background_sync_unmetered_only, false));
  }

  private Constraints getNetworkConstraints(boolean unmeteredOnly) {
    return new Constraints.Builder()
        .setRequiredNetworkType(unmeteredOnly ? NetworkType.UNMETERED : NetworkType.CONNECTED)
        .build();
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

  @SuppressWarnings("WeakerAccess")
  public void cancelNotifications() {
    Timber.d("cancelNotifications");
    alarmManager.cancel(getNotificationPendingIntent());
  }

  private Intent getNotificationIntent() {
    return new Intent(context, NotificationService.class);
  }

  private PendingIntent getNotificationPendingIntent() {
    Intent intent = getNotificationIntent();
    return atLeastOreo()
        ? PendingIntent.getForegroundService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        : PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }
}
