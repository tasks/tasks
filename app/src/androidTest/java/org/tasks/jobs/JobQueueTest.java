package org.tasks.jobs;

import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.tasks.Freeze;
import org.tasks.Snippet;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.todoroo.astrid.reminders.ReminderService.TYPE_DUE;
import static com.todoroo.astrid.reminders.ReminderService.TYPE_SNOOZE;
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
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

@RunWith(AndroidJUnit4.class)
public class JobQueueTest {

    private static final long ONE_MINUTE = TimeUnit.MINUTES.toMillis(1);
    private static final String TAG = NotificationJob.TAG;

    private JobQueue queue;
    private JobManager jobManager;
    private Preferences preferences;

    @Before
    public void before() {
        preferences = mock(Preferences.class);
        when(preferences.adjustForQuietHours(anyLong())).then(returnsFirstArg());
        jobManager = mock(JobManager.class);
        queue = new JobQueue(preferences, jobManager);
    }

    @After
    public void after() {
        verifyNoMoreInteractions(jobManager);
    }

    @Test
    public void alarmAndReminderSameTimeSameID() {
        long now = currentTimeMillis();

        queue.add(new Reminder(1, now, TYPE_DUE));
        queue.add(new Alarm(1, 1, now));

        verify(jobManager).schedule(TAG, now);

        Freeze.freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(
                    asList(new Reminder(1, now, TYPE_DUE),
                            new Alarm(1, 1, now)),
                    queue.getOverdueJobs());
        }});
    }

    @Test
    public void removeAlarmLeaveReminder() {
        long now = currentTimeMillis();

        queue.add(new Reminder(1, now, TYPE_DUE));
        queue.add(new Alarm(1, 1, now));

        verify(jobManager).schedule(TAG, now);

        queue.remove(singletonList(new Alarm(1, 1, now)));

        Freeze.freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(
                    singletonList(new Reminder(1, now, TYPE_DUE)),
                    queue.getOverdueJobs());
        }});
    }

    @Test
    public void removeReminderLeaveAlarm() {
        long now = currentTimeMillis();

        queue.add(new Reminder(1, now, TYPE_DUE));
        queue.add(new Alarm(1, 1, now));

        verify(jobManager).schedule(TAG, now);

        queue.remove(singletonList(new Reminder(1, now, TYPE_DUE)));

        Freeze.freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(
                    singletonList(new Alarm(1, 1, now)),
                    queue.getOverdueJobs());
        }});
    }

    @Test
    public void twoJobsAtSameTime() {
        queue.add(new Reminder(1, 1, 0));
        queue.add(new Reminder(2, 1, 0));

        verify(jobManager).schedule(TAG, 1);

        assertEquals(2, queue.size());
    }

    @Test
    public void rescheduleForFirstJob() {
        queue.add(new Reminder(1, 1, 0));

        verify(jobManager).schedule(TAG, 1);
    }

    @Test
    public void dontRescheduleForLaterJobs() {
        queue.add(new Reminder(1, 1, 0));
        queue.add(new Reminder(2, 2, 0));

        verify(jobManager).schedule(TAG, 1);
    }

    @Test
    public void rescheduleForNewerJob() {
        queue.add(new Reminder(1, 2, 0));
        queue.add(new Reminder(1, 1, 0));

        InOrder order = inOrder(jobManager);
        order.verify(jobManager).schedule(TAG, 2);
        order.verify(jobManager).schedule(TAG, 1);
    }

    @Test
    public void rescheduleWhenCancelingOnlyJob() {
        queue.add(new Reminder(1, 2, 0));
        queue.cancelReminder(1);

        InOrder order = inOrder(jobManager);
        order.verify(jobManager).schedule(TAG, 2);
        order.verify(jobManager).cancel(TAG);
    }

    @Test
    public void rescheduleWhenCancelingFirstJob() {
        queue.add(new Reminder(1, 1, 0));
        queue.add(new Reminder(2, 2, 0));

        queue.cancelReminder(1);

        InOrder order = inOrder(jobManager);
        order.verify(jobManager).schedule(TAG, 1);
        order.verify(jobManager).schedule(TAG, 2);
    }

    @Test
    public void dontRescheduleWhenCancelingLaterJob() {
        queue.add(new Reminder(1, 1, 0));
        queue.add(new Reminder(2, 2, 0));

        queue.cancelReminder(2);

        verify(jobManager).schedule(TAG, 1);
    }

    @Test
    public void nextScheduledTimeIsZeroWhenQueueIsEmpty() {
        when(preferences.adjustForQuietHours(anyLong())).thenReturn(1234L);

        assertEquals(0, queue.nextScheduledTime());
    }

    @Test
    public void adjustNextScheduledTimeForQuietHours() {
        when(preferences.adjustForQuietHours(anyLong())).thenReturn(1234L);
        queue.add(new Reminder(1, 1, 1));

        verify(jobManager).schedule(TAG, 1234);
    }

    @Test
    public void overdueJobsAreReturned() {
        long now = currentTimeMillis();

        queue.add(new Reminder(1, now, TYPE_DUE));
        queue.add(new Reminder(2, now + ONE_MINUTE, TYPE_DUE));

        verify(jobManager).schedule(TAG, now);

        Freeze.freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(
                    singletonList(new Reminder(1, now, TYPE_DUE)),
                    queue.getOverdueJobs());
        }});
    }

    @Test
    public void twoOverdueJobsAtSameTimeReturned() {
        long now = currentTimeMillis();

        queue.add(new Reminder(1, now, TYPE_DUE));
        queue.add(new Reminder(2, now, TYPE_DUE));

        verify(jobManager).schedule(TAG, now);

        Freeze.freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(
                    asList(new Reminder(1, now, TYPE_DUE), new Reminder(2, now, TYPE_DUE)),
                    queue.getOverdueJobs());
        }});
    }

    @Test
    public void twoOverdueJobsAtDifferentTimes() {
        long now = currentTimeMillis();

        queue.add(new Reminder(1, now, TYPE_DUE));
        queue.add(new Reminder(2, now + ONE_MINUTE, TYPE_DUE));

        verify(jobManager).schedule(TAG, now);

        Freeze.freezeAt(now + 2 * ONE_MINUTE).thawAfter(new Snippet() {{
            assertEquals(
                    asList(new Reminder(1, now, TYPE_DUE), new Reminder(2, now + ONE_MINUTE, TYPE_DUE)),
                    queue.getOverdueJobs());
        }});
    }

    @Test
    public void overdueJobsAreRemoved() {
        long now = currentTimeMillis();

        queue.add(new Reminder(1, now, TYPE_DUE));
        queue.add(new Reminder(2, now + ONE_MINUTE, TYPE_DUE));

        verify(jobManager).schedule(TAG, now);

        Freeze.freezeAt(now).thawAfter(new Snippet() {{
            queue.remove(queue.getOverdueJobs());
        }});

        assertEquals(
                singletonList(new Reminder(2, now + ONE_MINUTE, TYPE_DUE)),
                queue.getJobs());
    }

    @Test
    public void multipleOverduePeriodsLapsed() {
        long now = currentTimeMillis();

        queue.add(new Reminder(1, now, TYPE_DUE));
        queue.add(new Reminder(2, now + ONE_MINUTE, TYPE_DUE));
        queue.add(new Reminder(3, now + 2 * ONE_MINUTE, TYPE_DUE));

        verify(jobManager).schedule(TAG, now);

        Freeze.freezeAt(now + ONE_MINUTE).thawAfter(new Snippet() {{
            queue.remove(queue.getOverdueJobs());
        }});

        assertEquals(
                singletonList(new Reminder(3, now + 2 * ONE_MINUTE, TYPE_DUE)),
                queue.getJobs());
    }

    @Test
    public void clearShouldCancelExisting() {
        queue.add(new Reminder(1, 1, 0));

        queue.clear();

        InOrder order = inOrder(jobManager);
        order.verify(jobManager).schedule(TAG, 1);
        order.verify(jobManager).cancel(TAG);
        assertEquals(0, queue.size());
    }

    @Test
    public void ignoreInvalidCancel() {
        long now = currentTimeMillis();

        queue.add(new Reminder(1, now, TYPE_DUE));
        queue.cancelReminder(2);

        verify(jobManager).schedule(TAG, now);
    }

    @Test
    public void allDuringSameMinuteAreOverdue() {
        DateTime now = new DateTime(2017, 9, 3, 0, 14, 6, 455);
        DateTime due = new DateTime(2017, 9, 3, 0, 14, 0, 0);
        DateTime snooze = new DateTime(2017, 9, 3, 0, 14, 59, 999);

        queue.add(new Reminder(1, due.getMillis(), TYPE_DUE));
        queue.add(new Reminder(2, snooze.getMillis(), TYPE_SNOOZE));
        queue.add(new Reminder(3, due.plusMinutes(1).getMillis(), TYPE_DUE));

        verify(jobManager).schedule(TAG, due.getMillis());

        Freeze.freezeAt(now).thawAfter(new Snippet() {{
            List<? extends JobQueueEntry> overdueJobs = queue.getOverdueJobs();
            assertEquals(
                    asList(new Reminder(1, due.getMillis(), TYPE_DUE), new Reminder(2, snooze.getMillis(), TYPE_SNOOZE)),
                    overdueJobs);
            queue.remove(overdueJobs);
            assertEquals(
                    singletonList(new Reminder(3, due.plusMinutes(1).getMillis(), TYPE_DUE)),
                    queue.getJobs());
        }});
    }
}
