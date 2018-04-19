/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.List;
import javax.inject.Inject;
import org.tasks.injection.ApplicationScope;
import org.tasks.jobs.NotificationQueue;
import org.tasks.jobs.ReminderEntry;
import org.tasks.preferences.Preferences;
import org.tasks.reminders.Random;
import org.tasks.time.DateTime;

@ApplicationScope
public final class ReminderService {

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

  private final NotificationQueue jobs;
  private final Random random;
  private final TaskDao taskDao;
  private final Preferences preferences;

  @Inject
  ReminderService(Preferences preferences, NotificationQueue notificationQueue, TaskDao taskDao) {
    this(preferences, notificationQueue, new Random(), taskDao);
  }

  ReminderService(Preferences preferences, NotificationQueue jobs, Random random, TaskDao taskDao) {
    this.preferences = preferences;
    this.jobs = jobs;
    this.random = random;
    this.taskDao = taskDao;
  }

  public void scheduleAllAlarms(List<Long> taskIds) {
    for (Task task : taskDao.fetch(taskIds)) {
      scheduleAlarm(task);
    }
  }

  public void scheduleAllAlarms() {
    for (Task task : taskDao.getTasksWithReminders()) {
      scheduleAlarm(task);
    }
  }

  public void cancelReminder(long taskId) {
    jobs.cancelReminder(taskId);
  }

  public void scheduleAlarm(Task task) {
    if (task == null || !task.isSaved()) {
      return;
    }

    long taskId = task.getId();

    // Make sure no alarms are scheduled other than the next one. When that one is shown, it
    // will schedule the next one after it, and so on and so forth.
    cancelReminder(taskId);

    if (task.isCompleted() || task.isDeleted()) {
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

    // if random reminders are too close to due date, favor due date
    if (whenRandom != NO_ALARM && whenDueDate - whenRandom < DateUtilities.ONE_DAY) {
      whenRandom = NO_ALARM;
    }

    // snooze trumps all
    if (whenSnooze != NO_ALARM) {
      jobs.add(new ReminderEntry(taskId, whenSnooze, TYPE_SNOOZE));
    } else if (whenRandom < whenDueDate && whenRandom < whenOverdue) {
      jobs.add(new ReminderEntry(taskId, whenRandom, TYPE_RANDOM));
    } else if (whenDueDate < whenOverdue) {
      jobs.add(new ReminderEntry(taskId, whenDueDate, TYPE_DUE));
    } else if (whenOverdue != NO_ALARM) {
      jobs.add(new ReminderEntry(taskId, whenOverdue, TYPE_OVERDUE));
    }
  }

  private long calculateNextSnoozeReminder(Task task) {
    if (task.getReminderSnooze() > task.getReminderLast()) {
      return task.getReminderSnooze();
    }
    return NO_ALARM;
  }

  private long calculateNextOverdueReminder(Task task) {
    // Uses getNowValue() instead of DateUtilities.now()
    if (task.hasDueDate() && task.isNotifyAfterDeadline()) {
      DateTime overdueDate = new DateTime(task.getDueDate()).plusDays(1);
      if (!task.hasDueTime()) {
        overdueDate = overdueDate.withMillisOfDay(preferences.getDefaultDueTime());
      }

      DateTime lastReminder = new DateTime(task.getReminderLast());

      if (overdueDate.isAfter(lastReminder)) {
        return overdueDate.getMillis();
      }

      overdueDate = lastReminder.withMillisOfDay(overdueDate.getMillisOfDay());

      return overdueDate.isAfter(lastReminder)
          ? overdueDate.getMillis()
          : overdueDate.plusDays(1).getMillis();
    }
    return NO_ALARM;
  }

  /**
   * Calculate the next alarm time for due date reminders.
   *
   * <p>This alarm always returns the due date, and is triggered if the last reminder time occurred
   * before the due date. This means it is possible to return due dates in the past.
   *
   * <p>If the date was indicated to not have a due time, we read from preferences and assign a
   * time.
   */
  private long calculateNextDueDateReminder(Task task) {
    if (task.hasDueDate() && task.isNotifyAtDeadline()) {
      long dueDate = task.getDueDate();
      long lastReminder = task.getReminderLast();

      long dueDateAlarm;

      if (task.hasDueTime()) {
        dueDateAlarm = dueDate;
      } else {
        dueDateAlarm =
            new DateTime(dueDate).withMillisOfDay(preferences.getDefaultDueTime()).getMillis();
      }

      return lastReminder < dueDateAlarm ? dueDateAlarm : NO_ALARM;
    }
    return NO_ALARM;
  }

  /**
   * Calculate the next alarm time for random reminders.
   *
   * <p>We take the last reminder time and add approximately the reminder period. If it's still in
   * the past, we set it to some time in the near future.
   */
  private long calculateNextRandomReminder(Task task) {
    long reminderPeriod = task.getReminderPeriod();
    if ((reminderPeriod) > 0) {
      long when = task.getReminderLast();

      if (when == 0) {
        when = task.getCreationDate();
      }

      when += (long) (reminderPeriod * (0.85f + 0.3f * random.nextFloat()));

      if (when < DateUtilities.now()) {
        when =
            DateUtilities.now() + (long) ((0.5f + 6 * random.nextFloat()) * DateUtilities.ONE_HOUR);
      }

      return when;
    }
    return NO_ALARM;
  }
}
