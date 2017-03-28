package com.todoroo.astrid.reminders;

import android.support.test.runner.AndroidJUnit4;

import com.todoroo.astrid.data.Task;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static com.todoroo.astrid.data.Task.NOTIFY_AT_DEADLINE;
import static com.todoroo.astrid.reminders.ReminderService.NO_ALARM;
import static junit.framework.Assert.assertEquals;
import static org.tasks.makers.TaskMaker.DUE_DATE;
import static org.tasks.makers.TaskMaker.DUE_TIME;
import static org.tasks.makers.TaskMaker.REMINDERS;
import static org.tasks.makers.TaskMaker.REMINDER_LAST;
import static org.tasks.makers.TaskMaker.newTask;

@RunWith(AndroidJUnit4.class)
public class NotifyAtDeadlineTest {

    private ReminderService reminderService;

    @Before
    public void setUp() {
        Preferences preferences = new Preferences(getTargetContext(), null);
        reminderService = new ReminderService(preferences, null);
    }

    @Test
    public void testNoReminderWhenNoDueDate() {
        Task task = newTask(with(REMINDERS, NOTIFY_AT_DEADLINE));
        assertEquals(NO_ALARM, reminderService.calculateNextDueDateReminder(task));
    }

    @Test
    public void testNoReminderWhenNotifyAtDeadlineFlagNotSet() {
        Task task = newTask(with(DUE_TIME, new DateTime(2014, 1, 24, 19, 23)));
        assertEquals(NO_ALARM, reminderService.calculateNextDueDateReminder(task));
    }

    @Test
    public void testScheduleReminderAtDueTime() {
        final DateTime dueDate = new DateTime(2014, 1, 24, 19, 23);
        Task task = newTask(with(DUE_TIME, dueDate), with(REMINDERS, NOTIFY_AT_DEADLINE));
        assertEquals(dueDate.plusSeconds(1).getMillis(), reminderService.calculateNextDueDateReminder(task));
    }

    @Test
    public void testScheduleReminderAtDefaultDueTime() {
        final DateTime dueDate = new DateTime(2015, 12, 29, 12, 0);
        Task task = newTask(with(DUE_DATE, dueDate), with(REMINDERS, NOTIFY_AT_DEADLINE));
        assertEquals(dueDate.withHourOfDay(18).getMillis(),
                reminderService.calculateNextDueDateReminder(task));
    }

    @Test
    public void testNoReminderIfAlreadyRemindedPastDueDate() {
        final DateTime dueDate = new DateTime(2015, 12, 29, 19, 23);
        Task task = newTask(
                with(DUE_TIME, dueDate),
                with(REMINDER_LAST, dueDate.plusSeconds(1)),
                with(REMINDERS, NOTIFY_AT_DEADLINE));
        assertEquals(NO_ALARM, reminderService.calculateNextDueDateReminder(task));
    }
}
