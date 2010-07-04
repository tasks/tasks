package com.todoroo.astrid.reminders;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.reminders.ReminderService.AlarmScheduler;
import com.todoroo.astrid.test.DatabaseTestCase;
import com.todoroo.astrid.utility.Preferences;

public class ReminderServiceTests extends DatabaseTestCase {

    ReminderService service;

    @Autowired
    TaskDao taskDao;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        service = new ReminderService();
        Preferences.setPreferenceDefaults();
    }

    /** tests with no alarms */
    public void testNoReminders() {
        service.setScheduler(new NoAlarmExpected());

        Task task = new Task();
        task.setValue(Task.TITLE, "water");
        task.setValue(Task.REMINDER_FLAGS, 0);
        taskDao.save(task, false);
        service.scheduleAlarm(task);
    }

    /** tests with due date */
    public void testDueDates() {
        // test due date in the past
        service.setScheduler(new NoAlarmExpected());
        final Task task = new Task();
        task.setValue(Task.TITLE, "water");
        task.setValue(Task.DUE_DATE, DateUtilities.now() - DateUtilities.ONE_DAY);
        task.setValue(Task.REMINDER_FLAGS, Task.NOTIFY_AT_DEADLINE);
        taskDao.save(task, false);
        service.scheduleAlarm(task);

        // test due date in the future
        task.setValue(Task.DUE_DATE, DateUtilities.now() + DateUtilities.ONE_DAY);
        taskDao.save(task, false);
        service.setScheduler(new AlarmExpected() {
            @Override
            public void createAlarm(Task task, long time, int type) {
                super.createAlarm(task, time, type);
                assertEquals((long)task.getValue(Task.DUE_DATE), time);
                assertEquals(type, ReminderService.TYPE_DUE);
            }
        });
        service.scheduleAlarm(task);
        assertTrue(((AlarmExpected)service.getScheduler()).alarmCreated);
    }

    /** tests with random */
    public void testRandom() {
        // test random
        final Task task = new Task();
        task.setValue(Task.TITLE, "water");
        task.setValue(Task.REMINDER_PERIOD, DateUtilities.ONE_WEEK);
        taskDao.save(task, false);
        service.setScheduler(new AlarmExpected() {
            @Override
            public void createAlarm(Task task, long time, int type) {
                super.createAlarm(task, time, type);
                assertTrue(time > DateUtilities.now());
                assertTrue(time < DateUtilities.now() + 1.2 * DateUtilities.ONE_WEEK);
                assertEquals(type, ReminderService.TYPE_RANDOM);
            }
        });
        service.scheduleAlarm(task);
        assertTrue(((AlarmExpected)service.getScheduler()).alarmCreated);

        // test random with last notify way in the past
        task.setValue(Task.REMINDER_LAST, DateUtilities.now() - 2 * DateUtilities.ONE_WEEK);
        ((AlarmExpected)service.getScheduler()).alarmCreated = false;
        service.scheduleAlarm(task);
        assertTrue(((AlarmExpected)service.getScheduler()).alarmCreated);
    }

    /** tests with overdue */
    public void testOverdue() {
        // test due date in the future
        service.setScheduler(new NoAlarmExpected());
        final Task task = new Task();
        task.setValue(Task.TITLE, "water");
        task.setValue(Task.DUE_DATE, DateUtilities.now() + DateUtilities.ONE_DAY);
        task.setValue(Task.REMINDER_FLAGS, Task.NOTIFY_AFTER_DEADLINE);
        taskDao.save(task, false);
        service.scheduleAlarm(task);

        // test due date in the past
        task.setValue(Task.DUE_DATE, DateUtilities.now() - DateUtilities.ONE_DAY);
        taskDao.save(task, false);
        service.setScheduler(new AlarmExpected() {
            @Override
            public void createAlarm(Task task, long time, int type) {
                super.createAlarm(task, time, type);
                assertTrue(time > DateUtilities.now());
                assertTrue(time < DateUtilities.now() + DateUtilities.ONE_DAY);
                assertEquals(type, ReminderService.TYPE_OVERDUE);
            }
        });
        service.scheduleAlarm(task);
        assertTrue(((AlarmExpected)service.getScheduler()).alarmCreated);
    }

    /** tests with multiple */
    public void testMultipleReminders() {
        // test due date in the future, enable random
        final Task task = new Task();
        task.setValue(Task.TITLE, "water");
        task.setValue(Task.DUE_DATE, DateUtilities.now() + DateUtilities.ONE_WEEK);
        task.setValue(Task.REMINDER_FLAGS, Task.NOTIFY_AT_DEADLINE);
        task.setValue(Task.REMINDER_PERIOD, DateUtilities.ONE_DAY);
        taskDao.save(task, false);
        service.setScheduler(new AlarmExpected() {
            @Override
            public void createAlarm(Task task, long time, int type) {
                super.createAlarm(task, time, type);
                assertTrue(time > DateUtilities.now());
                assertTrue(time < DateUtilities.now() + DateUtilities.ONE_DAY);
                assertEquals(type, ReminderService.TYPE_RANDOM);
            }
        });
        service.scheduleAlarm(task);
        assertTrue(((AlarmExpected)service.getScheduler()).alarmCreated);

        // now set the due date in the past
        task.setValue(Task.DUE_DATE, DateUtilities.now() - DateUtilities.ONE_WEEK);
        ((AlarmExpected)service.getScheduler()).alarmCreated = false;
        service.scheduleAlarm(task);
        assertTrue(((AlarmExpected)service.getScheduler()).alarmCreated);

        // now set the due date before the random
        task.setValue(Task.DUE_DATE, DateUtilities.now() + DateUtilities.ONE_HOUR);
        taskDao.save(task, false);
        service.setScheduler(new AlarmExpected() {
            @Override
            public void createAlarm(Task task, long time, int type) {
                super.createAlarm(task, time, type);
                assertEquals((long)task.getValue(Task.DUE_DATE), time);
                assertEquals(type, ReminderService.TYPE_DUE);
            }
        });
        service.scheduleAlarm(task);
        assertTrue(((AlarmExpected)service.getScheduler()).alarmCreated);
    }

    // --- helper classes

    public class NoAlarmExpected implements AlarmScheduler {
        public void createAlarm(Task task, long time, int type) {
            fail("created alarm, no alarm expected");
        }
    }

    public class AlarmExpected implements AlarmScheduler {
        public boolean alarmCreated = false;
        public void createAlarm(Task task, long time, int type) {
            alarmCreated = true;
        }
    }

}
