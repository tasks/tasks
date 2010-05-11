package com.todoroo.astrid.reminders.service;

import java.util.HashMap;

import android.database.sqlite.SQLiteQueryBuilder;

import com.todoroo.andlib.test.data.TodorooCursor;
import com.todoroo.andlib.test.utility.DateUtilities;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.reminders.AlarmReceiver;
import com.todoroo.astrid.reminders.Constants;
import com.todoroo.astrid.reminders.PluginTestCase;
import com.todoroo.astrid.reminders.service.ReminderService.Alarm;

public class ReminderServiceTests extends PluginTestCase {

    /**
     * Test task fetching mechanism for reminders
     */
    @SuppressWarnings("unused")
    public void testTaskReminderFetch() {
        ReminderService service = new ReminderService(getContext());

        SQLiteQueryBuilder baseQuery = service.createBaseQuery();
        TodorooCursor<Task> cursor = service.fetchTasksWithReminders(baseQuery);
        assertEquals(0, cursor.getCount());
        cursor.close();

        // add new tasks
        int now = DateUtilities.now();
        Task task = new Task();
        HashMap<String, String> metadata = new HashMap<String, String>();

        task.setValue(Task.TITLE, "normal");
        task.setValue(Task.DUE_DATE, now + 10);
        metadata.put(Constants.KEY_REMINDER, Integer.toString(Constants.REMINDER_AT_DEADLINE));
        long normalId = createTask(task, metadata);

        task.clear();
        task.setValue(Task.TITLE, "completed");
        task.setValue(Task.DUE_DATE, now + 10);
        task.setValue(Task.COMPLETION_DATE, now + 10);
        metadata.put(Constants.KEY_REMINDER, Integer.toString(Constants.REMINDER_ALARM_CLOCK));
        long completedId = createTask(task, metadata);

        task.clear();
        task.setValue(Task.TITLE, "hidden");
        task.setValue(Task.DUE_DATE, now + 10);
        task.setValue(Task.HIDDEN_UNTIL, now + 10);
        metadata.put(Constants.KEY_REMINDER, Integer.toString(Constants.REMINDER_INCREASING));
        long hiddenId = createTask(task, metadata);

        task.clear();
        task.setValue(Task.TITLE, "noalarm");
        task.setValue(Task.DUE_DATE, now + 10);
        metadata.put(Constants.KEY_REMINDER, Integer.toString(Constants.REMINDER_NONE));
        long noAlarmId = createTask(task, metadata);

        task.clear();
        task.setValue(Task.TITLE, "overdue");
        task.setValue(Task.DUE_DATE, now - 10);
        metadata.put(Constants.KEY_REMINDER, Integer.toString(Constants.REMINDER_AT_DEADLINE));
        long overdueId = createTask(task, metadata);

        cursor = service.fetchTasksWithReminders(baseQuery);
        assertEquals(2, cursor.getCount());
        cursor.moveToFirst();
        task.readFromCursor(cursor, ReminderService.PROPERTIES);
        assertEquals(task.getId(), normalId);
        assertEquals(task.getValue(ReminderService.REMINDER_MODE),
                Integer.toString(Constants.REMINDER_AT_DEADLINE));
        cursor.moveToNext();
        task.readFromCursor(cursor, ReminderService.PROPERTIES);
        assertEquals(task.getId(), hiddenId);
        assertEquals(task.getValue(ReminderService.REMINDER_MODE),
                Integer.toString(Constants.REMINDER_INCREASING));
        cursor.close();
    }

    /**
     * Test task fetching mechanism for overdue interval
     */
    @SuppressWarnings("unused")
    public void testTaskOverdueFetch() {
        ReminderService service = new ReminderService(getContext());

        SQLiteQueryBuilder baseQuery = service.createBaseQuery();
        TodorooCursor<Task> cursor = service.fetchTasksWithOverdueReminders(baseQuery);
        assertEquals(0, cursor.getCount());
        cursor.close();

        // add new tasks
        int now = DateUtilities.now();
        Task task = new Task();
        HashMap<String, String> metadata = new HashMap<String, String>();

        task.setValue(Task.TITLE, "normal");
        task.setValue(Task.DUE_DATE, now - 10);
        metadata.put(Constants.KEY_OVERDUE, Integer.toString(1500));
        long normalId = createTask(task, metadata);

        task.clear();
        task.setValue(Task.TITLE, "completed");
        task.setValue(Task.DUE_DATE, now - 10);
        task.setValue(Task.COMPLETION_DATE, now - 10);
        metadata.put(Constants.KEY_OVERDUE, Integer.toString(1500));
        long completedId = createTask(task, metadata);

        task.clear();
        task.setValue(Task.TITLE, "hidden");
        task.setValue(Task.DUE_DATE, now - 10);
        task.setValue(Task.HIDDEN_UNTIL, now + 10);
        metadata.put(Constants.KEY_OVERDUE, Integer.toString(1500));
        long hiddenId = createTask(task, metadata);

        task.clear();
        task.setValue(Task.TITLE, "noalarm");
        task.setValue(Task.DUE_DATE, now - 10);
        metadata.put(Constants.KEY_OVERDUE, Integer.toString(0));
        long noAlarmId = createTask(task, metadata);

        task.clear();
        task.setValue(Task.TITLE, "not due");
        task.setValue(Task.DUE_DATE, now + 10);
        metadata.put(Constants.KEY_OVERDUE, Integer.toString(0));
        long notDueId = createTask(task, metadata);

        cursor = service.fetchTasksWithOverdueReminders(baseQuery);
        assertEquals(2, cursor.getCount());
        cursor.moveToFirst();
        task.readFromCursor(cursor, ReminderService.PROPERTIES);
        assertEquals(task.getId(), normalId);
        assertEquals(task.getValue(ReminderService.OVERDUE_INTERVAL),
                Integer.valueOf(1500));
        cursor.moveToNext();
        task.readFromCursor(cursor, ReminderService.PROPERTIES);
        assertEquals(task.getId(), hiddenId);
        assertEquals(task.getValue(ReminderService.OVERDUE_INTERVAL),
                Integer.valueOf(1500));
        cursor.close();
    }

    /**
     * Test task fetching mechanism for reminders
     */
    public void testTaskFetchForReminders() {
        ReminderService service = new ReminderService(getContext());

        // try invalid
        assertNull("could fetch invalid", service.fetchTask(-99));

        // add new tasks
        Task task = new Task();
        HashMap<String, String> metadata = new HashMap<String, String>();
        task.setValue(Task.TITLE, "salami");
        task.setValue(Task.DUE_DATE, 100);
        metadata.put(Constants.KEY_OVERDUE, "123");
        metadata.put(Constants.KEY_LAST_NOTIFIED, "345");
        metadata.put(Constants.KEY_REMINDER, "567");
        long id = createTask(task, metadata);

        task = service.fetchTask(id);
        assertEquals(task.getValue(Task.TITLE), "salami");
        assertEquals(task.getValue(ReminderService.OVERDUE_INTERVAL), Integer.valueOf(123));
        assertEquals(task.getValue(ReminderService.LAST_NOTIFIED), Integer.valueOf(345));
        assertEquals(task.getValue(ReminderService.REMINDER_MODE), Integer.valueOf(567));

        task.setValue(ReminderService.OVERDUE_INTERVAL, 321);
        task.setValue(ReminderService.LAST_NOTIFIED, 543);
        task.setValue(ReminderService.REMINDER_MODE, 765);

        service.writeReminderOptions(task);
        task = service.fetchTask(id);

        assertEquals(task.getValue(ReminderService.OVERDUE_INTERVAL), Integer.valueOf(321));
        assertEquals(task.getValue(ReminderService.LAST_NOTIFIED), Integer.valueOf(543));
        assertEquals(task.getValue(ReminderService.REMINDER_MODE), Integer.valueOf(765));
    }

    /**
     * Test alarm generation
     */
    public void testAlarmGeneration() {
        ReminderService service = new ReminderService(getContext());
        int now = DateUtilities.now();

        // test simple task with deadline
        Task task = new Task();
        task.setValue(Task.TITLE, "blah");
        task.setValue(Task.DUE_DATE, now + 10000);
        task.setValue(ReminderService.REMINDER_MODE, Constants.REMINDER_AT_DEADLINE);
        Alarm alarm = service.createReminderAlarm(task);
        assertEquals(task.getValue(Task.DUE_DATE), Integer.valueOf(alarm.alarmTime));

        // test simple task no reminders
        task.setValue(ReminderService.REMINDER_MODE, Constants.REMINDER_NONE);
        alarm = service.createReminderAlarm(task);
        assertNull(alarm);

        // test simple task with deadline passed and no overdue interval
        task.setValue(Task.DUE_DATE, now - 10);
        task.setValue(ReminderService.REMINDER_MODE, Constants.REMINDER_AT_DEADLINE);
        alarm = service.createReminderAlarm(task);
        assertNull(alarm);

        // test alarm clock task
        task.setValue(Task.DUE_DATE, now + 10000);
        task.setValue(ReminderService.REMINDER_MODE, Constants.REMINDER_ALARM_CLOCK);
        alarm = service.createReminderAlarm(task);
        assertEquals(task.getValue(Task.DUE_DATE), Integer.valueOf(alarm.alarmTime));

        // test overdue alarm clock task
        task.setValue(Task.DUE_DATE, now - 10);
        alarm = service.createReminderAlarm(task);
        assertNull(alarm);

        // test increasing 30 days from now
        task.setValue(ReminderService.REMINDER_MODE, Constants.REMINDER_INCREASING);
        task.setValue(ReminderService.LAST_NOTIFIED, now);
        task.setValue(Task.DUE_DATE, now + 30 * 86400);
        alarm = service.createReminderAlarm(task);
        assertTrue(alarm.alarmTime > now + 5 * 86400 &&
                alarm.alarmTime < now + 25 * 86400);

        // test increasing 30 days from now, with last notified of 100 days ago
        task.setValue(ReminderService.REMINDER_MODE, Constants.REMINDER_INCREASING);
        task.setValue(Task.DUE_DATE, now + 30 * 86400);
        task.setValue(ReminderService.LAST_NOTIFIED, now - 100 * 86400);
        alarm =  service.createReminderAlarm(task);
        assertTrue(alarm.alarmTime > 0 &&
                alarm.alarmTime < now + 1 * 86400);

        // test increasing when we're within the day boundary and task is
        // not defined with a specific time
        task.setValue(Task.URGENCY, Task.URGENCY_THIS_WEEK);
        task.setValue(Task.DUE_DATE, now + 80000);
        task.setValue(ReminderService.LAST_NOTIFIED, now);
        alarm = service.createReminderAlarm(task);
        assertTrue(alarm.intent.getBooleanExtra(AlarmReceiver.TOKEN_IS_DEADLINE, false));
        assertEquals(task.getValue(Task.DUE_DATE), Integer.valueOf(alarm.alarmTime));

        // now same due date, but we've defined specific time
        task.setValue(Task.URGENCY, Task.URGENCY_SPECIFIC_DAY_TIME);
        alarm = service.createReminderAlarm(task);
        assertTrue(DateUtilities.now() < alarm.alarmTime &&
                alarm.alarmTime < task.getValue(Task.DUE_DATE));

        // move the due date right up against actual
        task.setValue(Task.DUE_DATE, now + 1000);
        alarm = service.createReminderAlarm(task);
        assertEquals(task.getValue(Task.DUE_DATE), Integer.valueOf(alarm.alarmTime));
        assertTrue(alarm.intent.getBooleanExtra(AlarmReceiver.TOKEN_IS_DEADLINE, false));

        // and after due date
        task.setValue(Task.DUE_DATE, now - 80000);
        alarm = service.createReminderAlarm(task);
        assertNull(alarm);
    }

    /**
     * Test alarm generation (overdue)
     */
    public void testOverdueAlarmGeneration() {
        ReminderService service = new ReminderService(getContext());
        int now = DateUtilities.now();

        // test simple overdue
        Task task = new Task();
        task.setValue(Task.TITLE, "blah");

        task.setValue(Task.DUE_DATE, now - 100);
        task.setValue(ReminderService.OVERDUE_INTERVAL, 1000);
        Alarm alarm = service.createOverdueAlarm(task);
        assertTrue(now + 500 < alarm.alarmTime && alarm.alarmTime < now + 1500);

        // same case, but almost exact due as normal
        task.setValue(Task.DUE_DATE, DateUtilities.now());
        alarm = service.createOverdueAlarm(task);
        assertTrue(now + 500 < alarm.alarmTime && alarm.alarmTime < now + 1500);
    }

}
