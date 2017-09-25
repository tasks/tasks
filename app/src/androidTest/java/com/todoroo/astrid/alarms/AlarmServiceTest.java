package com.todoroo.astrid.alarms;

import android.support.test.runner.AndroidJUnit4;

import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.tasks.injection.TestComponent;
import org.tasks.jobs.Alarm;
import org.tasks.jobs.JobQueue;
import org.tasks.time.DateTime;

import javax.inject.Inject;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.tasks.makers.TaskMaker.REMINDER_LAST;
import static org.tasks.makers.TaskMaker.newTask;

@RunWith(AndroidJUnit4.class)
public class AlarmServiceTest extends DatabaseTestCase {

    @Inject MetadataDao metadataDao;
    @Inject TaskDao taskDao;

    private AlarmService alarmService;
    private JobQueue jobs;

    @Before
    public void before() {
        jobs = mock(JobQueue.class);
        alarmService = new AlarmService(metadataDao, jobs);
    }

    @After
    public void after() {
        verifyNoMoreInteractions(jobs);
    }

    @Test
    public void scheduleAlarm() {
        Task task = newTask();

        taskDao.persist(task);
        DateTime alarmTime = new DateTime(2017, 9, 24, 19, 57);

        Metadata alarm = new Metadata();
        alarm.setTask(task.getId());
        alarm.setKey(AlarmFields.METADATA_KEY);
        alarm.setValue(AlarmFields.TYPE, AlarmFields.TYPE_SINGLE);
        alarm.setValue(AlarmFields.TIME, alarmTime.getMillis());
        metadataDao.persist(alarm);

        alarmService.scheduleAllAlarms();

        InOrder order = inOrder(jobs);
        order.verify(jobs).add(new Alarm(alarm));
    }

    @Test
    public void ignoreStaleAlarm() {
        DateTime alarmTime = new DateTime(2017, 9, 24, 19, 57);

        Task task = newTask(with(REMINDER_LAST, alarmTime.endOfMinute()));

        taskDao.persist(task);

        Metadata alarm = new Metadata();
        alarm.setTask(task.getId());
        alarm.setKey(AlarmFields.METADATA_KEY);
        alarm.setValue(AlarmFields.TYPE, AlarmFields.TYPE_SINGLE);
        alarm.setValue(AlarmFields.TIME, alarmTime.getMillis());
        metadataDao.persist(alarm);

        alarmService.scheduleAllAlarms();

        verifyNoMoreInteractions(jobs);
    }

    @Override
    protected void inject(TestComponent component) {
        component.inject(this);
    }
}
