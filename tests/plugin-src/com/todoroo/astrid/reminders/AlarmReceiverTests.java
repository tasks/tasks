package com.todoroo.astrid.reminders;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;

import com.todoroo.andlib.test.utility.DateUtilities;
import com.todoroo.astrid.model.Task;

public class AlarmReceiverTests extends PluginTestCase {

    /** simple test of task at deadline */
    public void testDeadlineReminder() {
        Task task = new Task();
        task.setValue(Task.TITLE, "poop");
        task.setValue(Task.DUE_DATE, DateUtilities.now());

        long id = createTask(task, null);
        Intent alarmIntent = new Intent();
        alarmIntent.putExtra(AlarmReceiver.TOKEN_TASK_ID, id);
        alarmIntent.putExtra(AlarmReceiver.TOKEN_IS_DEADLINE, true);
        alarmIntent.putExtra(AlarmReceiver.TOKEN_IS_ALARMCLOCK, false);

        AlarmReceiver rx = new AlarmReceiver();
        AssertiveNotificationManager nm = new AssertiveNotificationManager(getContext());
        AlarmReceiver.notificationManager = nm;
        rx.onReceive(getContext(), alarmIntent);
        nm.assertNotified();
    }

    /** task at deadline, except hidden. no notification should sound */
    public void testDeadlineReminderExceptHidden() {
        Task task = new Task();
        task.setValue(Task.TITLE, "poop");
        task.setValue(Task.DUE_DATE, DateUtilities.now());
        task.setValue(Task.HIDDEN_UNTIL, DateUtilities.now() + 100);

        long id = createTask(task, null);
        Intent alarmIntent = new Intent();
        alarmIntent.putExtra(AlarmReceiver.TOKEN_TASK_ID, id);
        alarmIntent.putExtra(AlarmReceiver.TOKEN_IS_DEADLINE, true);
        alarmIntent.putExtra(AlarmReceiver.TOKEN_IS_ALARMCLOCK, false);

        AlarmReceiver rx = new AlarmReceiver();
        AssertiveNotificationManager nm = new AssertiveNotificationManager(getContext());
        AlarmReceiver.notificationManager = nm;
        rx.onReceive(getContext(), alarmIntent);
        nm.assertNotNotified();
    }

    /** task upcoming */
    public void testUpcomingReminder() {
        Task task = new Task();
        task.setValue(Task.TITLE, "poop");
        task.setValue(Task.DUE_DATE, DateUtilities.now() + 100);

        long id = createTask(task, null);
        Intent alarmIntent = new Intent();
        alarmIntent.putExtra(AlarmReceiver.TOKEN_TASK_ID, id);
        alarmIntent.putExtra(AlarmReceiver.TOKEN_IS_DEADLINE, false);
        alarmIntent.putExtra(AlarmReceiver.TOKEN_IS_ALARMCLOCK, false);

        AlarmReceiver rx = new AlarmReceiver();
        AssertiveNotificationManager nm = new AssertiveNotificationManager(getContext());
        AlarmReceiver.notificationManager = nm;
        rx.onReceive(getContext(), alarmIntent);
        nm.assertNotified();
    }

    /** task overdue */
    public void testOverdueReminder() {
        Task task = new Task();
        task.setValue(Task.TITLE, "poop");
        task.setValue(Task.DUE_DATE, DateUtilities.now() - 100);

        long id = createTask(task, null);
        Intent alarmIntent = new Intent();
        alarmIntent.putExtra(AlarmReceiver.TOKEN_TASK_ID, id);
        alarmIntent.putExtra(AlarmReceiver.TOKEN_IS_DEADLINE, false);
        alarmIntent.putExtra(AlarmReceiver.TOKEN_IS_ALARMCLOCK, false);

        AlarmReceiver rx = new AlarmReceiver();
        AssertiveNotificationManager nm = new AssertiveNotificationManager(getContext());
        AlarmReceiver.notificationManager = nm;
        rx.onReceive(getContext(), alarmIntent);
        nm.assertNotified();
    }

    /** task alarm clock */
    public void testAlarmClock() {
        Task task = new Task();
        task.setValue(Task.TITLE, "poop");
        task.setValue(Task.DUE_DATE, DateUtilities.now());

        long id = createTask(task, null);
        Intent alarmIntent = new Intent();
        alarmIntent.putExtra(AlarmReceiver.TOKEN_TASK_ID, id);
        alarmIntent.putExtra(AlarmReceiver.TOKEN_IS_DEADLINE, true);
        alarmIntent.putExtra(AlarmReceiver.TOKEN_IS_ALARMCLOCK, true);

        AlarmReceiver rx = new AlarmReceiver();
        AssertiveNotificationManager nm = new AssertiveNotificationManager(getContext());
        AlarmReceiver.notificationManager = nm;
        rx.onReceive(getContext(), alarmIntent);
        nm.assertNotified();
        assertTrue((nm.getNotification().flags & Notification.FLAG_INSISTENT) > 0);
    }

    /** test the intent that the alarm receiver creates */
    public void testOpenIntent() throws Exception {
        Task task = new Task();
        task.setValue(Task.TITLE, "poop");
        task.setValue(Task.DUE_DATE, DateUtilities.now());

        long id = createTask(task, null);
        Intent alarmIntent = new Intent();
        alarmIntent.putExtra(AlarmReceiver.TOKEN_TASK_ID, id);
        alarmIntent.putExtra(AlarmReceiver.TOKEN_IS_DEADLINE, true);
        alarmIntent.putExtra(AlarmReceiver.TOKEN_IS_ALARMCLOCK, false);

        AlarmReceiver rx = new AlarmReceiver();
        AssertiveNotificationManager nm = new AssertiveNotificationManager(getContext());
        AlarmReceiver.notificationManager = nm;
        rx.onReceive(getContext(), alarmIntent);
        nm.assertNotified();

        PendingIntent intent = nm.getNotification().contentIntent;
        intent.send();
    }
}
