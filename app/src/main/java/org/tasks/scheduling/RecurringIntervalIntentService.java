package org.tasks.scheduling;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.time.DateTimeUtils.printTimestamp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import javax.inject.Inject;
import org.tasks.injection.InjectingJobIntentService;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public abstract class RecurringIntervalIntentService extends InjectingJobIntentService {

  private static final long PADDING = SECONDS.toMillis(1);

  @Inject Preferences preferences;
  @Inject AlarmManager alarmManager;

  @Override
  protected void doWork(Intent intent) {
    long interval = intervalMillis();

    if (interval <= 0) {
      Timber.d("service disabled");
      return;
    }

    String lastRunPreference = getLastRunPreference();
    long lastRun = lastRunPreference != null ? preferences.getLong(lastRunPreference, 0) : 0;
    long now = currentTimeMillis();
    long nextRun = lastRun + interval;

    if (lastRunPreference == null || nextRun < now + PADDING) {
      nextRun = now + interval;
      Timber.d("running now [nextRun=%s]", printTimestamp(nextRun));
      if (lastRunPreference != null) {
        preferences.setLong(lastRunPreference, now);
      }
      run();
    } else {
      Timber.d("will run at %s [lastRun=%s]", printTimestamp(nextRun), printTimestamp(lastRun));
    }

    PendingIntent pendingIntent =
        PendingIntent.getBroadcast(
            this, 0, new Intent(this, getBroadcastClass()), PendingIntent.FLAG_UPDATE_CURRENT);
    alarmManager.wakeup(nextRun, pendingIntent);
  }

  abstract Class<? extends BroadcastReceiver> getBroadcastClass();

  abstract void run();

  abstract long intervalMillis();

  abstract String getLastRunPreference();
}
