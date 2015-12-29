package com.todoroo.astrid.reminders;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;

import com.todoroo.astrid.data.Task;

import org.tasks.preferences.Preferences;
import org.tasks.scheduling.AlarmManager;
import org.tasks.time.DateTime;

import java.util.concurrent.TimeUnit;

import static com.todoroo.astrid.reminders.ReminderService.NO_ALARM;
import static org.mockito.Mockito.mock;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.Freeze.thaw;
import static org.tasks.date.DateTimeUtils.newDate;

public class NotifyAtDeadlineTest extends AndroidTestCase {

    @SuppressLint("NewApi")
    private static final int MILLIS_PER_HOUR = (int) TimeUnit.HOURS.toMillis(1);

    private final Task dueAtNoon = new Task() {{
        setDueDate(Task.URGENCY_SPECIFIC_DAY, newDate(2014, 1, 27).getMillis());
        setReminderFlags(Task.NOTIFY_AT_DEADLINE);
    }};

    private ReminderService reminderService;
    private Preferences preferences;

    @Override
    public void setUp() {
        preferences = new Preferences(getContext(), null, null);
        reminderService = new ReminderService(getContext(), preferences, mock(AlarmManager.class));
        freezeAt(new DateTime(2014, 1, 24, 17, 23, 37));
    }

    @Override
    public void tearDown() {
        thaw();
    }

    public void testNoReminderWhenNoDueDate() {
        Task task = new Task() {{
            setReminderFlags(Task.NOTIFY_AT_DEADLINE);
        }};
        assertEquals(NO_ALARM, reminderService.calculateNextDueDateReminder(task));
    }

    public void testNoReminderWhenNotifyAtDeadlineFlagNotSet() {
        Task task = new Task() {{
            setDueDate(URGENCY_SPECIFIC_DAY_TIME, new DateTime(2014, 1, 24, 19, 23).getMillis());
        }};
        assertEquals(NO_ALARM, reminderService.calculateNextDueDateReminder(task));
    }

    public void testScheduleReminderAtDueTime() {
        final DateTime dueDate = new DateTime(2014, 1, 24, 19, 23);
        Task task = new Task() {{
            setDueDate(URGENCY_SPECIFIC_DAY_TIME, dueDate.getMillis());
            setReminderFlags(Task.NOTIFY_AT_DEADLINE);
        }};
        assertEquals(dueDate.plusSeconds(1).getMillis(), reminderService.calculateNextDueDateReminder(task));
    }

    public void testScheduleReminderAtDefaultDueTime() {
        final DateTime dueDate = new DateTime(2015, 12, 29, 12, 0);
        Task task = new Task() {{
            setDueDate(URGENCY_SPECIFIC_DAY, dueDate.getMillis());
            setReminderFlags(Task.NOTIFY_AT_DEADLINE);
        }};
        assertEquals(dueDate.withHourOfDay(18).getMillis(),
                reminderService.calculateNextDueDateReminder(task));
    }

    public void testNoReminderIfAlreadyRemindedPastDueDate() {
        final DateTime dueDate = new DateTime(2015, 12, 29, 19, 23);
        Task task = new Task() {{
            setDueDate(URGENCY_SPECIFIC_DAY_TIME, dueDate.getMillis());
            setReminderLast(dueDate.plusSeconds(1).getMillis());
            setReminderFlags(Task.NOTIFY_AT_DEADLINE);
        }};
        assertEquals(NO_ALARM, reminderService.calculateNextDueDateReminder(task));
    }
}
