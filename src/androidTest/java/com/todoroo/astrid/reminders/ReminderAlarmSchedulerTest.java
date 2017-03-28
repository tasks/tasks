package com.todoroo.astrid.reminders;

import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.tasks.Freeze;
import org.tasks.Snippet;
import org.tasks.jobs.JobManager;
import org.tasks.jobs.Reminder;
import org.tasks.makers.TaskMaker;
import org.tasks.preferences.Preferences;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static com.todoroo.astrid.reminders.ReminderService.TYPE_DUE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.tasks.makers.TaskMaker.newTask;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

@RunWith(AndroidJUnit4.class)
public class ReminderAlarmSchedulerTest {

    private static final long ONE_MINUTE = TimeUnit.MINUTES.toMillis(1);

    private JobManager jobManager;
    private ReminderAlarmScheduler scheduler;

    @Before
    public void before() {
        jobManager = mock(JobManager.class);
        Preferences preferences = mock(Preferences.class);
        when(preferences.adjustForQuietHours(anyLong())).then(returnsFirstArg());
        scheduler = new ReminderAlarmScheduler(jobManager, preferences);
    }

    @After
    public void after() {
        verifyNoMoreInteractions(jobManager);
    }

    @Test
    public void scheduleFirstReminder() {
        long now = currentTimeMillis();

        scheduler.createAlarm(newTask(with(TaskMaker.ID, 1L)), now, 0);

        verify(jobManager).scheduleReminder(now, true);
    }

    @Test
    public void dontScheduleLaterReminder() {
        long now = currentTimeMillis();

        scheduler.createAlarm(newTask(with(TaskMaker.ID, 1L)), now, 0);
        scheduler.createAlarm(newTask(with(TaskMaker.ID, 1L)), now + ONE_MINUTE, 0);

        verify(jobManager).scheduleReminder(now, true);
    }

    @Test
    public void rescheduleNewerReminder() {
        long now = currentTimeMillis();

        scheduler.createAlarm(newTask(with(TaskMaker.ID, 1L)), now, 0);
        scheduler.createAlarm(newTask(with(TaskMaker.ID, 2L)), now - ONE_MINUTE, 0);

        InOrder order = inOrder(jobManager);
        order.verify(jobManager).scheduleReminder(now, true);
        order.verify(jobManager).scheduleReminder(now - ONE_MINUTE, true);
    }

    @Test
    public void removeLastReminderCancelsJob() {
        long now = currentTimeMillis();
        scheduler.createAlarm(newTask(with(TaskMaker.ID, 1L)), now, 0);

        scheduler.createAlarm(newTask(with(TaskMaker.ID, 1L)), 0, 0);

        InOrder order = inOrder(jobManager);
        order.verify(jobManager).scheduleReminder(now, true);
        order.verify(jobManager).cancelReminders();
    }

    @Test
    public void removePastRemindersReturnsPastReminder() {
        long now = currentTimeMillis();

        Freeze.freezeAt(now).thawAfter(new Snippet() {{
            scheduler.createAlarm(newTask(with(TaskMaker.ID, 1L)), now, TYPE_DUE);

            List<Reminder> reminders = scheduler.removePastReminders();

            verify(jobManager).scheduleReminder(now, true);

            assertEquals(singletonList(new Reminder(1, now, TYPE_DUE)), reminders);
        }});
    }

    @Test
    public void dontRescheduleForSecondJobAtSameTime() {
        long now = currentTimeMillis();

        scheduler.createAlarm(newTask(with(TaskMaker.ID, 1L)), now, TYPE_DUE);
        scheduler.createAlarm(newTask(with(TaskMaker.ID, 2L)), now, TYPE_DUE);

        verify(jobManager).scheduleReminder(now, true);
    }

    @Test
    public void removePastRemindersReturnsPastRemindersAtSameTime() {
        long now = currentTimeMillis();

        Freeze.freezeAt(now).thawAfter(new Snippet() {{
            scheduler.createAlarm(newTask(with(TaskMaker.ID, 1L)), now, TYPE_DUE);
            scheduler.createAlarm(newTask(with(TaskMaker.ID, 2L)), now, TYPE_DUE);

            List<Reminder> reminders = scheduler.removePastReminders();

            verify(jobManager).scheduleReminder(now, true);

            assertEquals(asList(new Reminder(1, now, TYPE_DUE), new Reminder(2, now, TYPE_DUE)), reminders);
        }});
    }
}
