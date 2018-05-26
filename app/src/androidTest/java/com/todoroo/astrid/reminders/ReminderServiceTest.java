package com.todoroo.astrid.reminders;

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
import static org.tasks.makers.TaskMaker.RANDOM_REMINDER_PERIOD;
import static org.tasks.makers.TaskMaker.REMINDERS;
import static org.tasks.makers.TaskMaker.REMINDER_LAST;
import static org.tasks.makers.TaskMaker.SNOOZE_TIME;
import static org.tasks.makers.TaskMaker.newTask;

import android.support.test.runner.AndroidJUnit4;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.tasks.R;
import org.tasks.Snippet;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.jobs.NotificationQueue;
import org.tasks.jobs.ReminderEntry;
import org.tasks.preferences.Preferences;
import org.tasks.reminders.Random;
import org.tasks.time.DateTime;

@RunWith(AndroidJUnit4.class)
public class ReminderServiceTest extends InjectingTestCase {

  @Inject Preferences preferences;
  @Inject TaskDao taskDao;

  private ReminderService service;
  private Random random;
  private NotificationQueue jobs;

  @Before
  public void before() {
    jobs = mock(NotificationQueue.class);
    random = mock(Random.class);
    when(random.nextFloat()).thenReturn(1.0f);
    preferences.reset();
    service = new ReminderService(preferences, jobs, random, taskDao);
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
    service.scheduleAlarm(newTask(with(ID, 1L), with(DUE_TIME, newDateTime())));

    verify(jobs).cancelReminder(1);
  }

  @Test
  public void dontScheduleDueDateReminderWhenTimeNotSet() {
    service.scheduleAlarm(newTask(with(ID, 1L), with(REMINDERS, NOTIFY_AT_DEADLINE)));

    verify(jobs).cancelReminder(1);
  }

  @Test
  public void schedulePastDueDate() {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, newDateTime().minusDays(1)),
            with(REMINDERS, NOTIFY_AT_DEADLINE));
    service.scheduleAlarm(task);

    InOrder order = inOrder(jobs);
    order.verify(jobs).cancelReminder(1);
    order.verify(jobs).add(new ReminderEntry(1, task.getDueDate(), ReminderService.TYPE_DUE));
  }

  @Test
  public void scheduleFutureDueDate() {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, newDateTime().plusDays(1)),
            with(REMINDERS, NOTIFY_AT_DEADLINE));
    service.scheduleAlarm(task);

    InOrder order = inOrder(jobs);
    order.verify(jobs).cancelReminder(1);
    order.verify(jobs).add(new ReminderEntry(1, task.getDueDate(), ReminderService.TYPE_DUE));
  }

  @Test
  public void scheduleReminderAtDefaultDueTime() {
    DateTime now = newDateTime();
    Task task = newTask(with(ID, 1L), with(DUE_DATE, now), with(REMINDERS, NOTIFY_AT_DEADLINE));

    service.scheduleAlarm(task);

    InOrder order = inOrder(jobs);
    order.verify(jobs).cancelReminder(1);
    order
        .verify(jobs)
        .add(
            new ReminderEntry(
                1, now.startOfDay().withHourOfDay(18).getMillis(), ReminderService.TYPE_DUE));
  }

  @Test
  public void dontScheduleReminderForCompletedTask() {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, newDateTime().plusDays(1)),
            with(COMPLETION_TIME, newDateTime()),
            with(REMINDERS, NOTIFY_AT_DEADLINE));

    service.scheduleAlarm(task);

    verify(jobs).cancelReminder(1);
  }

  @Test
  public void dontScheduleReminderForDeletedTask() {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, newDateTime().plusDays(1)),
            with(DELETION_TIME, newDateTime()),
            with(REMINDERS, NOTIFY_AT_DEADLINE));

    service.scheduleAlarm(task);

    verify(jobs).cancelReminder(1);
  }

  @Test
  public void dontScheduleDueDateReminderWhenAlreadyReminded() {
    DateTime now = newDateTime();
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, now),
            with(REMINDER_LAST, now.plusSeconds(1)),
            with(REMINDERS, NOTIFY_AT_DEADLINE));

    service.scheduleAlarm(task);

    verify(jobs).cancelReminder(1);
  }

  @Test
  public void ignoreStaleSnoozeTime() {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, newDateTime()),
            with(SNOOZE_TIME, newDateTime().minusMinutes(5)),
            with(REMINDER_LAST, newDateTime().minusMinutes(4)),
            with(REMINDERS, NOTIFY_AT_DEADLINE));
    service.scheduleAlarm(task);

    InOrder order = inOrder(jobs);
    order.verify(jobs).cancelReminder(1);
    order.verify(jobs).add(new ReminderEntry(1, task.getDueDate(), ReminderService.TYPE_DUE));
  }

  @Test
  public void dontIgnoreMissedSnoozeTime() {
    DateTime dueDate = newDateTime();
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, dueDate),
            with(SNOOZE_TIME, dueDate.minusMinutes(4)),
            with(REMINDER_LAST, dueDate.minusMinutes(5)),
            with(REMINDERS, NOTIFY_AT_DEADLINE));
    service.scheduleAlarm(task);

    InOrder order = inOrder(jobs);
    order.verify(jobs).cancelReminder(1);
    order
        .verify(jobs)
        .add(new ReminderEntry(1, task.getReminderSnooze(), ReminderService.TYPE_SNOOZE));
  }

  @Test
  public void scheduleInitialRandomReminder() {
    freezeClock()
        .thawAfter(
            new Snippet() {
              {
                DateTime now = newDateTime();
                when(random.nextFloat()).thenReturn(0.3865f);
                Task task =
                    newTask(
                        with(ID, 1L),
                        with(REMINDER_LAST, (DateTime) null),
                        with(CREATION_TIME, now.minusDays(1)),
                        with(RANDOM_REMINDER_PERIOD, ONE_WEEK));

                service.scheduleAlarm(task);

                InOrder order = inOrder(jobs);
                order.verify(jobs).cancelReminder(1);
                order
                    .verify(jobs)
                    .add(
                        new ReminderEntry(
                            1L,
                            now.minusDays(1).getMillis() + 584206592,
                            ReminderService.TYPE_RANDOM));
              }
            });
  }

  @Test
  public void scheduleNextRandomReminder() {
    freezeClock()
        .thawAfter(
            new Snippet() {
              {
                DateTime now = newDateTime();
                when(random.nextFloat()).thenReturn(0.3865f);
                Task task =
                    newTask(
                        with(ID, 1L),
                        with(REMINDER_LAST, now.minusDays(1)),
                        with(CREATION_TIME, now.minusDays(30)),
                        with(RANDOM_REMINDER_PERIOD, ONE_WEEK));

                service.scheduleAlarm(task);

                InOrder order = inOrder(jobs);
                order.verify(jobs).cancelReminder(1);
                order
                    .verify(jobs)
                    .add(
                        new ReminderEntry(
                            1L,
                            now.minusDays(1).getMillis() + 584206592,
                            ReminderService.TYPE_RANDOM));
              }
            });
  }

  @Test
  public void scheduleOverdueRandomReminder() {
    freezeClock()
        .thawAfter(
            new Snippet() {
              {
                DateTime now = newDateTime();
                when(random.nextFloat()).thenReturn(0.3865f);
                Task task =
                    newTask(
                        with(ID, 1L),
                        with(REMINDER_LAST, now.minusDays(14)),
                        with(CREATION_TIME, now.minusDays(30)),
                        with(RANDOM_REMINDER_PERIOD, ONE_WEEK));

                service.scheduleAlarm(task);

                InOrder order = inOrder(jobs);
                order.verify(jobs).cancelReminder(1);
                order
                    .verify(jobs)
                    .add(
                        new ReminderEntry(
                            1L, now.getMillis() + 10148400, ReminderService.TYPE_RANDOM));
              }
            });
  }

  @Test
  public void scheduleOverdueNoLastReminder() {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 9, 22, 15, 30)),
            with(REMINDER_LAST, (DateTime) null),
            with(REMINDERS, NOTIFY_AFTER_DEADLINE));

    service.scheduleAlarm(task);

    InOrder order = inOrder(jobs);
    order.verify(jobs).cancelReminder(1);
    order
        .verify(jobs)
        .add(
            new ReminderEntry(
                1L,
                new DateTime(2017, 9, 23, 15, 30, 1, 0).getMillis(),
                ReminderService.TYPE_OVERDUE));
  }

  @Test
  public void scheduleOverduePastLastReminder() {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 9, 22, 15, 30)),
            with(REMINDER_LAST, new DateTime(2017, 9, 24, 12, 0)),
            with(REMINDERS, NOTIFY_AFTER_DEADLINE));

    service.scheduleAlarm(task);

    InOrder order = inOrder(jobs);
    order.verify(jobs).cancelReminder(1);
    order
        .verify(jobs)
        .add(
            new ReminderEntry(
                1L,
                new DateTime(2017, 9, 24, 15, 30, 1, 0).getMillis(),
                ReminderService.TYPE_OVERDUE));
  }

  @Test
  public void scheduleOverdueBeforeLastReminder() {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 9, 22, 12, 30)),
            with(REMINDER_LAST, new DateTime(2017, 9, 24, 15, 0)),
            with(REMINDERS, NOTIFY_AFTER_DEADLINE));

    service.scheduleAlarm(task);

    InOrder order = inOrder(jobs);
    order.verify(jobs).cancelReminder(1);
    order
        .verify(jobs)
        .add(
            new ReminderEntry(
                1L,
                new DateTime(2017, 9, 25, 12, 30, 1, 0).getMillis(),
                ReminderService.TYPE_OVERDUE));
  }

  @Test
  public void scheduleOverdueWithNoDueTime() {
    preferences.setInt(R.string.p_rmd_time, (int) TimeUnit.HOURS.toMillis(15));
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_DATE, new DateTime(2017, 9, 22)),
            with(REMINDER_LAST, new DateTime(2017, 9, 23, 12, 17, 59, 999)),
            with(REMINDERS, NOTIFY_AFTER_DEADLINE));

    service.scheduleAlarm(task);

    InOrder order = inOrder(jobs);
    order.verify(jobs).cancelReminder(1);
    order
        .verify(jobs)
        .add(
            new ReminderEntry(
                1L,
                new DateTime(2017, 9, 23, 15, 0, 0, 0).getMillis(),
                ReminderService.TYPE_OVERDUE));
  }

  @Test
  public void scheduleSubsequentOverdueReminder() {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 9, 22, 15, 30)),
            with(REMINDER_LAST, new DateTime(2017, 9, 23, 15, 30, 59, 999)),
            with(REMINDERS, NOTIFY_AFTER_DEADLINE));

    service.scheduleAlarm(task);

    InOrder order = inOrder(jobs);
    order.verify(jobs).cancelReminder(1);
    order
        .verify(jobs)
        .add(
            new ReminderEntry(
                1L,
                new DateTime(2017, 9, 24, 15, 30, 1, 0).getMillis(),
                ReminderService.TYPE_OVERDUE));
  }

  @Test
  public void scheduleOverdueAfterLastReminder() {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 9, 22, 15, 30)),
            with(REMINDER_LAST, new DateTime(2017, 9, 23, 12, 17, 59, 999)),
            with(REMINDERS, NOTIFY_AFTER_DEADLINE));

    service.scheduleAlarm(task);

    InOrder order = inOrder(jobs);
    order.verify(jobs).cancelReminder(1);
    order
        .verify(jobs)
        .add(
            new ReminderEntry(
                1L,
                new DateTime(2017, 9, 23, 15, 30, 1, 0).getMillis(),
                ReminderService.TYPE_OVERDUE));
  }

  @Test
  public void snoozeOverridesAll() {
    DateTime now = newDateTime();
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, now),
            with(SNOOZE_TIME, now.plusMonths(12)),
            with(REMINDERS, NOTIFY_AT_DEADLINE | NOTIFY_AFTER_DEADLINE),
            with(RANDOM_REMINDER_PERIOD, ONE_HOUR));

    service.scheduleAlarm(task);

    InOrder order = inOrder(jobs);
    order.verify(jobs).cancelReminder(1);
    order
        .verify(jobs)
        .add(new ReminderEntry(1, now.plusMonths(12).getMillis(), ReminderService.TYPE_SNOOZE));
  }
}
