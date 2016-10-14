package org.tasks.scheduling;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.data.Task;

import org.tasks.injection.ForApplication;
import org.tasks.receivers.RefreshReceiver;

import javax.inject.Inject;

import timber.log.Timber;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.todoroo.andlib.utility.DateUtilities.ONE_MINUTE;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.time.DateTimeUtils.nextMidnight;
import static org.tasks.time.DateTimeUtils.printTimestamp;

public class RefreshScheduler {

    private final Context context;
    private final AlarmManager alarmManager;

    @Inject
    public RefreshScheduler(@ForApplication Context context, AlarmManager alarmManager) {
        this.context = context;
        this.alarmManager = alarmManager;
    }

    public void scheduleRefresh(Task task) {
        if (task.isCompleted()) {
            scheduleRefresh(task.getCompletionDate() + ONE_MINUTE);
        } else if (task.hasDueDate()) {
            scheduleRefresh(task.getDueDate());
        }
        if (task.hasHideUntilDate()) {
            scheduleRefresh(task.getHideUntil());
        }
    }

    private void scheduleRefresh(Long refreshTime) {
        long now = currentTimeMillis();
        if (now < refreshTime && refreshTime < nextMidnight(now)) {
            refreshTime += 1000; // this is ghetto
            Timber.d("Scheduling refresh at %s", printTimestamp(refreshTime));
            Intent intent = new Intent(context, RefreshReceiver.class);
            intent.setAction(Long.toString(refreshTime));
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, FLAG_UPDATE_CURRENT);
            alarmManager.noWakeup(refreshTime, pendingIntent);
        }
    }
}
