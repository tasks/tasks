package com.todoroo.astrid.repeats;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.tasks.makers.TaskMaker.AFTER_COMPLETE;
import static org.tasks.makers.TaskMaker.COMPLETION_TIME;
import static org.tasks.makers.TaskMaker.DUE_TIME;
import static org.tasks.makers.TaskMaker.ID;
import static org.tasks.makers.TaskMaker.RRULE;
import static org.tasks.makers.TaskMaker.newTask;

import android.annotation.SuppressLint;
import android.support.test.runner.AndroidJUnit4;
import com.google.ical.values.RRule;
import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import java.text.ParseException;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.tasks.LocalBroadcastManager;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.time.DateTime;

@SuppressLint("NewApi")
@RunWith(AndroidJUnit4.class)
public class RepeatTaskHelperTest extends InjectingTestCase {

  @Inject TaskDao taskDao;

  private LocalBroadcastManager localBroadcastManager;
  private AlarmService alarmService;
  private GCalHelper gCalHelper;
  private RepeatTaskHelper helper;
  private InOrder mocks;

  @Before
  public void before() {
    alarmService = mock(AlarmService.class);
    gCalHelper = mock(GCalHelper.class);
    localBroadcastManager = mock(LocalBroadcastManager.class);
    mocks = inOrder(alarmService, gCalHelper, localBroadcastManager);
    helper = new RepeatTaskHelper(gCalHelper, alarmService, taskDao, localBroadcastManager);
  }

  @After
  public void after() {
    verifyNoMoreInteractions(localBroadcastManager, gCalHelper, alarmService);
  }

  @Test
  public void noRepeat() {
    helper.handleRepeat(newTask(with(DUE_TIME, new DateTime(2017, 10, 4, 13, 30))));
  }

  @Test
  public void testMinutelyRepeat() throws ParseException {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 10, 4, 13, 30)),
            with(RRULE, new RRule("RRULE:FREQ=MINUTELY;INTERVAL=30")));

    repeatAndVerify(
        task, new DateTime(2017, 10, 4, 13, 30, 1), new DateTime(2017, 10, 4, 14, 0, 1));
  }

  @Test
  public void testMinutelyRepeatAfterCompletion() throws ParseException {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 10, 4, 13, 30)),
            with(COMPLETION_TIME, new DateTime(2017, 10, 4, 13, 17, 45, 340)),
            with(RRULE, new RRule("RRULE:FREQ=MINUTELY;INTERVAL=30")),
            with(AFTER_COMPLETE, true));

    repeatAndVerify(
        task, new DateTime(2017, 10, 4, 13, 30, 1), new DateTime(2017, 10, 4, 13, 47, 1));
  }

  @Test
  public void testMinutelyDecrementCount() throws ParseException {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 10, 4, 13, 30)),
            with(RRULE, new RRule("RRULE:FREQ=MINUTELY;COUNT=2;INTERVAL=30")));

    repeatAndVerify(
        task, new DateTime(2017, 10, 4, 13, 30, 1), new DateTime(2017, 10, 4, 14, 0, 1));

    assertEquals(1, new RRule(task.getRecurrenceWithoutFrom()).getCount());
  }

  @Test
  public void testMinutelyLastOccurrence() throws ParseException {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 10, 4, 13, 30)),
            with(RRULE, new RRule("RRULE:FREQ=MINUTELY;COUNT=1;INTERVAL=30")));

    helper.handleRepeat(task);
  }

  @Test
  public void testHourlyRepeat() throws ParseException {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 10, 4, 13, 30)),
            with(RRULE, new RRule("RRULE:FREQ=HOURLY;INTERVAL=6")));

    repeatAndVerify(
        task, new DateTime(2017, 10, 4, 13, 30, 1), new DateTime(2017, 10, 4, 19, 30, 1));
  }

  @Test
  public void testHourlyRepeatAfterCompletion() throws ParseException {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 10, 4, 13, 30)),
            with(COMPLETION_TIME, new DateTime(2017, 10, 4, 13, 17, 45, 340)),
            with(RRULE, new RRule("RRULE:FREQ=HOURLY;INTERVAL=6")),
            with(AFTER_COMPLETE, true));

    repeatAndVerify(
        task, new DateTime(2017, 10, 4, 13, 30, 1), new DateTime(2017, 10, 4, 19, 17, 1));
  }

  @Test
  public void testDailyRepeat() throws ParseException {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 10, 4, 13, 30)),
            with(RRULE, new RRule("RRULE:FREQ=DAILY;INTERVAL=6")));

    repeatAndVerify(
        task, new DateTime(2017, 10, 4, 13, 30, 1), new DateTime(2017, 10, 10, 13, 30, 1));
  }

  @Test
  public void testRepeatWeeklyNoDays() throws ParseException {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 10, 4, 13, 30)),
            with(RRULE, new RRule("RRULE:FREQ=WEEKLY;INTERVAL=2")));

    repeatAndVerify(
        task, new DateTime(2017, 10, 4, 13, 30, 1), new DateTime(2017, 10, 18, 13, 30, 1));
  }

  @Test
  public void testYearly() throws ParseException {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 10, 4, 13, 30)),
            with(RRULE, new RRule("RRULE:FREQ=YEARLY;INTERVAL=3")));

    repeatAndVerify(
        task, new DateTime(2017, 10, 4, 13, 30, 1), new DateTime(2020, 10, 4, 13, 30, 1));
  }

  @Test
  public void testMonthlyRepeat() throws ParseException {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 10, 4, 13, 30)),
            with(RRULE, new RRule("RRULE:FREQ=MONTHLY;INTERVAL=3")));

    repeatAndVerify(
        task, new DateTime(2017, 10, 4, 13, 30, 1), new DateTime(2018, 1, 4, 13, 30, 1));
  }

  @Test
  public void testMonthlyRepeatAtEndOfMonth() throws ParseException {
    Task task =
        newTask(
            with(ID, 1L),
            with(DUE_TIME, new DateTime(2017, 1, 31, 13, 30)),
            with(RRULE, new RRule("RRULE:FREQ=MONTHLY;INTERVAL=1")));

    repeatAndVerify(
        task, new DateTime(2017, 1, 31, 13, 30, 1), new DateTime(2017, 2, 28, 13, 30, 1));
  }

  private void repeatAndVerify(Task task, DateTime oldDueDate, DateTime newDueDate) {
    helper.handleRepeat(task);

    mocks.verify(gCalHelper).rescheduleRepeatingTask(task);
    mocks.verify(alarmService).rescheduleAlarms(1, oldDueDate.getMillis(), newDueDate.getMillis());
    mocks
        .verify(localBroadcastManager)
        .broadcastRepeat(1, oldDueDate.getMillis(), newDueDate.getMillis());
  }

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }
}
