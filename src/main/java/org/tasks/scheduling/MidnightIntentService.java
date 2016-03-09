package org.tasks.scheduling;

import android.app.PendingIntent;
import android.content.Intent;

import org.tasks.injection.InjectingIntentService;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import timber.log.Timber;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.time.DateTimeUtils.nextMidnight;
import static org.tasks.time.DateTimeUtils.printTimestamp;

public abstract class MidnightIntentService extends InjectingIntentService {

    private static final long PADDING = SECONDS.toMillis(1);

    @Inject Preferences preferences;
    @Inject AlarmManager alarmManager;

    private final String name;

    public MidnightIntentService(String name) {
        super(name);
        this.name = name;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        long lastRun = preferences.getLong(getLastRunPreference(), 0);
        long nextRun = nextMidnight(lastRun);
        long now = currentTimeMillis();

        if (nextRun <= now) {
            nextRun = nextMidnight(now);
            Timber.d("%s running now [nextRun=%s]", name, printTimestamp(nextRun));
            if (!isNullOrEmpty(getLastRunPreference())) {
                preferences.setLong(getLastRunPreference(), now);
            }
            run();
        } else {
            Timber.d("%s will run at %s [lastRun=%s]", name, printTimestamp(nextRun), printTimestamp(lastRun));
        }

        PendingIntent pendingIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.noWakeup(nextRun + PADDING, pendingIntent);
    }

    abstract void run();

    protected String getLastRunPreference() {
        return null;
    }
}
