package com.todoroo.astrid.reminders;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.test.TodorooRobolectricTestCase;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.todoroo.astrid.reminders.ReminderService.NO_ALARM;
import static org.junit.Assert.assertEquals;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.Freeze.thaw;
import static org.tasks.date.DateTimeUtils.newDate;

@RunWith(RobolectricTestRunner.class)
public class NotifyAtDeadlineTest extends TodorooRobolectricTestCase {

    @Autowired
    TaskDao taskDao;

    ReminderService service;

    @Override
    public void before() {
        super.before();
        freezeAt(new DateTime(2014, 1, 24, 17, 23, 37));
        service = new ReminderService();
    }

    @After
    public void after() {
        thaw();
    }

    @Test
    public void scheduleReminderAtDueTime() {
        final DateTime dueDate = new DateTime(2014, 1, 24, 19, 23, 57);
        Task task = new Task() {{
            setDueDate(dueDate.getMillis());
            setReminderFlags(Task.NOTIFY_AT_DEADLINE);
        }};
        assertEquals(dueDate.getMillis(), service.calculateNextDueDateReminder(task));
    }

    @Test
    public void noReminderWhenNoDueDate() {
        Task task = new Task() {{
            setReminderFlags(Task.NOTIFY_AT_DEADLINE);
        }};
        assertEquals(NO_ALARM, service.calculateNextDueDateReminder(task));
    }

    @Test
    public void noReminderWhenNotifyAtDeadlineFlagNotSet() {
        Task task = new Task() {{
            setDueDate(new DateTime(2014, 1, 24, 19, 23, 57).getMillis());
        }};
        assertEquals(NO_ALARM, service.calculateNextDueDateReminder(task));
    }

    @Test
    public void dontNotifyMoreThanOncePerDay() {
        Task task = new Task() {{
            setDueDate(newDate(2014, 1, 23).getTime());
            setReminderFlags(Task.NOTIFY_AT_DEADLINE);
            setReminderLast(new DateTime(2014, 1, 23, 17, 23, 37).getMillis());
        }};
        assertEquals(NO_ALARM, service.calculateNextDueDateReminder(task));
    }

    @Test
    public void notifyIfLastNotificationWasMoreThanOneDayAgo() {
        final DateTime dueDate = new DateTime(2014, 1, 23, 0, 0, 0, 0);
        Task task = new Task() {{
            setDueDate(dueDate.getMillis());
            setReminderFlags(Task.NOTIFY_AT_DEADLINE);
            setReminderLast(new DateTime(2014, 1, 23, 17, 23, 36).getMillis());
        }};
        assertEquals(
                dueDate.withHourOfDay(18).getMillis(),
                service.calculateNextDueDateReminder(task));
    }
}
