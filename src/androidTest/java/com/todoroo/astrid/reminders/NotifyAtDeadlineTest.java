package com.todoroo.astrid.reminders;

import android.test.AndroidTestCase;

import com.todoroo.astrid.data.Task;

import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static com.todoroo.astrid.data.Task.NOTIFY_AT_DEADLINE;
import static com.todoroo.astrid.reminders.ReminderService.NO_ALARM;
import static org.tasks.makers.TaskMaker.DUE_DATE;
import static org.tasks.makers.TaskMaker.DUE_TIME;
import static org.tasks.makers.TaskMaker.REMINDERS;
import static org.tasks.makers.TaskMaker.REMINDER_LAST;
import static org.tasks.makers.TaskMaker.newTask;

public class NotifyAtDeadlineTest extends AndroidTestCase {

    private ReminderService reminderService;

    @Override
    public void setUp() {
        Preferences preferences = new Preferences(getContext(), null);
        reminderService = new ReminderService(getContext(), preferences, null);
    }

    public void testNoReminderWhenNoDueDate() {
        Task task = newTask(with(REMINDERS, NOTIFY_AT_DEADLINE));
        assertEquals(NO_ALARM, reminderService.calculateNextDueDateReminder(task));
    }

    public void testNoReminderWhenNotifyAtDeadlineFlagNotSet() {
        Task task = newTask(with(DUE_TIME, new DateTime(2014, 1, 24, 19, 23)));
        assertEquals(NO_ALARM, reminderService.calculateNextDueDateReminder(task));
    }

    public void testScheduleReminderAtDueTime() {
        final DateTime dueDate = new DateTime(2014, 1, 24, 19, 23);
        Task task = newTask(with(DUE_TIME, dueDate), with(REMINDERS, NOTIFY_AT_DEADLINE));
        assertEquals(dueDate.plusSeconds(1).getMillis(), reminderService.calculateNextDueDateReminder(task));
    }

    public void testScheduleReminderAtDefaultDueTime() {
        final DateTime dueDate = new DateTime(2015, 12, 29, 12, 0);
        Task task = newTask(with(DUE_DATE, dueDate), with(REMINDERS, NOTIFY_AT_DEADLINE));
        assertEquals(dueDate.withHourOfDay(18).getMillis(),
                reminderService.calculateNextDueDateReminder(task));
    }

    public void testNoReminderIfAlreadyRemindedPastDueDate() {
        final DateTime dueDate = new DateTime(2015, 12, 29, 19, 23);
        Task task = newTask(
                with(DUE_TIME, dueDate),
                with(REMINDER_LAST, dueDate.plusSeconds(1)),
                with(REMINDERS, NOTIFY_AT_DEADLINE));
        assertEquals(NO_ALARM, reminderService.calculateNextDueDateReminder(task));
    }
}
