/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.content.Context;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.ReminderService.AlarmScheduler;

import org.tasks.injection.InjectingTestCase;

import javax.inject.Inject;

import static org.tasks.Freeze.freezeClock;
import static org.tasks.Freeze.thaw;
import static org.tasks.date.DateTimeUtils.newDateTime;

public class ReminderServiceTest extends InjectingTestCase {

    @Inject TaskDao taskDao;
    @Inject ReminderService reminderService;

    @Override
    public void setUp() {
        super.setUp();
        freezeClock();
    }

    @Override
    public void tearDown() {
        thaw();
    }

    public void testNoReminders() {
        reminderService.setScheduler(new NoAlarmExpected());

        Task task = new Task();
        task.setTitle("water");
        task.setReminderFlags(0);
        task.setReminderPeriod(0L);
        taskDao.save(task);
        reminderService.scheduleAlarm(taskDao, task);
    }

    public void testDueDates() {
        reminderService.setScheduler(new AlarmExpected() {
            @Override
            public void createAlarm(Context context, Task task, long time, int type) {
                if (time == ReminderService.NO_ALARM)
                    return;
                super.createAlarm(getContext(), task, time, type);
                assertEquals((long) task.getDueDate(), time);
                assertEquals(type, ReminderService.TYPE_DUE);
            }
        });

        // test due date in the past
        final Task task = new Task();
        task.setTitle("water");
        task.setDueDate(DateUtilities.now() - DateUtilities.ONE_DAY);
        task.setReminderFlags(Task.NOTIFY_AT_DEADLINE);
        taskDao.save(task);

        // test due date in the future
        task.setDueDate(DateUtilities.now() + DateUtilities.ONE_DAY);
        taskDao.save(task);
        assertTrue(((AlarmExpected) reminderService.getScheduler()).alarmCreated);
    }

    public void disabled_testRandom() {
        // test random
        final Task task = new Task();
        task.setTitle("water");
        task.setReminderPeriod(DateUtilities.ONE_WEEK);
        reminderService.setScheduler(new AlarmExpected() {
            @Override
            public void createAlarm(Context context, Task task, long time, int type) {
                if (time == ReminderService.NO_ALARM)
                    return;
                super.createAlarm(getContext(), task, time, type);
                assertTrue(time > DateUtilities.now());
                assertTrue(time < DateUtilities.now() + 1.2 * DateUtilities.ONE_WEEK);
                assertEquals(type, ReminderService.TYPE_RANDOM);
            }
        });
        taskDao.save(task);
        assertTrue(((AlarmExpected) reminderService.getScheduler()).alarmCreated);
    }

    public void testOverdue() {
        // test due date in the future
        reminderService.setScheduler(new AlarmExpected() {
            @Override
            public void createAlarm(Context context, Task task, long time, int type) {
                if (time == ReminderService.NO_ALARM)
                    return;
                super.createAlarm(getContext(), task, time, type);
                assertTrue(time > task.getDueDate());
                assertTrue(time < task.getDueDate() + DateUtilities.ONE_DAY);
                assertEquals(type, ReminderService.TYPE_OVERDUE);
            }
        });
        final Task task = new Task();
        task.setTitle("water");
        task.setDueDate(DateUtilities.now() + DateUtilities.ONE_DAY);
        task.setReminderFlags(Task.NOTIFY_AFTER_DEADLINE);
        taskDao.save(task);

        // test due date in the past
        task.setDueDate(DateUtilities.now() - DateUtilities.ONE_DAY);
        reminderService.setScheduler(new AlarmExpected() {
            @Override
            public void createAlarm(Context context, Task task, long time, int type) {
                if (time == ReminderService.NO_ALARM)
                    return;
                super.createAlarm(getContext(), task, time, type);
                assertTrue(time > DateUtilities.now() - 1000L);
                assertTrue(time < DateUtilities.now() + 2 * DateUtilities.ONE_DAY);
                assertEquals(type, ReminderService.TYPE_OVERDUE);
            }
        });
        taskDao.save(task);
        assertTrue(((AlarmExpected) reminderService.getScheduler()).alarmCreated);

        // test due date in the past, but recently notified
        task.setReminderLast(DateUtilities.now());
        reminderService.setScheduler(new AlarmExpected() {
            @Override
            public void createAlarm(Context context, Task task, long time, int type) {
                if (time == ReminderService.NO_ALARM)
                    return;
                super.createAlarm(getContext(), task, time, type);
                assertTrue(time > DateUtilities.now() + DateUtilities.ONE_HOUR);
                assertTrue(time < DateUtilities.now() + DateUtilities.ONE_DAY);
                assertEquals(type, ReminderService.TYPE_OVERDUE);
            }
        });
        taskDao.save(task);
        assertTrue(((AlarmExpected) reminderService.getScheduler()).alarmCreated);
    }

    public void testMultipleReminders() {
        // test due date in the future, enable random
        final Task task = new Task();
        task.setTitle("water");
        task.setDueDate(DateUtilities.now() + DateUtilities.ONE_WEEK);
        task.setReminderFlags(Task.NOTIFY_AT_DEADLINE);
        task.setReminderPeriod(DateUtilities.ONE_HOUR);
        reminderService.setScheduler(new AlarmExpected() {
            @Override
            public void createAlarm(Context context, Task task, long time, int type) {
                if (time == ReminderService.NO_ALARM)
                    return;
                super.createAlarm(getContext(), task, time, type);
                assertTrue(time > DateUtilities.now());
                assertTrue(time < DateUtilities.now() + DateUtilities.ONE_DAY);
                assertEquals(type, ReminderService.TYPE_RANDOM);
            }
        });
        taskDao.save(task);
        assertTrue(((AlarmExpected) reminderService.getScheduler()).alarmCreated);

        // now set the due date in the past
        task.setDueDate(DateUtilities.now() - DateUtilities.ONE_WEEK);
        ((AlarmExpected) reminderService.getScheduler()).alarmCreated = false;
        reminderService.scheduleAlarm(taskDao, task);
        assertTrue(((AlarmExpected) reminderService.getScheduler()).alarmCreated);

        // now set the due date before the random
        task.setDueDate(DateUtilities.now() + DateUtilities.ONE_HOUR);
        reminderService.setScheduler(new AlarmExpected() {
            @Override
            public void createAlarm(Context context, Task task, long time, int type) {
                if (time == ReminderService.NO_ALARM)
                    return;
                super.createAlarm(getContext(), task, time, type);
                assertEquals((long) task.getDueDate(), time);
                assertEquals(type, ReminderService.TYPE_DUE);
            }
        });
        taskDao.save(task);
        assertTrue(((AlarmExpected) reminderService.getScheduler()).alarmCreated);
    }

    public void testSnoozeReminders() {
        thaw(); // TODO: get rid of this

        // test due date and snooze in the future
        final Task task = new Task();
        task.setTitle("spacemen");
        task.setDueDate(DateUtilities.now() + 5000L);
        task.setReminderFlags(Task.NOTIFY_AT_DEADLINE);
        task.setReminderSnooze(DateUtilities.now() + DateUtilities.ONE_WEEK);
        reminderService.setScheduler(new AlarmExpected() {
            @Override
            public void createAlarm(Context context, Task task, long time, int type) {
                if (time == ReminderService.NO_ALARM)
                    return;
                super.createAlarm(getContext(), task, time, type);
                assertTrue(time > DateUtilities.now() + DateUtilities.ONE_WEEK - 1000L);
                assertTrue(time < DateUtilities.now() + DateUtilities.ONE_WEEK + 1000L);
                assertEquals(type, ReminderService.TYPE_SNOOZE);
            }
        });
        taskDao.save(task);
        assertTrue(((AlarmExpected) reminderService.getScheduler()).alarmCreated);

        // snooze in the past
        task.setReminderSnooze(DateUtilities.now() - DateUtilities.ONE_WEEK);
        reminderService.setScheduler(new AlarmExpected() {
            @Override
            public void createAlarm(Context context, Task task, long time, int type) {
                if (time == ReminderService.NO_ALARM)
                    return;
                super.createAlarm(getContext(), task, time, type);
                assertTrue(time > DateUtilities.now() - 1000L);
                assertTrue(time < DateUtilities.now() + 5000L);
                assertEquals(type, ReminderService.TYPE_DUE);
            }
        });
        taskDao.save(task);
        assertTrue(((AlarmExpected) reminderService.getScheduler()).alarmCreated);
    }

    // --- helper classes

    public class NoAlarmExpected implements AlarmScheduler {
        public void createAlarm(Context context, Task task, long time, int type) {
            if(time == 0 || time == Long.MAX_VALUE)
                return;
            fail("created alarm, no alarm expected (" + type + ": " + newDateTime(time));
        }
    }

    public class AlarmExpected implements AlarmScheduler {
        public boolean alarmCreated = false;
        public void createAlarm(Context context, Task task, long time, int type) {
            alarmCreated = true;
        }
    }
}
