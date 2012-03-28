package com.todoroo.astrid.helper;

import java.util.Date;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

public class DueDateTimeMigrator {

    @Autowired TaskDao taskDao;

    private static final String PREF_MIGRATED_DUE_TIMES = "migrated_due_times";  //$NON-NLS-1$
    private static final String LEGACY_NO_TIME_STRING = "23:59:59"; //$NON-NLS-1$

    public DueDateTimeMigrator() {
        DependencyInjectionService.getInstance().inject(this);
    }

    private interface TaskDateAdjuster {
        public void adjust(Date date);
    }

    public void migrateDueTimes() {
        if (!Preferences.getBoolean(PREF_MIGRATED_DUE_TIMES, false)) {
            // Get tasks with due time (i.e. due date != 23:59:59)
            TodorooCursor<Task> tasksWithSeconds = taskDao.query(Query.select(Task.ID, Task.TITLE, Task.DUE_DATE).where(
                    Criterion.and(Task.DUE_DATE.gt(0),
                            Criterion.not(Functions.strftime(Task.DUE_DATE).eq(LEGACY_NO_TIME_STRING)))));

            // Set those tassk to have time HH:MM:00
            processCursor(tasksWithSeconds, new TaskDateAdjuster() {
                @Override
                public void adjust(Date date) {
                    date.setSeconds(1);
                }
            });

            // Get tasks with no due time (i.e. due date = 23:59:59)
            TodorooCursor<Task> tasksWithoutDueTime = taskDao.query(Query.select(Task.ID, Task.TITLE, Task.DUE_DATE).where(
                    Criterion.and(Task.DUE_DATE.gt(0),
                            Functions.strftime(Task.DUE_DATE).eq(LEGACY_NO_TIME_STRING))));

            // Set those tasks to have time 12:00:00
            processCursor(tasksWithoutDueTime, new TaskDateAdjuster() {
                @Override
                public void adjust(Date date) {
                    date.setHours(12);
                    date.setMinutes(0);
                    date.setSeconds(0);
                }
            });
            Preferences.setBoolean(PREF_MIGRATED_DUE_TIMES, true);
        }
    }

    private void processCursor(TodorooCursor<Task> cursor, TaskDateAdjuster adjuster) {
        Task curr = new Task();
        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                curr.readFromCursor(cursor);
                System.err.println("Processing task: " + curr.getValue(Task.TITLE) + ", ID: " + curr.getId());
                long time = curr.getValue(Task.DUE_DATE) / 1000L * 1000L;
                Date date = new Date(time);
                adjuster.adjust(date);
                curr.setValue(Task.DUE_DATE, date.getTime());
                taskDao.save(curr);
            }
        } finally {
            cursor.close();
        }
    }

}
