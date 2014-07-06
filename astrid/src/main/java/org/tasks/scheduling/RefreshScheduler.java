package org.tasks.scheduling;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.injection.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.todoroo.andlib.utility.DateUtilities.ONE_MINUTE;
import static org.tasks.date.DateTimeUtils.currentTimeMillis;

@Singleton
public class RefreshScheduler {

    private final TaskDao taskDao;
    private final Context context;

    private static final Property<?>[] REFRESH_PROPERTIES = new Property<?>[]{
            Task.DUE_DATE,
            Task.HIDE_UNTIL
    };

    @Inject
    public RefreshScheduler(TaskDao taskDao, @ForApplication Context context) {
        this.taskDao = taskDao;
        this.context = context;
    }

    public void scheduleAllAlarms() {
        TodorooCursor<Task> cursor = getTasks();
        try {
            Task task = new Task();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                task.readFromCursor(cursor);
                scheduleRefresh(task);
            }
        } finally {
            cursor.close();
        }
    }

    public void scheduleRefresh(Task task) {
        if (task.containsValue(Task.DUE_DATE)) {
            scheduleRefresh(task.getDueDate());
        }
        if (task.containsValue(Task.HIDE_UNTIL)) {
            scheduleRefresh(task.getHideUntil());
        }
        if (task.containsValue(Task.COMPLETION_DATE)) {
            scheduleRefresh(task.getCompletionDate() + ONE_MINUTE);
        }
    }

    private void scheduleRefresh(Long dueDate) {
        if (currentTimeMillis() > dueDate) {
            return;
        }

        dueDate += 1000; // this is ghetto
        Intent intent = new Intent(context, RefreshBroadcastReceiver.class);
        intent.setAction(Long.toString(dueDate));
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, FLAG_UPDATE_CURRENT);
        am.set(AlarmManager.RTC, dueDate, pendingIntent);
    }

    private TodorooCursor<Task> getTasks() {
        long now = currentTimeMillis();
        return taskDao.query(Query.select(REFRESH_PROPERTIES).where(Criterion.and(
                TaskDao.TaskCriteria.isActive(),
                Criterion.or(Task.HIDE_UNTIL.gt(now), Task.DUE_DATE.gt(now)))));
    }
}
