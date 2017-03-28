package org.tasks.jobs;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.Freeze;
import org.tasks.Snippet;
import org.tasks.preferences.Preferences;

import java.util.concurrent.TimeUnit;

import static com.todoroo.astrid.reminders.ReminderService.TYPE_DUE;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

@RunWith(AndroidJUnit4.class)
public class JobQueueTest {

    private static final long ONE_MINUTE = TimeUnit.MINUTES.toMillis(1);

    private JobQueue<Reminder> queue;
    private Preferences preferences;

    @Before
    public void before() {
        preferences = mock(Preferences.class);
        when(preferences.adjustForQuietHours(anyLong())).then(returnsFirstArg());
        queue = new JobQueue<>(preferences);
    }

    @Test
    public void twoJobsAtSameTime() {
        queue.add(new Reminder(1, 1, 0));
        queue.add(new Reminder(2, 1, 0));

        assertEquals(2, queue.size());
    }

    @Test
    public void rescheduleForFirstJob() {
        assertTrue(queue.add(new Reminder(1, 1, 0)));
    }

    @Test
    public void dontRescheduleForLaterJobs() {
        queue.add(new Reminder(1, 1, 0));

        assertFalse(queue.add(new Reminder(2, 2, 0)));
    }

    @Test
    public void rescheduleForNewerJob() {
        queue.add(new Reminder(1, 2, 0));

        assertTrue(queue.add(new Reminder(1, 1, 0)));
    }

    @Test
    public void rescheduleWhenCancelingOnlyJob() {
        queue.add(new Reminder(1, 2, 0));

        assertTrue(queue.cancel(1));
    }

    @Test
    public void rescheduleWhenCancelingFirstJob() {
        queue.add(new Reminder(1, 1, 0));
        queue.add(new Reminder(2, 2, 0));

        assertTrue(queue.cancel(1));
    }

    @Test
    public void dontRescheduleWhenCancelingLaterJob() {
        queue.add(new Reminder(1, 1, 0));
        queue.add(new Reminder(2, 2, 0));

        assertFalse(queue.cancel(2));
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

        assertEquals(1234, queue.nextScheduledTime());
    }

    @Test
    public void overdueJobsAreReturned() {
        long now = currentTimeMillis();

        queue.add(new Reminder(1, now, TYPE_DUE));
        queue.add(new Reminder(2, now + ONE_MINUTE, TYPE_DUE));

        Freeze.freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(
                    singletonList(new Reminder(1, now, TYPE_DUE)),
                    queue.removeOverdueJobs());
        }});
    }

    @Test
    public void overdueJobsAreRemoved() {
        long now = currentTimeMillis();

        queue.add(new Reminder(1, now, TYPE_DUE));
        queue.add(new Reminder(2, now + ONE_MINUTE, TYPE_DUE));

        Freeze.freezeAt(now).thawAfter(new Snippet() {{
            queue.removeOverdueJobs();
        }});

        assertEquals(
                singletonList(new Reminder(2, now + ONE_MINUTE, TYPE_DUE)),
                queue.getJobs());
    }
}
