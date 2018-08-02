package org.tasks.jobs;

import static com.google.common.collect.Sets.newHashSet;
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

import android.support.test.runner.AndroidJUnit4;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.tasks.Freeze;
import org.tasks.Snippet;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

@RunWith(AndroidJUnit4.class)
public class NotificationQueueTest {

  private static final long ONE_MINUTE = TimeUnit.MINUTES.toMillis(1);

  private NotificationQueue queue;
  private WorkManager workManager;
  private Preferences preferences;

  @Before
  public void before() {
    preferences = mock(Preferences.class);
    when(preferences.adjustForQuietHours(anyLong())).then(returnsFirstArg());
    workManager = mock(WorkManager.class);
    queue = new NotificationQueue(preferences, workManager);
  }

  @After
  public void after() {
    verifyNoMoreInteractions(workManager);
  }

  @Test
  public void alarmAndReminderSameTimeSameID() {
    long now = currentTimeMillis();

    queue.add(new ReminderEntry(1, now, TYPE_DUE));
    queue.add(new AlarmEntry(1, 1, now));

    verify(workManager).scheduleNotification(now, true);

    Freeze.freezeAt(now)
        .thawAfter(
            new Snippet() {
              {
                assertEquals(
                    newHashSet(new AlarmEntry(1, 1, now), new ReminderEntry(1, now, TYPE_DUE)),
                    newHashSet(queue.getOverdueJobs()));
              }
            });
  }

  @Test
  public void removeAlarmLeaveReminder() {
    long now = currentTimeMillis();

    queue.add(new ReminderEntry(1, now, TYPE_DUE));
    queue.add(new AlarmEntry(1, 1, now));

    verify(workManager).scheduleNotification(now, true);

    queue.remove(singletonList(new AlarmEntry(1, 1, now)));

    Freeze.freezeAt(now)
        .thawAfter(
            new Snippet() {
              {
                assertEquals(
                    singletonList(new ReminderEntry(1, now, TYPE_DUE)), queue.getOverdueJobs());
              }
            });
  }

  @Test
  public void removeReminderLeaveAlarm() {
    long now = currentTimeMillis();

    queue.add(new ReminderEntry(1, now, TYPE_DUE));
    queue.add(new AlarmEntry(1, 1, now));

    verify(workManager).scheduleNotification(now, true);

    queue.remove(singletonList(new ReminderEntry(1, now, TYPE_DUE)));

    Freeze.freezeAt(now)
        .thawAfter(
            new Snippet() {
              {
                assertEquals(singletonList(new AlarmEntry(1, 1, now)), queue.getOverdueJobs());
              }
            });
  }

  @Test
  public void twoJobsAtSameTime() {
    queue.add(new ReminderEntry(1, 1, 0));
    queue.add(new ReminderEntry(2, 1, 0));

    verify(workManager).scheduleNotification(1, true);

    assertEquals(2, queue.size());
  }

  @Test
  public void rescheduleForFirstJob() {
    queue.add(new ReminderEntry(1, 1, 0));

    verify(workManager).scheduleNotification(1, true);
  }

  @Test
  public void dontRescheduleForLaterJobs() {
    queue.add(new ReminderEntry(1, 1, 0));
    queue.add(new ReminderEntry(2, 2, 0));

    verify(workManager).scheduleNotification(1, true);
  }

  @Test
  public void rescheduleForNewerJob() {
    queue.add(new ReminderEntry(1, 2, 0));
    queue.add(new ReminderEntry(1, 1, 0));

    InOrder order = inOrder(workManager);
    order.verify(workManager).scheduleNotification(2, true);
    order.verify(workManager).scheduleNotification(1, true);
  }

  @Test
  public void rescheduleWhenCancelingOnlyJob() {
    queue.add(new ReminderEntry(1, 2, 0));
    queue.cancelReminder(1);

    InOrder order = inOrder(workManager);
    order.verify(workManager).scheduleNotification(2, true);
    order.verify(workManager).cancelNotifications();
  }

  @Test
  public void rescheduleWhenCancelingFirstJob() {
    queue.add(new ReminderEntry(1, 1, 0));
    queue.add(new ReminderEntry(2, 2, 0));

    queue.cancelReminder(1);

    InOrder order = inOrder(workManager);
    order.verify(workManager).scheduleNotification(1, true);
    order.verify(workManager).scheduleNotification(2, true);
  }

  @Test
  public void dontRescheduleWhenCancelingLaterJob() {
    queue.add(new ReminderEntry(1, 1, 0));
    queue.add(new ReminderEntry(2, 2, 0));

    queue.cancelReminder(2);

    verify(workManager).scheduleNotification(1, true);
  }

  @Test
  public void nextScheduledTimeIsZeroWhenQueueIsEmpty() {
    when(preferences.adjustForQuietHours(anyLong())).thenReturn(1234L);

    assertEquals(0, queue.nextScheduledTime());
  }

  @Test
  public void adjustNextScheduledTimeForQuietHours() {
    when(preferences.adjustForQuietHours(anyLong())).thenReturn(1234L);
    queue.add(new ReminderEntry(1, 1, 1));

    verify(workManager).scheduleNotification(1234, true);
  }

  @Test
  public void overdueJobsAreReturned() {
    long now = currentTimeMillis();

    queue.add(new ReminderEntry(1, now, TYPE_DUE));
    queue.add(new ReminderEntry(2, now + ONE_MINUTE, TYPE_DUE));

    verify(workManager).scheduleNotification(now, true);

    Freeze.freezeAt(now)
        .thawAfter(
            new Snippet() {
              {
                assertEquals(
                    singletonList(new ReminderEntry(1, now, TYPE_DUE)), queue.getOverdueJobs());
              }
            });
  }

  @Test
  public void twoOverdueJobsAtSameTimeReturned() {
    long now = currentTimeMillis();

    queue.add(new ReminderEntry(1, now, TYPE_DUE));
    queue.add(new ReminderEntry(2, now, TYPE_DUE));

    verify(workManager).scheduleNotification(now, true);

    Freeze.freezeAt(now)
        .thawAfter(
            new Snippet() {
              {
                assertEquals(
                    asList(
                        new ReminderEntry(1, now, TYPE_DUE), new ReminderEntry(2, now, TYPE_DUE)),
                    queue.getOverdueJobs());
              }
            });
  }

  @Test
  public void twoOverdueJobsAtDifferentTimes() {
    long now = currentTimeMillis();

    queue.add(new ReminderEntry(1, now, TYPE_DUE));
    queue.add(new ReminderEntry(2, now + ONE_MINUTE, TYPE_DUE));

    verify(workManager).scheduleNotification(now, true);

    Freeze.freezeAt(now + 2 * ONE_MINUTE)
        .thawAfter(
            new Snippet() {
              {
                assertEquals(
                    asList(
                        new ReminderEntry(1, now, TYPE_DUE),
                        new ReminderEntry(2, now + ONE_MINUTE, TYPE_DUE)),
                    queue.getOverdueJobs());
              }
            });
  }

  @Test
  public void overdueJobsAreRemoved() {
    long now = currentTimeMillis();

    queue.add(new ReminderEntry(1, now, TYPE_DUE));
    queue.add(new ReminderEntry(2, now + ONE_MINUTE, TYPE_DUE));

    verify(workManager).scheduleNotification(now, true);

    Freeze.freezeAt(now)
        .thawAfter(
            new Snippet() {
              {
                queue.remove(queue.getOverdueJobs());
              }
            });

    assertEquals(singletonList(new ReminderEntry(2, now + ONE_MINUTE, TYPE_DUE)), queue.getJobs());
  }

  @Test
  public void multipleOverduePeriodsLapsed() {
    long now = currentTimeMillis();

    queue.add(new ReminderEntry(1, now, TYPE_DUE));
    queue.add(new ReminderEntry(2, now + ONE_MINUTE, TYPE_DUE));
    queue.add(new ReminderEntry(3, now + 2 * ONE_MINUTE, TYPE_DUE));

    verify(workManager).scheduleNotification(now, true);

    Freeze.freezeAt(now + ONE_MINUTE)
        .thawAfter(
            new Snippet() {
              {
                queue.remove(queue.getOverdueJobs());
              }
            });

    assertEquals(
        singletonList(new ReminderEntry(3, now + 2 * ONE_MINUTE, TYPE_DUE)), queue.getJobs());
  }

  @Test
  public void clearShouldCancelExisting() {
    queue.add(new ReminderEntry(1, 1, 0));

    queue.clear();

    InOrder order = inOrder(workManager);
    order.verify(workManager).scheduleNotification(1, true);
    order.verify(workManager).cancelNotifications();
    assertEquals(0, queue.size());
  }

  @Test
  public void ignoreInvalidCancel() {
    long now = currentTimeMillis();

    queue.add(new ReminderEntry(1, now, TYPE_DUE));
    queue.cancelReminder(2);

    verify(workManager).scheduleNotification(now, true);
  }

  @Test
  public void allDuringSameMinuteAreOverdue() {
    DateTime now = new DateTime(2017, 9, 3, 0, 14, 6, 455);
    DateTime due = new DateTime(2017, 9, 3, 0, 14, 0, 0);
    DateTime snooze = new DateTime(2017, 9, 3, 0, 14, 59, 999);

    queue.add(new ReminderEntry(1, due.getMillis(), TYPE_DUE));
    queue.add(new ReminderEntry(2, snooze.getMillis(), TYPE_SNOOZE));
    queue.add(new ReminderEntry(3, due.plusMinutes(1).getMillis(), TYPE_DUE));

    verify(workManager).scheduleNotification(due.getMillis(), true);

    Freeze.freezeAt(now)
        .thawAfter(
            new Snippet() {
              {
                List<? extends NotificationQueueEntry> overdueJobs = queue.getOverdueJobs();
                assertEquals(
                    asList(
                        new ReminderEntry(1, due.getMillis(), TYPE_DUE),
                        new ReminderEntry(2, snooze.getMillis(), TYPE_SNOOZE)),
                    overdueJobs);
                queue.remove(overdueJobs);
                assertEquals(
                    singletonList(new ReminderEntry(3, due.plusMinutes(1).getMillis(), TYPE_DUE)),
                    queue.getJobs());
              }
            });
  }
}
