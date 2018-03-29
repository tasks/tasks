package com.todoroo.astrid.alarms;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.tasks.makers.TaskMaker.REMINDER_LAST;
import static org.tasks.makers.TaskMaker.newTask;

import android.support.test.runner.AndroidJUnit4;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.tasks.data.Alarm;
import org.tasks.data.AlarmDao;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.jobs.AlarmEntry;
import org.tasks.jobs.NotificationQueue;
import org.tasks.time.DateTime;

@RunWith(AndroidJUnit4.class)
public class AlarmJobServiceTest extends InjectingTestCase {

  @Inject AlarmDao alarmDao;
  @Inject TaskDao taskDao;

  private AlarmService alarmService;
  private NotificationQueue jobs;

  @Before
  public void before() {
    jobs = mock(NotificationQueue.class);
    alarmService = new AlarmService(alarmDao, jobs);
  }

  @After
  public void after() {
    verifyNoMoreInteractions(jobs);
  }

  @Test
  public void scheduleAlarm() {
    Task task = newTask();

    taskDao.createNew(task);
    DateTime alarmTime = new DateTime(2017, 9, 24, 19, 57);

    Alarm alarm = new Alarm(task.getId(), alarmTime.getMillis());
    alarm.setId(alarmDao.insert(alarm));

    alarmService.scheduleAllAlarms();

    InOrder order = inOrder(jobs);
    order.verify(jobs).add(new AlarmEntry(alarm));
  }

  @Test
  public void ignoreStaleAlarm() {
    DateTime alarmTime = new DateTime(2017, 9, 24, 19, 57);

    Task task = newTask(with(REMINDER_LAST, alarmTime.endOfMinute()));

    taskDao.createNew(task);

    alarmDao.insert(new Alarm(task.getId(), alarmTime.getMillis()));

    alarmService.scheduleAllAlarms();

    verifyNoMoreInteractions(jobs);
  }

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }
}
