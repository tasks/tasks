package com.todoroo.astrid.reminders;

import android.support.test.runner.AndroidJUnit4;

import com.todoroo.astrid.data.Task;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.tasks.Snippet;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.jobs.JobQueue;
import org.tasks.jobs.Reminder;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static com.todoroo.astrid.data.Task.NOTIFY_AFTER_DEADLINE;
import static com.todoroo.astrid.data.Task.NOTIFY_AT_DEADLINE;
import static com.todoroo.astrid.reminders.ReminderService.TYPE_RANDOM;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.tasks.Freeze.freezeClock;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.makers.TaskMaker.COMPLETION_TIME;
import static org.tasks.makers.TaskMaker.DELETION_TIME;
import static org.tasks.makers.TaskMaker.DUE_DATE;
import static org.tasks.makers.TaskMaker.DUE_TIME;
import static org.tasks.makers.TaskMaker.ID;
import static org.tasks.makers.TaskMaker.RANDOM_REMINDER_PERIOD;
import static org.tasks.makers.TaskMaker.REMINDERS;
import static org.tasks.makers.TaskMaker.REMINDER_LAST;
import static org.tasks.makers.TaskMaker.SNOOZE_TIME;
import static org.tasks.makers.TaskMaker.newTask;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

@RunWith(AndroidJUnit4.class)
public class ReminderServiceTest extends InjectingTestCase {

    @Inject Preferences preferences;

    private ReminderService service;
    private JobQueue<Reminder> jobs;

    @Before
    public void before() {
        jobs = mock(JobQueue.class);
        service = new ReminderService(preferences, jobs);
    }

    @After
    public void after() {
        verifyNoMoreInteractions(jobs);
    }

    @Override
    protected void inject(TestComponent component) {
        component.inject(this);
    }

    @Test
    public void dontScheduleDueDateReminderWhenFlagNotSet() {
        service.scheduleAlarm(null, newTask(with(ID, 1L), with(DUE_TIME, newDateTime())));

        verify(jobs).cancel(1);
    }

    @Test
    public void dontScheduleDueDateReminderWhenTimeNotSet() {
        service.scheduleAlarm(null, newTask(with(ID, 1L), with(REMINDERS, NOTIFY_AT_DEADLINE)));

        verify(jobs).cancel(1);
    }

    @Test
    public void schedulePastDueDate() {
        Task task = newTask(
                with(ID, 1L),
                with(DUE_TIME, newDateTime().minusDays(1)),
                with(REMINDERS, NOTIFY_AT_DEADLINE));
        service.scheduleAlarm(null, task);

        InOrder order = inOrder(jobs);
        order.verify(jobs).cancel(1);
        order.verify(jobs).add(new Reminder(1, task.getDueDate(), ReminderService.TYPE_DUE));
    }

    @Test
    public void scheduleFutureDueDate() {
        Task task = newTask(
                with(ID, 1L),
                with(DUE_TIME, newDateTime().plusDays(1)),
                with(REMINDERS, NOTIFY_AT_DEADLINE));
        service.scheduleAlarm(null, task);

        InOrder order = inOrder(jobs);
        order.verify(jobs).cancel(1);
        order.verify(jobs).add(new Reminder(1, task.getDueDate(), ReminderService.TYPE_DUE));
    }

    @Test
    public void scheduleReminderAtDefaultDueTime() {
        DateTime now = newDateTime();
        Task task = newTask(
                with(ID, 1L),
                with(DUE_DATE, now),
                with(REMINDERS, NOTIFY_AT_DEADLINE));
        service.scheduleAlarm(null, task);

        InOrder order = inOrder(jobs);
        order.verify(jobs).cancel(1);
        order.verify(jobs).add(new Reminder(1, now.startOfDay().withHourOfDay(18).getMillis(), ReminderService.TYPE_DUE));
    }

    @Test
    public void dontScheduleReminderForCompletedTask() {
        Task task = newTask(
                with(ID, 1L),
                with(DUE_TIME, newDateTime().plusDays(1)),
                with(COMPLETION_TIME, newDateTime()),
                with(REMINDERS, NOTIFY_AT_DEADLINE));

        service.scheduleAlarm(null, task);

        verify(jobs).cancel(1);
    }

    @Test
    public void dontScheduleReminderForDeletedTask() {
        Task task = newTask(
                with(ID, 1L),
                with(DUE_TIME, newDateTime().plusDays(1)),
                with(DELETION_TIME, newDateTime()),
                with(REMINDERS, NOTIFY_AT_DEADLINE));

        service.scheduleAlarm(null, task);

        verify(jobs).cancel(1);
    }

    @Test
    public void dontScheduleDueDateReminderWhenAlreadyReminded() {
        DateTime now = newDateTime();
        Task task = newTask(
                with(ID, 1L),
                with(DUE_TIME, now),
                with(REMINDER_LAST, now.plusSeconds(1)),
                with(REMINDERS, NOTIFY_AT_DEADLINE));

        service.scheduleAlarm(null, task);

        verify(jobs).cancel(1);
    }

    @Test
    public void snoozeOverridesAll() {
        DateTime now = newDateTime();
        Task task = newTask(
                with(ID, 1L),
                with(DUE_TIME, now),
                with(SNOOZE_TIME, now.plusMonths(12)),
                with(REMINDERS, NOTIFY_AT_DEADLINE | NOTIFY_AFTER_DEADLINE),
                with(RANDOM_REMINDER_PERIOD, TimeUnit.HOURS.toMillis(1)));

        service.scheduleAlarm(null, task);

        InOrder order = inOrder(jobs);
        order.verify(jobs).cancel(1);
        order.verify(jobs).add(new Reminder(1, now.plusMonths(12).getMillis(), ReminderService.TYPE_SNOOZE));
    }

    @Test
    public void ignoreLapsedSnoozeTime() {
        Task task = newTask(
                with(ID, 1L),
                with(DUE_TIME, newDateTime()),
                with(SNOOZE_TIME, newDateTime().minusMinutes(5)),
                with(REMINDERS, NOTIFY_AT_DEADLINE));
        service.scheduleAlarm(null, task);

        InOrder order = inOrder(jobs);
        order.verify(jobs).cancel(1);
        order.verify(jobs).add(new Reminder(1, task.getDueDate(), ReminderService.TYPE_DUE));

    }

    @Test
    public void scheduleRandomReminder() {
        freezeClock().thawAfter(new Snippet() {{
            Task task = newTask(
                    with(ID, 1L),
                    with(RANDOM_REMINDER_PERIOD, TimeUnit.DAYS.toMillis(7)));
            service.scheduleAlarm(null, task);

            ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
            InOrder order = inOrder(jobs);
            order.verify(jobs).cancel(1);
            order.verify(jobs).add(captor.capture());

            Reminder reminder = captor.getValue();
            assertTrue(reminder.getTime() > currentTimeMillis());
            assertTrue(reminder.getTime() < currentTimeMillis() + 1.2 * TimeUnit.DAYS.toMillis(7));
            assertEquals(TYPE_RANDOM, reminder.getType());
        }});
    }

//    @Test
//    public void testOverdue() {
//        // test due date in the future
//        reminderService.setScheduler(new AlarmExpected() {
//            @Override
//            public void createAlarm(Task task, long time, int type) {
//                if (time == ReminderService.NO_ALARM)
//                    return;
//                super.createAlarm(task, time, type);
//                assertTrue(time > task.getDueDate());
//                assertTrue(time < task.getDueDate() + DateUtilities.ONE_DAY);
//                assertEquals(type, ReminderService.TYPE_OVERDUE);
//            }
//        });
//        final Task task = new Task();
//        task.setTitle("water");
//        task.setDueDate(DateUtilities.now() + DateUtilities.ONE_DAY);
//        task.setReminderFlags(Task.NOTIFY_AFTER_DEADLINE);
//        taskDao.save(task);
//
//        // test due date in the past
//        task.setDueDate(DateUtilities.now() - DateUtilities.ONE_DAY);
//        reminderService.setScheduler(new AlarmExpected() {
//            @Override
//            public void createAlarm(Task task, long time, int type) {
//                if (time == ReminderService.NO_ALARM)
//                    return;
//                super.createAlarm(task, time, type);
//                assertTrue(time > DateUtilities.now() - 1000L);
//                assertTrue(time < DateUtilities.now() + 2 * DateUtilities.ONE_DAY);
//                assertEquals(type, ReminderService.TYPE_OVERDUE);
//            }
//        });
//        taskDao.save(task);
//        assertTrue(((AlarmExpected) reminderService.getScheduler()).alarmCreated);
//
//        // test due date in the past, but recently notified
//        task.setReminderLast(DateUtilities.now());
//        reminderService.setScheduler(new AlarmExpected() {
//            @Override
//            public void createAlarm(Task task, long time, int type) {
//                if (time == ReminderService.NO_ALARM)
//                    return;
//                super.createAlarm(task, time, type);
//                assertTrue(time > DateUtilities.now() + DateUtilities.ONE_HOUR);
//                assertTrue(time < DateUtilities.now() + DateUtilities.ONE_DAY);
//                assertEquals(type, ReminderService.TYPE_OVERDUE);
//            }
//        });
//        taskDao.save(task);
//        assertTrue(((AlarmExpected) reminderService.getScheduler()).alarmCreated);
//    }
//
//    @Test
//    public void testMultipleReminders() {
//        // test due date in the future, enable random
//        final Task task = new Task();
//        task.setTitle("water");
//        task.setDueDate(DateUtilities.now() + DateUtilities.ONE_WEEK);
//        task.setReminderFlags(Task.NOTIFY_AT_DEADLINE);
//        task.setReminderPeriod(DateUtilities.ONE_HOUR);
//        reminderService.setScheduler(new AlarmExpected() {
//            @Override
//            public void createAlarm(Task task, long time, int type) {
//                if (time == ReminderService.NO_ALARM)
//                    return;
//                super.createAlarm(task, time, type);
//                assertTrue(time > DateUtilities.now());
//                assertTrue(time < DateUtilities.now() + DateUtilities.ONE_DAY);
//                assertEquals(type, ReminderService.TYPE_RANDOM);
//            }
//        });
//        taskDao.save(task);
//        assertTrue(((AlarmExpected) reminderService.getScheduler()).alarmCreated);
//
//        // now set the due date in the past
//        task.setDueDate(DateUtilities.now() - DateUtilities.ONE_WEEK);
//        ((AlarmExpected) reminderService.getScheduler()).alarmCreated = false;
//        reminderService.scheduleAlarm(taskDao, task);
//        assertTrue(((AlarmExpected) reminderService.getScheduler()).alarmCreated);
//
//        // now set the due date before the random
//        task.setDueDate(DateUtilities.now() + DateUtilities.ONE_HOUR);
//        reminderService.setScheduler(new AlarmExpected() {
//            @Override
//            public void createAlarm(Task task, long time, int type) {
//                if (time == ReminderService.NO_ALARM)
//                    return;
//                super.createAlarm(task, time, type);
//                assertEquals((long) task.getDueDate(), time);
//                assertEquals(type, ReminderService.TYPE_DUE);
//            }
//        });
//        taskDao.save(task);
//        assertTrue(((AlarmExpected) reminderService.getScheduler()).alarmCreated);
//    }
}
