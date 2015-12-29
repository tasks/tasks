package org.tasks.scheduling;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;

import org.tasks.R;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

import java.util.concurrent.TimeUnit;

public class AlarmManagerTests extends AndroidTestCase {

    @SuppressLint("NewApi")
    private static final int MILLIS_PER_HOUR = (int) TimeUnit.HOURS.toMillis(1);

    private Preferences preferences;
    private AlarmManager alarmManager;

    @Override
    public void setUp() {
        preferences = new Preferences(getContext(), null, null);
        preferences.clear();
        preferences.setBoolean(R.string.p_rmd_enable_quiet, true);
        alarmManager = new AlarmManager(getContext(), preferences);
    }

    public void testNotQuietWhenQuietHoursDisabled() {
        preferences.setBoolean(R.string.p_rmd_enable_quiet, false);
        setQuietHoursStart(22);
        setQuietHoursEnd(10);

        long dueDate = new DateTime(2015, 12, 29, 8, 0, 1).getMillis();

        assertEquals(dueDate, alarmManager.adjustForQuietHours(dueDate));
    }

    public void testIsQuietAtStartOfQuietHoursNoWrap() {
        setQuietHoursStart(18);
        setQuietHoursEnd(19);

        long dueDate = new DateTime(2015, 12, 29, 18, 0, 1).getMillis();

        assertEquals(new DateTime(2015, 12, 29, 19, 0).getMillis(),
                alarmManager.adjustForQuietHours(dueDate));
    }

    public void testIsQuietAtStartOfQuietHoursWrap() {
        setQuietHoursStart(22);
        setQuietHoursEnd(10);

        long dueDate = new DateTime(2015, 12, 29, 22, 0, 1).getMillis();

        assertEquals(new DateTime(2015, 12, 30, 10, 0).getMillis(),
                alarmManager.adjustForQuietHours(dueDate));
    }

    public void testAdjustForQuietHoursNightWrap() {
        setQuietHoursStart(22);
        setQuietHoursEnd(10);

        long dueDate = new DateTime(2015, 12, 29, 23, 30).getMillis();

        assertEquals(new DateTime(2015, 12, 30, 10, 0).getMillis(),
                alarmManager.adjustForQuietHours(dueDate));
    }

    public void testAdjustForQuietHoursMorningWrap() {
        setQuietHoursStart(22);
        setQuietHoursEnd(10);

        long dueDate = new DateTime(2015, 12, 30, 7, 15).getMillis();

        assertEquals(new DateTime(2015, 12, 30, 10, 0).getMillis(),
                alarmManager.adjustForQuietHours(dueDate));
    }

    public void testAdjustForQuietHoursWhenStartAndEndAreSame() {
        setQuietHoursStart(18);
        setQuietHoursEnd(18);

        long dueDate = new DateTime(2015, 12, 29, 18, 0, 0).getMillis();

        assertEquals(dueDate, alarmManager.adjustForQuietHours(dueDate));
    }

    public void testIsNotQuietAtEndOfQuietHoursNoWrap() {
        setQuietHoursStart(17);
        setQuietHoursEnd(18);

        long dueDate = new DateTime(2015, 12, 29, 18, 0).getMillis();

        assertEquals(dueDate, alarmManager.adjustForQuietHours(dueDate));
    }

    public void testIsNotQuietAtEndOfQuietHoursWrap() {
        setQuietHoursStart(22);
        setQuietHoursEnd(10);

        long dueDate = new DateTime(2015, 12, 29, 10, 0).getMillis();

        assertEquals(dueDate, alarmManager.adjustForQuietHours(dueDate));
    }

    public void testIsNotQuietBeforeNoWrap() {
        setQuietHoursStart(17);
        setQuietHoursEnd(18);

        long dueDate = new DateTime(2015, 12, 29, 11, 30).getMillis();

        assertEquals(dueDate, alarmManager.adjustForQuietHours(dueDate));
    }

    public void testIsNotQuietAfterNoWrap() {
        setQuietHoursStart(17);
        setQuietHoursEnd(18);

        long dueDate = new DateTime(2015, 12, 29, 22, 15).getMillis();

        assertEquals(dueDate, alarmManager.adjustForQuietHours(dueDate));
    }

    public void testIsNotQuietWrap() {
        setQuietHoursStart(22);
        setQuietHoursEnd(10);

        long dueDate = new DateTime(2015, 12, 29, 13, 45).getMillis();

        assertEquals(dueDate, alarmManager.adjustForQuietHours(dueDate));
    }

    private void setQuietHoursStart(int hour) {
        preferences.setInt(R.string.p_rmd_quietStart, hour * MILLIS_PER_HOUR);
    }

    private void setQuietHoursEnd(int hour) {
        preferences.setInt(R.string.p_rmd_quietEnd, hour * MILLIS_PER_HOUR);
    }
}
