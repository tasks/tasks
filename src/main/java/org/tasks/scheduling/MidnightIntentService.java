package org.tasks.scheduling;

import android.app.PendingIntent;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.injection.InjectingIntentService;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.time.DateTimeUtils.printTimestamp;

public abstract class MidnightIntentService extends InjectingIntentService {

    private static final Logger log = LoggerFactory.getLogger(MidnightIntentService.class);

    private static final long PADDING = SECONDS.toMillis(1);

    @Inject Preferences preferences;
    @Inject AlarmManager alarmManager;

    public MidnightIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        long lastRun = preferences.getLong(getLastRunPreference(), 0);
        long nextRun = nextMidnight(lastRun);
        long now = currentTimeMillis();

        if (nextRun <= now) {
            nextRun = nextMidnight(now);
            log.debug("running now [nextRun={}]", printTimestamp(nextRun));
            preferences.setLong(getLastRunPreference(), now);
            run();
        } else {
            log.debug("will run at {} [lastRun={}]", printTimestamp(nextRun), printTimestamp(lastRun));
        }

        PendingIntent pendingIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.noWakeup(nextRun + PADDING, pendingIntent);
    }

    private static long nextMidnight(long timestamp) {
        return newDateTime(timestamp).withMillisOfDay(0).plusDays(1).getMillis();
    }

    abstract void run();

    abstract String getLastRunPreference();
}
