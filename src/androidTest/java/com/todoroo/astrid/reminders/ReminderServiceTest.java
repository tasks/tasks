package com.todoroo.astrid.reminders;

import android.support.test.runner.AndroidJUnit4;

import com.todoroo.astrid.data.Task;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.tasks.Snippet;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.jobs.JobQueue;
import org.tasks.jobs.Reminder;
import org.tasks.preferences.Preferences;
import org.tasks.reminders.Random;
import org.tasks.time.DateTime;

import javax.inject.Inject;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static com.todoroo.andlib.utility.DateUtilities.ONE_HOUR;
import static com.todoroo.andlib.utility.DateUtilities.ONE_WEEK;
import static com.todoroo.astrid.data.Task.NOTIFY_AFTER_DEADLINE;
import static com.todoroo.astrid.data.Task.NOTIFY_AT_DEADLINE;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.tasks.Freeze.freezeClock;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.makers.TaskMaker.COMPLETION_TIME;
import static org.tasks.makers.TaskMaker.CREATION_TIME;
import static org.tasks.makers.TaskMaker.DELETION_TIME;
import static org.tasks.makers.TaskMaker.DUE_DATE;
import static org.tasks.makers.TaskMaker.DUE_TIME;
import static org.tasks.makers.TaskMaker.ID;
import static org.tasks.makers.TaskMaker.PRIORITY;
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
    private Random random;
    private JobQueue<Reminder> jobs;

    @Before
    public void before() {
        jobs = mock(JobQueue.class);
        random = mock(Random.class);
        when(random.nextFloat()).thenReturn(1.0f);
        service = new ReminderService(preferences, jobs, random);
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
    public void scheduleInitialRandomReminder() {
        freezeClock().thawAfter(new Snippet() {{
            DateTime now = newDateTime();
            when(random.nextFloat()).thenReturn(0.3865f);
            Task task = newTask(
                    with(ID, 1L),
                    with(REMINDER_LAST, (DateTime) null),
                    with(CREATION_TIME, now.minusDays(1)),
                    with(RANDOM_REMINDER_PERIOD, ONE_WEEK));

            service.scheduleAlarm(null, task);

            InOrder order = inOrder(jobs);
            order.verify(jobs).cancel(1);
            order.verify(jobs).add(new Reminder(1L, now.minusDays(1).getMillis() + 584206592, ReminderService.TYPE_RANDOM));
        }});
    }

    @Test
    public void scheduleNextRandomReminder() {
        freezeClock().thawAfter(new Snippet() {{
            DateTime now = newDateTime();
            when(random.nextFloat()).thenReturn(0.3865f);
            Task task = newTask(
                    with(ID, 1L),
                    with(REMINDER_LAST, now.minusDays(1)),
                    with(CREATION_TIME, now.minusDays(30)),
                    with(RANDOM_REMINDER_PERIOD, ONE_WEEK));

            service.scheduleAlarm(null, task);

            InOrder order = inOrder(jobs);
            order.verify(jobs).cancel(1);
            order.verify(jobs).add(new Reminder(1L, now.minusDays(1).getMillis() + 584206592, ReminderService.TYPE_RANDOM));
        }});
    }

    @Test
    public void scheduleOverdueRandomReminder() {
        freezeClock().thawAfter(new Snippet() {{
            DateTime now = newDateTime();
            when(random.nextFloat()).thenReturn(0.3865f);
            Task task = newTask(
                    with(ID, 1L),
                    with(REMINDER_LAST, now.minusDays(14)),
                    with(CREATION_TIME, now.minusDays(30)),
                    with(RANDOM_REMINDER_PERIOD, ONE_WEEK));

            service.scheduleAlarm(null, task);

            InOrder order = inOrder(jobs);
            order.verify(jobs).cancel(1);
            order.verify(jobs).add(new Reminder(1L, now.getMillis() + 10148400, ReminderService.TYPE_RANDOM));
        }});
    }

    @Test
    public void scheduleOverdueForFutureDueDate() {
        freezeClock().thawAfter(new Snippet() {{
            when(random.nextFloat()).thenReturn(0.3865f);
            Task task = newTask(
                    with(ID, 1L),
                    with(DUE_TIME, newDateTime().plusMinutes(5)),
                    with(REMINDERS, NOTIFY_AFTER_DEADLINE));

            service.scheduleAlarm(null, task);

            InOrder order = inOrder(jobs);
            order.verify(jobs).cancel(1);
            order.verify(jobs).add(new Reminder(1L, task.getDueDate() + 4582800, ReminderService.TYPE_OVERDUE));
        }});
    }

    @Test
    public void scheduleOverdueForPastDueDateWithNoReminderPastDueDate() {
        freezeClock().thawAfter(new Snippet() {{
            DateTime now = newDateTime();
            Task task = newTask(
                    with(ID, 1L),
                    with(DUE_TIME, now.minusMinutes(5)),
                    with(REMINDERS, NOTIFY_AFTER_DEADLINE));

            service.scheduleAlarm(null, task);

            InOrder order = inOrder(jobs);
            order.verify(jobs).cancel(1);
            order.verify(jobs).add(new Reminder(1L, currentTimeMillis(), ReminderService.TYPE_OVERDUE));
        }});
    }

    @Test
    public void scheduleOverdueForPastDueDateLastReminderSixHoursAgo() {
        freezeClock().thawAfter(new Snippet() {{
            Task task = newTask(
                    with(ID, 1L),
                    with(DUE_TIME, newDateTime().minusHours(12)),
                    with(REMINDER_LAST, newDateTime().minusHours(6)),
                    with(REMINDERS, NOTIFY_AFTER_DEADLINE));

            service.scheduleAlarm(null, task);

            InOrder order = inOrder(jobs);
            order.verify(jobs).cancel(1);
            order.verify(jobs).add(new Reminder(1L, currentTimeMillis(), ReminderService.TYPE_OVERDUE));
        }});
    }

    @Test
    public void scheduleOverdueForPastDueDateLastReminderWithinSixHours() {
        freezeClock().thawAfter(new Snippet() {{
            when(random.nextFloat()).thenReturn(0.3865f);
            Task task = newTask(
                    with(ID, 1L),
                    with(DUE_TIME, newDateTime().minusHours(12)),
                    with(PRIORITY, 2),
                    with(REMINDER_LAST, newDateTime().minusHours(3)),
                    with(REMINDERS, NOTIFY_AFTER_DEADLINE));

            service.scheduleAlarm(null, task);

            InOrder order = inOrder(jobs);
            order.verify(jobs).cancel(1);
            order.verify(jobs).add(new Reminder(1L, currentTimeMillis() + 22748400, ReminderService.TYPE_OVERDUE));
        }});
    }

    @Test
    public void snoozeOverridesAll() {
        DateTime now = newDateTime();
        Task task = newTask(
                with(ID, 1L),
                with(DUE_TIME, now),
                with(SNOOZE_TIME, now.plusMonths(12)),
                with(REMINDERS, NOTIFY_AT_DEADLINE | NOTIFY_AFTER_DEADLINE),
                with(RANDOM_REMINDER_PERIOD, ONE_HOUR));

        service.scheduleAlarm(null, task);

        InOrder order = inOrder(jobs);
        order.verify(jobs).cancel(1);
        order.verify(jobs).add(new Reminder(1, now.plusMonths(12).getMillis(), ReminderService.TYPE_SNOOZE));
    }

    @Test
    @Ignore
    public void randomReminderBeforeDueAndOverdue() {

    }

    @Test
    @Ignore
    public void randomReminderAfterDue() {

    }

    @Test
    @Ignore
    public void randomReminderAfterOverdue() {

    }

    @Test
    @Ignore
    public void dueDateBeforeOverdue() {

    }
}
