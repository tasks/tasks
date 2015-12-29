package org.tasks.scheduling;

import android.app.PendingIntent;
import android.content.Intent;

import org.tasks.injection.InjectingIntentService;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import timber.log.Timber;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.time.DateTimeUtils.printTimestamp;

public abstract class RecurringIntervalIntentService extends InjectingIntentService {

    private static final long PADDING = SECONDS.toMillis(1);

    @Inject Preferences preferences;
    @Inject AlarmManager alarmManager;

    public RecurringIntervalIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

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

        PendingIntent pendingIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.wakeup(nextRun, pendingIntent);
    }

    abstract void run();

    abstract long intervalMillis();

    abstract String getLastRunPreference();
}
