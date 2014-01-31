package com.todoroo.astrid.reminders;

import android.annotation.SuppressLint;

import com.todoroo.andlib.utility.Preferences;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.tasks.R;

import java.util.concurrent.TimeUnit;

import static com.todoroo.astrid.reminders.Notifications.isQuietHours;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.Freeze.thaw;
import static org.tasks.TestUtilities.clearPreferences;

@RunWith(RobolectricTestRunner.class)
public class NotificationsTest {

    @SuppressLint("NewApi")
    private static final int MILLIS_PER_HOUR = (int) TimeUnit.HOURS.toMillis(1);

    private static final DateTime now =
            new DateTime(2014, 1, 23, 18, 8, 31, 540);

    @Before
    public void before() {
        clearPreferences();
        Preferences.setBoolean(R.string.p_rmd_enable_quiet, true);
        freezeAt(now);
    }

    @After
    public void after() {
        thaw();
    }

    @Test
    public void notQuietWhenQuietHoursDisabled() {
        Preferences.setBoolean(R.string.p_rmd_enable_quiet, false);
        setQuietHoursStart(18);
        setQuietHoursEnd(19);

        assertFalse(isQuietHours());
    }

    @Test
    public void isQuietAtStartOfQuietHoursNoTimeWrap() {
        setQuietHoursStart(18);
        setQuietHoursEnd(19);

        assertTrue(isQuietHours());
    }

    @Test
    public void isNotQuietWhenStartAndEndAreSame() {
        setQuietHoursStart(18);
        setQuietHoursEnd(18);

        assertFalse(isQuietHours());
    }

    @Test
    public void isNotQuietAtEndOfQuietHoursNoTimeWrap() {
        setQuietHoursStart(17);
        setQuietHoursEnd(18);

        assertFalse(isQuietHours());
    }

    @Test
    public void isQuietAtStartOfQuietHoursTimeWrap() {
        setQuietHoursStart(18);
        setQuietHoursEnd(9);

        assertTrue(isQuietHours());
    }

    @Test
    public void isNotQuietAtEndOfQuietHoursTimeWrap() {
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
