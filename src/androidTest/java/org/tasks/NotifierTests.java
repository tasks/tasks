package org.tasks;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;

import org.tasks.time.DateTime;
import org.tasks.preferences.Preferences;

import java.util.concurrent.TimeUnit;

import static org.tasks.Notifier.isQuietHours;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.Freeze.thaw;

public class NotifierTests extends AndroidTestCase {

    @SuppressLint("NewApi")
    private static final int MILLIS_PER_HOUR = (int) TimeUnit.HOURS.toMillis(1);

    private static final DateTime now = new DateTime(2014, 1, 23, 18, 8, 31, 540);

    private Preferences preferences;

    @Override
    public void setUp() {
        preferences = new Preferences(getContext(), null);
        preferences.clear();
        preferences.setBoolean(R.string.p_rmd_enable_quiet, true);
        freezeAt(now);
    }

    @Override
    public void tearDown() {
        thaw();
    }

    public void testNotQuietWhenQuietHoursDisabled() {
        preferences.setBoolean(R.string.p_rmd_enable_quiet, false);
        setQuietHoursStart(18);
        setQuietHoursEnd(19);

        assertFalse(isQuietHours(preferences));
    }

    public void testIsQuietAtStartOfQuietHoursNoTimeWrap() {
        setQuietHoursStart(18);
        setQuietHoursEnd(19);

        assertTrue(isQuietHours(preferences));
    }

    public void testIsNotQuietWhenStartAndEndAreSame() {
        setQuietHoursStart(18);
        setQuietHoursEnd(18);

        assertFalse(isQuietHours(preferences));
    }

    public void testIsNotQuietAtEndOfQuietHoursNoTimeWrap() {
        setQuietHoursStart(17);
        setQuietHoursEnd(18);

        assertFalse(isQuietHours(preferences));
    }

    public void testIsQuietAtStartOfQuietHoursTimeWrap() {
        setQuietHoursStart(18);
        setQuietHoursEnd(9);

        assertTrue(isQuietHours(preferences));
    }

    public void testIsNotQuietAtEndOfQuietHoursTimeWrap() {
        setQuietHoursStart(19);
        setQuietHoursEnd(18);

        assertFalse(isQuietHours(preferences));
    }

    private void setQuietHoursStart(int hour) {
        preferences.setInt(R.string.p_rmd_quietStart, hour * MILLIS_PER_HOUR);
    }

    private void setQuietHoursEnd(int hour) {
        preferences.setInt(R.string.p_rmd_quietEnd, hour * MILLIS_PER_HOUR);
    }
}
