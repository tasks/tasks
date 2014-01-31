package com.todoroo.astrid.utility;

import android.annotation.SuppressLint;

import com.todoroo.andlib.utility.Preferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.tasks.R;

import java.util.concurrent.TimeUnit;

import static com.todoroo.andlib.utility.Preferences.getBoolean;
import static com.todoroo.andlib.utility.Preferences.getInt;
import static com.todoroo.andlib.utility.Preferences.setStringFromInteger;
import static com.todoroo.astrid.utility.AstridDefaultPreferenceSpec.migrateToNewQuietHours;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.tasks.TestUtilities.clearPreferences;

@RunWith(RobolectricTestRunner.class)
public class AstridDefaultPreferenceSpecTest {

    @SuppressLint("NewApi")
    private static final int MILLIS_PER_HOUR = (int) TimeUnit.HOURS.toMillis(1);

    @Before
    public void before() {
        clearPreferences();

        assertFalse(hasMigrated());
    }

    @After
    public void after() {
        assertTrue(hasMigrated());
    }

    @Test
    public void quietHoursEnabledAfterMigration() {
        setOldQuietHoursStart(1);

        migrateToNewQuietHours();

        assertTrue(quietHoursEnabled());
    }

    @Test
    public void quietHoursDisabledAfterMigration() {
        Preferences.setString(R.string.p_rmd_quietStart_old, "");

        migrateToNewQuietHours();

        assertFalse(quietHoursEnabled());
    }

    @Test
    public void migrateFromStartOfDefaultReminderTimeArray() {
        setOldReminderTime(0);

        migrateToNewQuietHours();

        assertEquals(0, newReminderTime());
    }

    @Test
    public void migrateFromEndOfDefaultReminderTimeArray() {
        setOldReminderTime(23);

        migrateToNewQuietHours();

        assertEquals(23 * MILLIS_PER_HOUR, newReminderTime());
    }

    @Test
    public void migrateFromStartOfQuietHourStartArray() {
        setOldQuietHoursStart(1);

        migrateToNewQuietHours();

        assertEquals(MILLIS_PER_HOUR, newQuietHoursStartTime());
    }

    @Test
    public void migrateFromEndOfQuietHoursStartArray() {
        setOldQuietHoursStart(23);

        migrateToNewQuietHours();

        assertEquals(23 * MILLIS_PER_HOUR, newQuietHoursStartTime());
    }

    @Test
    public void migrateFromStartOfQuietHoursEndArray() {
        setOldQuietHoursStart(1);
        setOldQuietHoursEnd(0);

        migrateToNewQuietHours();

        assertEquals(0, newQuietHoursEndTime());
    }

    @Test
    public void migrateFromEndOfQuietHoursEndArray() {
        setOldQuietHoursStart(1);
        setOldQuietHoursEnd(23);

        migrateToNewQuietHours();

        assertEquals(23 * MILLIS_PER_HOUR, newQuietHoursEndTime());
    }

    private boolean quietHoursEnabled() {
        return getBoolean(R.string.p_rmd_enable_quiet);
    }

    private boolean hasMigrated() {
        return getBoolean(R.string.p_rmd_hasMigrated);
    }

    private void setOldQuietHoursStart(int index) {
        setStringFromInteger(R.string.p_rmd_quietStart_old, index);
    }

    private void setOldQuietHoursEnd(int index) {
        setStringFromInteger(R.string.p_rmd_quietEnd_old, index);
    }

    private void setOldReminderTime(int index) {
        setStringFromInteger(R.string.p_rmd_time_old, index);
    }

    private int newQuietHoursStartTime() {
        return getInt(R.string.p_rmd_quietStart);
    }

    private int newQuietHoursEndTime() {
        return getInt(R.string.p_rmd_quietEnd);
    }

    private int newReminderTime() {
        return getInt(R.string.p_rmd_time);
    }
}
