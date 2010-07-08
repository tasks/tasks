package com.todoroo.astrid.reminders;

import java.util.Date;
import java.util.Random;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Preferences;


/**
 * Data service for reminders
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class ReminderService  {

    // --- constants

    private static final Property<?>[] PROPERTIES = new Property<?>[] {
        Task.DUE_DATE,
        Task.REMINDER_FLAGS,
        Task.REMINDER_PERIOD,
        Task.REMINDER_LAST
    };

    /** flag for due date reminder */
    static final int TYPE_DUE = 0;
    /** flag for overdue reminder */
    static final int TYPE_OVERDUE = 1;
    /** flag for random reminder */
    static final int TYPE_RANDOM = 2;
    /** flag for a snoozed reminder */
    static final int TYPE_SNOOZE = 3;

    static final Random random = new Random();

    // --- instance variables

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private Database database;

    private AlarmScheduler scheduler = new ReminderAlarmScheduler();

    public ReminderService() {
        DependencyInjectionService.getInstance().inject(this);
        setPreferenceDefaults();
    }

    // --- preference handling

    private static boolean preferencesInitialized = false;

    /** Set preference defaults, if unset. called at startup */
    public void setPreferenceDefaults() {
        if(preferencesInitialized)
            return;

        Context context = ContextManager.getContext();
        SharedPreferences prefs = Preferences.getPrefs(context);
        Editor editor = prefs.edit();
        Resources r = context.getResources();

        Preferences.setIfUnset(prefs, editor, r, R.string.p_rmd_quietStart, 22);
        Preferences.setIfUnset(prefs, editor, r, R.string.p_rmd_quietEnd, 10);
        Preferences.setIfUnset(prefs, editor, r, R.string.p_rmd_default_random_hours, 0);
        Preferences.setIfUnset(prefs, editor, r, R.string.p_rmd_time, 12);
        Preferences.setIfUnset(prefs, editor, r, R.string.p_rmd_nagging, true);
        Preferences.setIfUnset(prefs, editor, r, R.string.p_rmd_persistent, true);

        editor.commit();
        preferencesInitialized = true;
    }

    // --- reminder scheduling logic

    /**
     * Schedules all alarms
     */
    public void scheduleAllAlarms() {

        TodorooCursor<Task> cursor = getTasksWithReminders(PROPERTIES);
        try {
            Task task = new Task();
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                task.readFromCursor(cursor);
                scheduleAlarm(task, false);
            }
        } catch (Exception e) {
            // suppress
        } finally {
            cursor.close();
        }
    }

    private static final long NO_ALARM = Long.MAX_VALUE;

    /**
     * Schedules alarms for a single task
     * @param task
     */
    public void scheduleAlarm(Task task) {
        scheduleAlarm(task, true);
    }

    /**
     * Schedules alarms for a single task
     *
     * @param shouldPerformPropertyCheck
     *            whether to check if task has requisite properties
     */
    private void scheduleAlarm(Task task, boolean shouldPerformPropertyCheck) {
        if(task == null || !task.isSaved())
            return;

        // read data if necessary
        if(shouldPerformPropertyCheck) {
            for(Property<?> property : PROPERTIES) {
                if(!task.containsValue(property)) {
                    task = taskDao.fetch(task.getId(), PROPERTIES);
                    if(task == null)
                        return;
                    break;
                }
            }
        }

        // random reminders
        long whenRandom = calculateNextRandomReminder(task);

        // notifications at due date
        long whenDueDate = calculateNextDueDateReminder(task);

        // notifications after due date
        long whenOverdue = calculateNextOverdueReminder(task);

        if(whenRandom < whenDueDate && whenRandom < whenOverdue)
            scheduler.createAlarm(task, whenRandom, TYPE_RANDOM);
        else if(whenDueDate < whenOverdue)
            scheduler.createAlarm(task, whenDueDate, TYPE_DUE);
        else if(whenOverdue != NO_ALARM)
            scheduler.createAlarm(task, whenOverdue, TYPE_OVERDUE);
    }

    /**
     * Calculate the next alarm time for overdue reminders. If the due date
     * has passed, we schedule a reminder some time in the next 4 - 24 hours.
     *
     * @param task
     * @return
     */
    private long calculateNextOverdueReminder(Task task) {
        if(task.hasDueDate() && task.getFlag(Task.REMINDER_FLAGS, Task.NOTIFY_AFTER_DEADLINE)) {
            long dueDate = task.getValue(Task.DUE_DATE);
            if(dueDate > DateUtilities.now())
                return NO_ALARM;
            return DateUtilities.now() + (long)((4 + 20 * random.nextFloat()) * DateUtilities.ONE_HOUR);
        }
        return NO_ALARM;
    }

    /**
     * Calculate the next alarm time for due date reminders. If the due date
     * has not already passed, we return the due date, altering the time
     * if the date was indicated to not have a due time
     *
     * @param task
     * @return
     */
    private long calculateNextDueDateReminder(Task task) {
        if(task.hasDueDate() && task.getFlag(Task.REMINDER_FLAGS, Task.NOTIFY_AT_DEADLINE)) {
            long dueDate = task.getValue(Task.DUE_DATE);
            if(dueDate < DateUtilities.now())
                return NO_ALARM;
            else if(task.hasDueTime())
                // return due date straight up
                return dueDate;
            else {
                // return notification time on this day
                Date date = new Date(dueDate);
                date.setHours(Preferences.getIntegerFromString(R.string.p_rmd_time));
                date.setMinutes(0);
                return date.getTime();
            }
        }
        return NO_ALARM;
    }

    /**
     * Calculate the next alarm time for random reminders. We take the last
     * random reminder time and add approximately the reminder period, until
     * we get a time that's in the future.
     *
     * @param task
     * @return
     */
    private long calculateNextRandomReminder(Task task) {
        long reminderPeriod = task.getValue(Task.REMINDER_PERIOD);
        if((reminderPeriod) > 0) {
            long when = task.getValue(Task.REMINDER_LAST);

            // get or make up a last notification time
            if(when == 0) {
                when = DateUtilities.now();
                task.setValue(Task.REMINDER_LAST, when);
                taskDao.save(task, false);
            }

            when += (long)(reminderPeriod * (0.85f + 0.3f * random.nextFloat()));
            return when;
        }
        return NO_ALARM;
    }

    // --- alarm manager alarm creation

    /**
     * Interface for testing
     */
    public interface AlarmScheduler {
        public void createAlarm(Task task, long time, int type);
    }

    public void setScheduler(AlarmScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public AlarmScheduler getScheduler() {
        return scheduler;
    }

    private class ReminderAlarmScheduler implements AlarmScheduler {
        /**
         * Create an alarm for the given task at the given type
         *
         * @param task
         * @param time
         * @param type
         * @param flags
         */
        @SuppressWarnings("nls")
        public void createAlarm(Task task, long time, int type) {
            if(time == 0 || time == NO_ALARM)
                return;

            if(time < DateUtilities.now()) {
                time = DateUtilities.now() + (long)((0.5f +
                        4 * random.nextFloat()) * DateUtilities.ONE_HOUR);
            }

            Context context = ContextManager.getContext();
            Intent intent = new Intent(context, Notifications.class);
            intent.setType(Long.toString(task.getId()));
            intent.setAction(Integer.toString(type));
            intent.putExtra(Notifications.ID_KEY, task.getId());
            intent.putExtra(Notifications.TYPE_KEY, type);

            AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                    intent, 0);

            if(Constants.DEBUG)
                Log.e("Astrid", "Alarm (" + task.getId() + ", " + type +
                        ") set for " + new Date(time));
            am.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        }
    }

    // --- data fetching classes

    /**
     * Gets a listing of all tasks that are active &
     * @param properties
     * @return todoroo cursor. PLEASE CLOSE THIS CURSOR!
     */
    private TodorooCursor<Task> getTasksWithReminders(Property<?>... properties) {
        database.openForReading();
        return taskDao.query(Query.select(properties).where(Criterion.and(TaskCriteria.isActive(),
                Task.REMINDER_FLAGS.gt(0))));
    }


}