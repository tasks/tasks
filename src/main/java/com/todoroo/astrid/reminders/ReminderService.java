/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.injection.ApplicationScope;
import org.tasks.jobs.JobManager;
import org.tasks.jobs.JobQueue;
import org.tasks.jobs.Reminder;
import org.tasks.preferences.Preferences;
import org.tasks.reminders.Random;
import org.tasks.time.DateTime;

import java.util.List;

import javax.inject.Inject;

import static org.tasks.date.DateTimeUtils.newDateTime;

@ApplicationScope
public final class ReminderService  {

    // --- constants

    private static final Property<?>[] NOTIFICATION_PROPERTIES = new Property<?>[] {
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

    private static final long NO_ALARM = Long.MAX_VALUE;

    private final JobQueue<Reminder> jobs;
    private final Random random;
    private final Preferences preferences;

    private long now = -1; // For tracking when reminders might be scheduled all at once

    @Inject
    ReminderService(Preferences preferences, JobManager jobManager) {
        this(preferences, JobQueue.newReminderQueue(preferences, jobManager), new Random());
    }

    ReminderService(Preferences preferences, JobQueue<Reminder> jobs, Random random) {
        this.preferences = preferences;
        this.jobs = jobs;
        this.random = random;
    }

    private static final int MILLIS_PER_HOUR = 60 * 60 * 1000;

    public void scheduleAllAlarms(TaskDao taskDao) {
        now = DateUtilities.now(); // Before mass scheduling, initialize now variable
        Query query = Query.select(NOTIFICATION_PROPERTIES).where(Criterion.and(
                TaskCriteria.isActive(),
                Criterion.or(Task.REMINDER_FLAGS.gt(0), Task.REMINDER_PERIOD.gt(0))));
        taskDao.forEach(query, task -> scheduleAlarm(null, task));
        now = -1; // Signal done with now variable
    }

    public void clear() {
        jobs.clear();
    }

    private long getNowValue() {
        // If we're in the midst of mass scheduling, use the prestored now var
        return (now == -1 ? DateUtilities.now() : now);
    }

    public List<Reminder> getPastReminders() {
        return jobs.getOverdueJobs();
    }

    public boolean remove(Reminder reminder) {
        return jobs.remove(reminder);
    }

    public void scheduleNextJob() {
        jobs.scheduleNext();
    }

    public void scheduleAlarm(TaskDao taskDao, Task task) {
        if(task == null || !task.isSaved()) {
            return;
        }

        // read data if necessary
        long taskId = task.getId();
        if(taskDao != null) {
            for(Property<?> property : NOTIFICATION_PROPERTIES) {
                if(!task.containsValue(property)) {
                    task = taskDao.fetch(taskId, NOTIFICATION_PROPERTIES);
                    if(task == null) {
                        return;
                    }
                    break;
                }
            }
        }

        // Make sure no alarms are scheduled other than the next one. When that one is shown, it
        // will schedule the next one after it, and so on and so forth.
        jobs.cancel(taskId);

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

        if (whenDueDate <= now) {
            whenDueDate = now;
        }

        if (whenOverdue <= now) {
            whenOverdue = now;
        }

        // if random reminders are too close to due date, favor due date
        if(whenRandom != NO_ALARM && whenDueDate - whenRandom < DateUtilities.ONE_DAY) {
            whenRandom = NO_ALARM;
        }

        // snooze trumps all
        if(whenSnooze != NO_ALARM) {
            jobs.add(new Reminder(taskId, whenSnooze, TYPE_SNOOZE));
        } else if(whenRandom < whenDueDate && whenRandom < whenOverdue) {
            jobs.add(new Reminder(taskId, whenRandom, TYPE_RANDOM));
        } else if(whenDueDate < whenOverdue) {
            jobs.add(new Reminder(taskId, whenDueDate, TYPE_DUE));
        } else if(whenOverdue != NO_ALARM) {
            jobs.add(new Reminder(taskId, whenOverdue, TYPE_OVERDUE));
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
            DateTime due = newDateTime(task.getDueDate());
            if (!task.hasDueTime()) {
                due = due
                        .withHourOfDay(23)
                        .withMinuteOfHour(59)
                        .withSecondOfMinute(59);
            }
            long dueDateForOverdue = due.getMillis();
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
    private long calculateNextDueDateReminder(Task task) {
        if(task.hasDueDate() && task.isNotifyAtDeadline()) {
            long dueDate = task.getDueDate();
            long lastReminder = task.getReminderLast();

            long dueDateAlarm;

            if (task.hasDueTime()) {
                dueDateAlarm = dueDate;
            } else {
                dueDateAlarm = new DateTime(dueDate)
                        .withMillisOfDay(preferences.getInt(R.string.p_rmd_time, 18 * MILLIS_PER_HOUR))
                        .getMillis();
            }

            return lastReminder < dueDateAlarm ? dueDateAlarm : NO_ALARM;
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
}
