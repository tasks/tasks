/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

import java.util.Date;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.tasks.date.DateTimeUtils.currentTimeMillis;
import static org.tasks.date.DateTimeUtils.newDate;

/**
 * Data service for reminders
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Singleton
public final class ReminderService  {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

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

    private AlarmScheduler scheduler = new ReminderAlarmScheduler();

    private long now = -1; // For tracking when reminders might be scheduled all at once
    private Context context;
    private Preferences preferences;

    @Inject
    ReminderService(@ForApplication Context context, Preferences preferences) {
        this.context = context;
        this.preferences = preferences;
        setPreferenceDefaults();
    }

    // --- preference handling

    private static boolean preferencesInitialized = false;

    private static final int MILLIS_PER_HOUR = 60 * 60 * 1000;

    /** Set preference defaults, if unset. called at startup */
    public void setPreferenceDefaults() {
        if(preferencesInitialized) {
            return;
        }

        SharedPreferences prefs = preferences.getPrefs();
        Editor editor = prefs.edit();
        Resources r = context.getResources();

        preferences.setIfUnset(prefs, editor, r, R.string.p_rmd_default_random_hours, 0);
        preferences.setIfUnset(prefs, editor, r, R.string.p_rmd_persistent, true);

        editor.commit();
        preferencesInitialized = true;
    }

    // --- reminder scheduling logic

    /**
     * Schedules all alarms
     */
    public void scheduleAllAlarms(TaskDao taskDao) {
        TodorooCursor<Task> cursor = getTasksWithReminders(taskDao, NOTIFICATION_PROPERTIES);
        try {
            now = DateUtilities.now(); // Before mass scheduling, initialize now variable
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                Task task = new Task(cursor);
                scheduleAlarm(task, null);
            }
        } catch (Exception e) {
            // suppress
            log.error(e.getMessage(), e);
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
     */
    public void scheduleAlarm(TaskDao taskDao, Task task) {
        scheduleAlarm(task, taskDao);
    }

    public void clearAllAlarms(Task task) {
        scheduler.createAlarm(context, task, NO_ALARM, TYPE_SNOOZE);
        scheduler.createAlarm(context, task, NO_ALARM, TYPE_RANDOM);
        scheduler.createAlarm(context, task, NO_ALARM, TYPE_DUE);
        scheduler.createAlarm(context, task, NO_ALARM, TYPE_OVERDUE);
    }

    private void scheduleAlarm(Task task, TaskDao taskDao) {
        if(task == null || !task.isSaved()) {
            return;
        }

        // read data if necessary
        if(taskDao != null) {
            for(Property<?> property : NOTIFICATION_PROPERTIES) {
                if(!task.containsValue(property)) {
                    task = taskDao.fetch(task.getId(), NOTIFICATION_PROPERTIES);
                    if(task == null) {
                        return;
                    }
                    break;
                }
            }
        }

        // Make sure no alarms are scheduled other than the next one. When that one is shown, it
        // will schedule the next one after it, and so on and so forth.
        clearAllAlarms(task);
        if(task.isCompleted() || task.isDeleted()) {
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
        if(whenRandom != NO_ALARM && whenDueDate - whenRandom < DateUtilities.ONE_DAY) {
            whenRandom = NO_ALARM;
        }

        // snooze trumps all
        if(whenSnooze != NO_ALARM) {
            scheduler.createAlarm(context, task, whenSnooze, TYPE_SNOOZE);
        }
        else if(whenRandom < whenDueDate && whenRandom < whenOverdue) {
            scheduler.createAlarm(context, task, whenRandom, TYPE_RANDOM);
        }
        else if(whenDueDate < whenOverdue) {
            scheduler.createAlarm(context, task, whenDueDate, TYPE_DUE);
        }
        else if(whenOverdue != NO_ALARM) {
            scheduler.createAlarm(context, task, whenOverdue, TYPE_OVERDUE);
        }
        else {
            scheduler.createAlarm(context, task, 0, 0);
        }
    }

    /**
     * Calculate the next alarm time for snooze.
     * <p>
     * Pretty simple - if a snooze time is in the future, we use that. If it
     * has already passed, we do nothing.
     */
    private long calculateNextSnoozeReminder(Task task) {
        if(task.getReminderSnooze() > DateUtilities.now()) {
            return task.getReminderSnooze();
        }
        return NO_ALARM;
    }

    /**
     * Calculate the next alarm time for overdue reminders.
     * <p>
     * We schedule an alarm for after the due date (which could be in the past),
     * with the exception that if a reminder was recently issued, we move
     * the alarm time to the near future.
     */
    private long calculateNextOverdueReminder(Task task) {
     // Uses getNowValue() instead of DateUtilities.now()
        if(task.hasDueDate() && task.isNotifyAfterDeadline()) {
            Date due = newDate(task.getDueDate());
            if (!task.hasDueTime()) {
                due.setHours(23);
                due.setMinutes(59);
                due.setSeconds(59);
            }
            long dueDateForOverdue = due.getTime();
            long lastReminder = task.getReminderLast();

            if(dueDateForOverdue > getNowValue()) {
                return dueDateForOverdue + (long) ((0.5f + 2f * random.nextFloat()) * DateUtilities.ONE_HOUR);
            }

            if(lastReminder < dueDateForOverdue) {
                return getNowValue();
            }

            if(getNowValue() - lastReminder < 6 * DateUtilities.ONE_HOUR) {
                return getNowValue() + (long) ((2.0f +
                        task.getImportance() +
                        6f * random.nextFloat()) * DateUtilities.ONE_HOUR);
            }

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
     */
    long calculateNextDueDateReminder(Task task) {
        // Uses getNowValue() instead of DateUtilities.now()
        if(task.hasDueDate() && task.isNotifyAtDeadline()) {
            long dueDate = task.getDueDate();
            long lastReminder = task.getReminderLast();

            long dueDateAlarm = NO_ALARM;

            if(task.hasDueTime()) {
                dueDateAlarm = dueDate;
            } else if (DateUtilities.now() > lastReminder + DateUtilities.ONE_DAY) {
                // return notification time on this day
                Date date = new DateTime(dueDate).withMillisOfDay(preferences.getInt(R.string.p_rmd_time, 18 * MILLIS_PER_HOUR)).toDate();
                dueDateAlarm = date.getTime();
                if (dueDate > getNowValue() && dueDateAlarm < getNowValue()) {
                    // this only happens for tasks due today, cause dueDateAlarm wouldn't be in the past otherwise
                    // if the default reminder is in the past, then reschedule it
                    // on this day before start of quiet hours or after quiet hours
                    // randomly placed in this interval
                    long quietHoursStart = new DateTime().withMillisOfDay(preferences.getInt(R.string.p_rmd_quietStart, 22 * MILLIS_PER_HOUR)).getMillis();
                    Date quietHoursStartDate = newDate(quietHoursStart);

                    long quietHoursEnd = new DateTime().withMillisOfDay(preferences.getInt(R.string.p_rmd_quietEnd, 10 * MILLIS_PER_HOUR)).getMillis();
                    Date quietHoursEndDate = newDate(quietHoursEnd);

                    boolean quietHoursEnabled = preferences.getBoolean(R.string.p_rmd_enable_quiet, false);

                    long millisToQuiet;
                    long millisToEndOfDay = dueDate - getNowValue();

                    int periodDivFactor = 4;

                    if(quietHoursEnabled) {
                        long now = currentTimeMillis();
                        if(quietHoursStart <= quietHoursEnd) {
                            if(now >= quietHoursStart && now < quietHoursEnd) {
                                // its quiet now, quietHoursEnd is 23 max,
                                // so put the default reminder to the end of the quiethours
                                date = quietHoursEndDate;
                                dueDateAlarm = date.getTime();
                            } else if (now < quietHoursStart) {
                                // quietHours didnt start yet
                                millisToQuiet = quietHoursStartDate.getTime() - getNowValue();
                                long millisAfterQuiet = dueDate - quietHoursEndDate.getTime();

                                // if there is more time after quiethours today, select quiethours-end for reminder
                                if (millisAfterQuiet > (millisToQuiet / ((float)(1-(1/periodDivFactor))) )) {
                                    dueDateAlarm = quietHoursEndDate.getTime();
                                } else {
                                    dueDateAlarm = getNowValue() + (millisToQuiet / periodDivFactor);
                                }
                            } else {
                                // after quietHours, reuse dueDate for end of day
                                dueDateAlarm = getNowValue() + (millisToEndOfDay / periodDivFactor);
                            }
                        } else { // wrap across 24/hour boundary
                            if(now >= quietHoursStart) {
                                // do nothing for the end of day, dont let it even vibrate
                                dueDateAlarm = NO_ALARM;
                            } else if (now < quietHoursEnd) {
                                date = quietHoursEndDate;
                                dueDateAlarm = date.getTime();
                            } else {
                                // quietHours didnt start yet
                                millisToQuiet = quietHoursStartDate.getTime() - getNowValue();
                                dueDateAlarm = getNowValue() + (millisToQuiet / periodDivFactor);
                            }
                        }
                    } else {
                        // Quiet hours not activated, simply schedule the reminder on 1/periodDivFactor towards the end of day
                        dueDateAlarm = getNowValue() + (millisToEndOfDay / periodDivFactor);
                    }

                    if(dueDate > getNowValue() && dueDateAlarm < getNowValue()) {
                        dueDateAlarm = dueDate;
                    }
                }
            }

            if(lastReminder > dueDateAlarm) {
                return NO_ALARM;
            }

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
     */
    private long calculateNextRandomReminder(Task task) {
        long reminderPeriod = task.getReminderPeriod();
        if((reminderPeriod) > 0) {
            long when = task.getReminderLast();

            if(when == 0) {
                when = task.getCreationDate();
            }

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
        public void createAlarm(Context context, Task task, long time, int type);
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
         */
        @Override
        public void createAlarm(Context context, Task task, long time, int type) {
            if(task.getId() == Task.NO_ID) {
                return;
            }
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
                log.error(e.getMessage(), e);
                requestCode = type;
            }
            AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode,
                    intent, 0);

            if (time == 0 || time == NO_ALARM) {
                am.cancel(pendingIntent);
            } else {
                if(time < DateUtilities.now()) {
                    time = DateUtilities.now() + 5000L;
                }

                am.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
            }
        }
    }

    // --- data fetching methods

    /**
     * Gets a listing of all tasks that are active &
     * @return todoroo cursor. PLEASE CLOSE THIS CURSOR!
     */
    private TodorooCursor<Task> getTasksWithReminders(TaskDao taskDao, Property<?>... properties) {
        return taskDao.query(Query.select(properties).where(Criterion.and(
                TaskCriteria.isActive(),
                Criterion.or(Task.REMINDER_FLAGS.gt(0), Task.REMINDER_PERIOD.gt(0)))));
    }
}
