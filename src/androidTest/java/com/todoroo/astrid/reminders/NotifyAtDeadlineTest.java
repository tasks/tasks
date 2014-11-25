package com.todoroo.astrid.reminders;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;

import com.todoroo.astrid.data.Task;

import org.joda.time.DateTime;
import org.tasks.Freeze;
import org.tasks.R;
import org.tasks.preferences.Preferences;

import java.util.concurrent.TimeUnit;

import static com.todoroo.astrid.reminders.ReminderService.NO_ALARM;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.Freeze.thaw;
import static org.tasks.date.DateTimeUtils.newDate;

public class NotifyAtDeadlineTest extends AndroidTestCase {

    @SuppressLint("NewApi")
    private static final int MILLIS_PER_HOUR = (int) TimeUnit.HOURS.toMillis(1);

    private final Task dueAtNoon = new Task() {{
        setDueDate(Task.URGENCY_SPECIFIC_DAY, newDate(2014, 1, 27).getTime());
        setReminderFlags(Task.NOTIFY_AT_DEADLINE);
    }};

    private ReminderService reminderService;
    private Preferences preferences;

    @Override
    public void setUp() {
        preferences = new Preferences(getContext());
        reminderService = new ReminderService(getContext(), preferences);
        freezeAt(new DateTime(2014, 1, 24, 17, 23, 37));
    }

    @Override
    public void tearDown() {
        thaw();
    }

    public void testScheduleReminderAtDueTime() {
        final DateTime dueDate = new DateTime(2014, 1, 24, 19, 23, 57);
        Task task = new Task() {{
            setDueDate(dueDate.getMillis());
            setReminderFlags(Task.NOTIFY_AT_DEADLINE);
        }};
        assertEquals(dueDate.getMillis(), reminderService.calculateNextDueDateReminder(task));
    }

    public void testNoReminderWhenNoDueDate() {
        Task task = new Task() {{
            setReminderFlags(Task.NOTIFY_AT_DEADLINE);
        }};
        assertEquals(NO_ALARM, reminderService.calculateNextDueDateReminder(task));
    }

    public void testNoReminderWhenNotifyAtDeadlineFlagNotSet() {
        Task task = new Task() {{
            setDueDate(new DateTime(2014, 1, 24, 19, 23, 57).getMillis());
        }};
        assertEquals(NO_ALARM, reminderService.calculateNextDueDateReminder(task));
    }

    public void testDontNotifyMoreThanOncePerDay() {
        Task task = new Task() {{
            setDueDate(newDate(2014, 1, 23).getTime());
            setReminderFlags(Task.NOTIFY_AT_DEADLINE);
            setReminderLast(new DateTime(2014, 1, 23, 17, 23, 37).getMillis());
        }};
        assertEquals(NO_ALARM, reminderService.calculateNextDueDateReminder(task));
    }

    public void disabled_testNotifyIfLastNotificationWasMoreThanOneDayAgo() {
        final DateTime dueDate = new DateTime(2014, 1, 24, 0, 0, 0, 0);
        Task task = new Task() {{
            setDueDate(Task.URGENCY_SPECIFIC_DAY, dueDate.getMillis());
            setReminderFlags(Task.NOTIFY_AT_DEADLINE);
            setReminderLast(new DateTime(2014, 1, 23, 17, 23, 36).getMillis());
        }};
        assertEquals(
                dueDate.withHourOfDay(18).getMillis(),
                reminderService.calculateNextDueDateReminder(task));
    }

    public void testDuringQuietHoursSetNotificationAtEnd() {
        setQuietHours(0, 10);
        Freeze.freezeAt(new DateTime(2014, 1, 27, 9, 13, 37, 501));
        preferences.setInt(R.string.p_rmd_time, 8 * MILLIS_PER_HOUR);
        Task task = new Task() {{
            setDueDate(Task.URGENCY_SPECIFIC_DAY, newDate(2014, 1, 27).getTime());
            setReminderFlags(Task.NOTIFY_AT_DEADLINE);
        }};
        assertEquals(
                new DateTime(2014, 1, 27, 10, 0, 0, 0).getMillis(),
                reminderService.calculateNextDueDateReminder(task));
    }

    public void testAfterQuietHoursSetNotificationOnePeriodCloserToDueDate() {
        setQuietHours(0, 10);
        Freeze.freezeAt(new DateTime(2014, 1, 27, 11, 13, 37, 501));
        preferences.setInt(R.string.p_rmd_time, 8 * MILLIS_PER_HOUR);
        Task task = new Task() {{
            setDueDate(Task.URGENCY_SPECIFIC_DAY, newDate(2014, 1, 27).getTime());
            setReminderFlags(Task.NOTIFY_AT_DEADLINE);
        }};
        assertEquals(
                new DateTime(2014, 1, 27, 11, 25, 13, 125).getMillis(),
                reminderService.calculateNextDueDateReminder(task));
    }

    public void testBeforeQuietStartDueDateMoreThanOnePeriodAfterEnd() {
        setQuietHours(2, 11);
        Freeze.freezeAt(new DateTime(2014, 1, 27, 1, 53, 37, 509));
        preferences.setInt(R.string.p_rmd_time, MILLIS_PER_HOUR);
        Task task = new Task() {{
            setDueDate(Task.URGENCY_SPECIFIC_DAY, newDate(2014, 1, 27).getTime());
            setReminderFlags(Task.NOTIFY_AT_DEADLINE);
        }};
        assertEquals(
                new DateTime(2014, 1, 27, 11, 0, 0, 0).getMillis(),
                reminderService.calculateNextDueDateReminder(task));
    }

    public void testBeforeQuietStartDueDateLessThanOnePeriodAfterEnd() {
        setQuietHours(3, 11);
        Freeze.freezeAt(new DateTime(2014, 1, 27, 1, 53, 37, 509));
        preferences.setInt(R.string.p_rmd_time, MILLIS_PER_HOUR);
        assertEquals(
                new DateTime(2014, 1, 27, 2, 10, 13, 131).getMillis(),
                reminderService.calculateNextDueDateReminder(dueAtNoon));
    }

    public void testNoAlarmAfterQuietHoursStartWithWrap() {
        setQuietHours(10, 1);
        Freeze.freezeAt(new DateTime(2014, 1, 27, 10, 0, 0, 0));
        preferences.setInt(R.string.p_rmd_time, 8 * MILLIS_PER_HOUR);
        assertEquals(
                NO_ALARM,
                reminderService.calculateNextDueDateReminder(dueAtNoon));
    }

    public void testSetToQuietAlarmEndWithWrap() {
        setQuietHours(22, 11);
        Freeze.freezeAt(new DateTime(2014, 1, 27, 10, 59, 59, 999));
        preferences.setInt(R.string.p_rmd_time, 8 * MILLIS_PER_HOUR);
        assertEquals(
                new DateTime(2014, 1, 27, 11, 0, 0, 0).getMillis(),
                reminderService.calculateNextDueDateReminder(dueAtNoon));
    }

    public void testSetReminderOnePeriodFromNowBeforeQuietHourStartWithWrap() {
        setQuietHours(22, 11);
        Freeze.freezeAt(new DateTime(2014, 1, 27, 11, 0, 0, 0));
        preferences.setInt(R.string.p_rmd_time, 8 * MILLIS_PER_HOUR);
        assertEquals(
                // wtf? this is after due date
                new DateTime(2014, 1, 27, 13, 45, 0, 0).getMillis(),
                reminderService.calculateNextDueDateReminder(dueAtNoon));
    }

    public void testSetReminderOnePeriodFromNowNoQuietHours() {
        Freeze.freezeAt(new DateTime(2014, 1, 27, 11, 0, 0, 0));
        preferences.setBoolean(R.string.p_rmd_enable_quiet, false);
        preferences.setInt(R.string.p_rmd_time, 8 * MILLIS_PER_HOUR);
        assertEquals(
                new DateTime(2014, 1, 27, 11, 15, 0, 0).getMillis(),
                reminderService.calculateNextDueDateReminder(dueAtNoon));
    }

    private void setQuietHours(int start, int end) {
        preferences.setBoolean(R.string.p_rmd_enable_quiet, true);
        preferences.setInt(R.string.p_rmd_quietStart, start * MILLIS_PER_HOUR);
        preferences.setInt(R.string.p_rmd_quietEnd, end * MILLIS_PER_HOUR);
    }
}
