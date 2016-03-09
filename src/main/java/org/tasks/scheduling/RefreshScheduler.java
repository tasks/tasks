package org.tasks.scheduling;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.dao.TaskDao;
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

    private final TaskDao taskDao;
    private final Context context;
    private final AlarmManager alarmManager;

    @Inject
    public RefreshScheduler(TaskDao taskDao, @ForApplication Context context, AlarmManager alarmManager) {
        this.taskDao = taskDao;
        this.context = context;
        this.alarmManager = alarmManager;
    }

    public void scheduleApplicationRefreshes() {
        long now = currentTimeMillis();
        long midnight = nextMidnight(now);
        Criterion criterion = Criterion.or(
                Criterion.and(Task.HIDE_UNTIL.gt(now), Task.HIDE_UNTIL.lt(midnight)),
                Criterion.and(Task.DUE_DATE.gt(now), Task.DUE_DATE.lt(midnight)));
        taskDao.selectActive(criterion, new Callback<Task>() {
            @Override
            public void apply(Task task) {
                scheduleRefresh(task);
            }
        });
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
