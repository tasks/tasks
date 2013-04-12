/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
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
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.utility.Constants;


/**
 * Data service for reminders
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class ReminderService  {

    // --- constants

    public static final Property<?>[] NOTIFICATION_PROPERTIES = new Property<?>[] {
        Task.ID,
        Task.CREATION_DATE,
        Task.COMPLETION_DATE,
        Task.DELETION_DATE,
        Task.DUE_DATE,
        Task.REMINDER_FLAGS,
        Task.REMINDER_PERIOD,
        Task.REMINDER_LAST,
        Task.REMINDER_SNOOZE,
        Task.IMPORTANCE
    };

    /** flag for due date reminder */
    public static final int TYPE_DUE = 0;
    /** flag for overdue reminder */
    public static final int TYPE_OVERDUE = 1;
    /** flag for random reminder */
    public static final int TYPE_RANDOM = 2;
    /** flag for a snoozed reminder */
    public static final int TYPE_SNOOZE = 3;
    /** flag for an alarm reminder */
    public static final int TYPE_ALARM = 4;

    static final Random random = new Random();

    // --- instance variables

    @Autowired
    private TaskDao taskDao;

    private AlarmScheduler scheduler = new ReminderAlarmScheduler();

    private long now = -1; // For tracking when reminders might be scheduled all at once

    private ReminderService() {
        DependencyInjectionService.getInstance().inject(this);
        setPreferenceDefaults();
    }

    // --- singleton

    private static ReminderService instance = null;

    public static synchronized ReminderService getInstance() {
        if(instance == null)
            instance = new ReminderService();
        return instance;
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
        Preferences.setIfUnset(prefs, editor, r, R.string.p_rmd_time, 18);
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
        TodorooCursor<Task> cursor = getTasksWithReminders(NOTIFICATION_PROPERTIES);
        try {
            Task task = new Task();
            now = DateUtilities.now(); // Before mass scheduling, initialize now variable
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                task.readFromCursor(cursor);
                scheduleAlarm(task, false);
            }
        } catch (Exception e) {
            // suppress
        } finally {
            cursor.close();
            now = -1; // Signal done with now variable
        }
    }

    private long getNowValue() {
        // If we're in the midst of mass scheduling, use the prestored now var
        return (now == -1 ? DateUtilities.now() : now);
    }

    public static final long NO_ALARM = Long.MAX_VALUE;

    /**
     * Schedules alarms for a single task
     * @param task
     */
    public void scheduleAlarm(Task task) {
        scheduleAlarm(task, true);
    }

    public void clearAllAlarms(Task task) {
        scheduler.createAlarm(task, NO_ALARM, TYPE_SNOOZE);
        scheduler.createAlarm(task, NO_ALARM, TYPE_RANDOM);
        scheduler.createAlarm(task, NO_ALARM, TYPE_DUE);
        scheduler.createAlarm(task, NO_ALARM, TYPE_OVERDUE);
    }

    public void clearAlarm(Task task, int type) {
        scheduler.createAlarm(task, NO_ALARM, type);
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
            for(Property<?> property : NOTIFICATION_PROPERTIES) {
                if(!task.containsValue(property)) {
                    task = taskDao.fetch(task.getId(), NOTIFICATION_PROPERTIES);
                    if(task == null)
                        return;
                    break;
                }
            }
        }

        // Make sure no alarms are scheduled other than the next one. When that one is shown, it
        // will schedule the next one after it, and so on and so forth.
        clearAllAlarms(task);
        if(task.isCompleted() || task.isDeleted() || !Task.USER_ID_SELF.equals(task.getValue(Task.USER_ID))) {
            return;
        }

        // snooze reminder
        long whenSnooze = calculateNextSnoozeReminder(task);

        // random reminders
        long whenRandom = calculateNextRandomReminder(task);

        // notifications at due date
        long whenDueDate = calculateNextDueDateReminder(task);


        // notifications after due date
        long whenOverdue = calculateNextOverdueReminder(task);

        // For alarms around/before now, increment the now value so the next one will be later
        if (whenDueDate <= now || whenOverdue <= now) {
            whenDueDate = now;
            whenOverdue = now;
            now += 30 * DateUtilities.ONE_MINUTE; // Prevents overdue tasks from being scheduled all at once
        }

        // if random reminders are too close to due date, favor due date
        if(whenRandom != NO_ALARM && whenDueDate - whenRandom < DateUtilities.ONE_DAY)
            whenRandom = NO_ALARM;

        // snooze trumps all
        if(whenSnooze != NO_ALARM) {
            scheduler.createAlarm(task, whenSnooze, TYPE_SNOOZE);
        }
        else if(whenRandom < whenDueDate && whenRandom < whenOverdue) {
            scheduler.createAlarm(task, whenRandom, TYPE_RANDOM);
        }
        else if(whenDueDate < whenOverdue) {
            scheduler.createAlarm(task, whenDueDate, TYPE_DUE);
        }
        else if(whenOverdue != NO_ALARM) {
            scheduler.createAlarm(task, whenOverdue, TYPE_OVERDUE);
        }
        else {
            scheduler.createAlarm(task, 0, 0);
        }
    }

    /**
     * Calculate the next alarm time for snooze.
     * <p>
     * Pretty simple - if a snooze time is in the future, we use that. If it
     * has already passed, we do nothing.
     *
     * @param task
     * @return
     */
    private long calculateNextSnoozeReminder(Task task) {
        if(task.getValue(Task.REMINDER_SNOOZE) > DateUtilities.now())
            return task.getValue(Task.REMINDER_SNOOZE);
        return NO_ALARM;
    }

    /**
     * Calculate the next alarm time for overdue reminders.
     * <p>
     * We schedule an alarm for after the due date (which could be in the past),
     * with the exception that if a reminder was recently issued, we move
     * the alarm time to the near future.
     *
     * @param task
     * @return
     */
    private long calculateNextOverdueReminder(Task task) {
     // Uses getNowValue() instead of DateUtilities.now()
        if(task.hasDueDate() && task.getFlag(Task.REMINDER_FLAGS, Task.NOTIFY_AFTER_DEADLINE)) {
            Date due = new Date(task.getValue(Task.DUE_DATE));
            if (!task.hasDueTime()) {
                due.setHours(23);
                due.setMinutes(59);
                due.setSeconds(59);
            }
            long dueDateForOverdue = due.getTime();
            long lastReminder = task.getValue(Task.REMINDER_LAST);

            if(dueDateForOverdue > getNowValue())
                return dueDateForOverdue + (long)((0.5f + 2f * random.nextFloat()) * DateUtilities.ONE_HOUR);

            if(lastReminder < dueDateForOverdue)
                return getNowValue();

            if(getNowValue() - lastReminder < 6 * DateUtilities.ONE_HOUR)
                return getNowValue() + (long)((2.0f +
                        task.getValue(Task.IMPORTANCE) +
                        6f * random.nextFloat()) * DateUtilities.ONE_HOUR);

            return getNowValue();
        }
        return NO_ALARM;
    }

    /**
     * Calculate the next alarm time for due date reminders.
     * <p>
     * This alarm always returns the due date, and is triggered if
     * the last reminder time occurred before the due date. This means it is
     * possible to return due dates in the past.
     * <p>
     * If the date was indicated to not have a due time, we read from
     * preferences and assign a time.
     *
     * @param task
     * @return
     */
    private long calculateNextDueDateReminder(Task task) {
        // Uses getNowValue() instead of DateUtilities.now()
        if(task.hasDueDate() && task.getFlag(Task.REMINDER_FLAGS, Task.NOTIFY_AT_DEADLINE)) {
            long dueDate = task.getValue(Task.DUE_DATE);
            long lastReminder = task.getValue(Task.REMINDER_LAST);

            long dueDateAlarm = NO_ALARM;

            if(task.hasDueTime())
                // return due date straight up
                dueDateAlarm = dueDate;
            else if (DateUtilities.now() > lastReminder + DateUtilities.ONE_DAY) {
                // return notification time on this day
                Date date = new Date(dueDate);
                date.setHours(Preferences.getIntegerFromString(R.string.p_rmd_time, 18));
                date.setMinutes(0);
                date.setSeconds(0);
                dueDateAlarm = date.getTime();
                if (dueDate > getNowValue() && dueDateAlarm < getNowValue()) {
                    // this only happens for tasks due today, cause dueDateAlarm wouldnt be in the past otherwise
                    // if the default reminder is in the past, then reschedule it
                    // on this day before start of quiet hours or after quiet hours
                    // randomly placed in this interval
                    int quietHoursStart = Preferences.getIntegerFromString(R.string.p_rmd_quietStart, -1);
                    Date quietHoursStartDate = new Date();
                    quietHoursStartDate.setHours(quietHoursStart);
                    quietHoursStartDate.setMinutes(0);
                    quietHoursStartDate.setSeconds(0);

                    int quietHoursEnd = Preferences.getIntegerFromString(R.string.p_rmd_quietEnd, -1);
                    Date quietHoursEndDate = new Date();
                    quietHoursEndDate.setHours(quietHoursStart);
                    quietHoursEndDate.setMinutes(0);
                    quietHoursEndDate.setSeconds(0);

                    long millisToQuiet = quietHoursStartDate.getTime() - getNowValue();
                    long millisToEndOfDay = dueDate - getNowValue();

                    //
                    int periodDivFactor = 4;

                    if(quietHoursStart != -1 && quietHoursEnd != -1) {
                        int hour = new Date().getHours();
                        if(quietHoursStart <= quietHoursEnd) {
                            if(hour >= quietHoursStart && hour < quietHoursEnd) {
                                // its quiet now, quietHoursEnd is 23 max,
                                // so put the default reminder to the end of the quiethours
                                date.setHours(quietHoursEnd);
                                dueDateAlarm = date.getTime();
                            } else if (hour < quietHoursStart) {
                                // quietHours didnt start yet
                                millisToQuiet = quietHoursStartDate.getTime() - getNowValue();
                                long millisAfterQuiet = dueDate - quietHoursEndDate.getTime();

                                // if there is more time after quiethours today, select quiethours-end for reminder
                                if (millisAfterQuiet > (millisToQuiet / ((float)(1-(1/periodDivFactor))) ))
                                    dueDateAlarm = quietHoursEndDate.getTime();
                                else
                                    dueDateAlarm = getNowValue() + (long)(millisToQuiet / periodDivFactor);
                            } else {
                                // after quietHours, reuse dueDate for end of day
                                dueDateAlarm = getNowValue() + (long)(millisToEndOfDay / periodDivFactor);
                            }
                        } else { // wrap across 24/hour boundary
                            if(hour >= quietHoursStart) {
                                // do nothing for the end of day, dont let it even vibrate
                                dueDateAlarm = NO_ALARM;
                            } else if (hour < quietHoursEnd) {
                                date.setHours(quietHoursEnd);
                                dueDateAlarm = date.getTime();
                            } else {
                                // quietHours didnt start yet
                                millisToQuiet = quietHoursStartDate.getTime() - getNowValue();
                                dueDateAlarm = getNowValue() + (long)(millisToQuiet / periodDivFactor);
                            }
                        }
                    } else {
                        // Quiet hours not activated, simply schedule the reminder on 1/periodDivFactor towards the end of day
                        dueDateAlarm = getNowValue() + (long)(millisToEndOfDay / periodDivFactor);
                    }

                    if(dueDate > getNowValue() && dueDateAlarm < getNowValue())
                        dueDateAlarm = dueDate;
                }
            }

            if(lastReminder > dueDateAlarm)
                return NO_ALARM;

            return dueDateAlarm;
        }
        return NO_ALARM;
    }

    /**
     * Calculate the next alarm time for random reminders.
     * <p>
     * We take the last reminder time and add approximately the reminder
     * period. If it's still in the past, we set it to some time in the near
     * future.
     *
     * @param task
     * @return
     */
    private long calculateNextRandomReminder(Task task) {
        long reminderPeriod = task.getValue(Task.REMINDER_PERIOD);
        if((reminderPeriod) > 0) {
            long when = task.getValue(Task.REMINDER_LAST);

            if(when == 0)
                when = task.getValue(Task.CREATION_DATE);

            when += (long)(reminderPeriod * (0.85f + 0.3f * random.nextFloat()));

            if(when < DateUtilities.now()) {
                when = DateUtilities.now() + (long)((0.5f +
                        6 * random.nextFloat()) * DateUtilities.ONE_HOUR);
            }

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

    private static class ReminderAlarmScheduler implements AlarmScheduler {
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
            if(task.getId() == Task.NO_ID)
                return;
            Context context = ContextManager.getContext();
            Intent intent = new Intent(context, Notifications.class);
            intent.setType(Long.toString(task.getId()));
            intent.setAction(Integer.toString(type));
            intent.putExtra(Notifications.ID_KEY, task.getId());
            intent.putExtra(Notifications.EXTRAS_TYPE, type);

            // calculate the unique requestCode as a combination of the task-id and alarm-type:
            // concatenate id+type to keep the combo unique
            String rc = String.format("%d%d", task.getId(), type);
            int requestCode;
            try {
                requestCode = Integer.parseInt(rc);
            } catch (Exception e) {
                requestCode = type;
            }
            AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode,
                    intent, 0);

            if (time == 0 || time == NO_ALARM)
                am.cancel(pendingIntent);
            else {
                if(time < DateUtilities.now())
                    time = DateUtilities.now() + 5000L;

               if(Constants.DEBUG)
                    Log.e("Astrid", "Reminder set for " + new Date(time)+" for (\""+task.getValue(Task.TITLE)+"\" (" + task.getId() + "), " + type +")");
                am.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
            }
        }
    }

    // --- data fetching methods

    /**
     * Gets a listing of all tasks that are active &
     * @param properties
     * @return todoroo cursor. PLEASE CLOSE THIS CURSOR!
     */
    private TodorooCursor<Task> getTasksWithReminders(Property<?>... properties) {
        return taskDao.query(Query.select(properties).where(Criterion.and(
                TaskCriteria.isActive(),
                TaskCriteria.ownedByMe(),
                Criterion.or(Task.REMINDER_FLAGS.gt(0), Task.REMINDER_PERIOD.gt(0)))));
    }


}
