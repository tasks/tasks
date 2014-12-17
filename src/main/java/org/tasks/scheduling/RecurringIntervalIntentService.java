package org.tasks.scheduling;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.injection.InjectingIntentService;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.tasks.date.DateTimeUtils.currentTimeMillis;
import static org.tasks.date.DateTimeUtils.newDateTime;

public abstract class RecurringIntervalIntentService extends InjectingIntentService {

    private static final Logger log = LoggerFactory.getLogger(RecurringIntervalIntentService.class);

    private static final long PADDING = SECONDS.toMillis(1);

    @Inject Preferences preferences;

    public RecurringIntervalIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        long interval = intervalMillis();

        if (interval <= 0) {
            log.debug("service disabled");
            return;
        }

        long lastRun = preferences.getLong(getLastRunPreference(), 0);
        long now = currentTimeMillis();
        long nextRun = lastRun + interval;

        if (nextRun < now + PADDING) {
            nextRun = now + interval;
            log.debug("running now [nextRun={}]", newDateTime(nextRun));
            preferences.setLong(getLastRunPreference(), now);
            run();
        } else {
            log.debug("will run at {} [lastRun={}]", newDateTime(nextRun), newDateTime(lastRun));
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC, nextRun, pendingIntent);
    }

    abstract void run();

    abstract long intervalMillis();

    abstract String getLastRunPreference();
}
