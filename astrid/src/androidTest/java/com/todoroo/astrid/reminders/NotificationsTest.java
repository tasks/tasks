package com.todoroo.astrid.reminders;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;

import com.todoroo.andlib.utility.Preferences;

import org.joda.time.DateTime;
import org.tasks.R;

import java.util.concurrent.TimeUnit;

import static com.todoroo.astrid.reminders.Notifications.isQuietHours;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.Freeze.thaw;
import static org.tasks.TestUtilities.clearPreferences;

public class NotificationsTest extends AndroidTestCase {

    @SuppressLint("NewApi")
    private static final int MILLIS_PER_HOUR = (int) TimeUnit.HOURS.toMillis(1);

    private static final DateTime now =
            new DateTime(2014, 1, 23, 18, 8, 31, 540);

    public void setUp() {
        clearPreferences(getContext());
        Preferences.setBoolean(R.string.p_rmd_enable_quiet, true);
        freezeAt(now);
    }

    public void tearDown() {
        thaw();
    }

    public void testNotQuietWhenQuietHoursDisabled() {
        Preferences.setBoolean(R.string.p_rmd_enable_quiet, false);
        setQuietHoursStart(18);
        setQuietHoursEnd(19);

        assertFalse(isQuietHours());
    }

    public void testIsQuietAtStartOfQuietHoursNoTimeWrap() {
        setQuietHoursStart(18);
        setQuietHoursEnd(19);

        assertTrue(isQuietHours());
    }

    public void testIsNotQuietWhenStartAndEndAreSame() {
        setQuietHoursStart(18);
        setQuietHoursEnd(18);

        assertFalse(isQuietHours());
    }

    public void testIsNotQuietAtEndOfQuietHoursNoTimeWrap() {
        setQuietHoursStart(17);
        setQuietHoursEnd(18);

        assertFalse(isQuietHours());
    }

    public void testIsQuietAtStartOfQuietHoursTimeWrap() {
        setQuietHoursStart(18);
        setQuietHoursEnd(9);

        assertTrue(isQuietHours());
    }

    public void testIsNotQuietAtEndOfQuietHoursTimeWrap() {
        setQuietHoursStart(19);
        setQuietHoursEnd(18);

        assertFalse(isQuietHours());
    }

    private void setQuietHoursStart(int hour) {
        Preferences.setInt(R.string.p_rmd_quietStart, hour * MILLIS_PER_HOUR);
    }

    private void setQuietHoursEnd(int hour) {
        Preferences.setInt(R.string.p_rmd_quietEnd, hour * MILLIS_PER_HOUR);
    }
}
