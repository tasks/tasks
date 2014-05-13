package com.todoroo.astrid.utility;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;

import com.todoroo.andlib.utility.Preferences;

import org.tasks.R;

import java.util.concurrent.TimeUnit;

import static com.todoroo.andlib.utility.Preferences.getBoolean;
import static com.todoroo.andlib.utility.Preferences.getInt;
import static com.todoroo.andlib.utility.Preferences.setStringFromInteger;
import static com.todoroo.astrid.utility.AstridDefaultPreferenceSpec.migrateToNewQuietHours;
import static org.tasks.TestUtilities.clearPreferences;

public class AstridDefaultPreferenceSpecTest extends AndroidTestCase {

    @SuppressLint("NewApi")
    private static final int MILLIS_PER_HOUR = (int) TimeUnit.HOURS.toMillis(1);

    @Override
    public void setUp() throws Exception {
        super.setUp();

        clearPreferences(getContext());

        assertFalse(hasMigrated());
    }

    public void testQuietHoursEnabledAfterMigration() {
        setOldQuietHoursStart(1);

        migrateToNewQuietHours();

        assertTrue(quietHoursEnabled());
    }

    public void testQuietHoursDisabledAfterMigration() {
        Preferences.setString(R.string.p_rmd_quietStart_old, "");

        migrateToNewQuietHours();

        assertFalse(quietHoursEnabled());
    }

    public void testMigrateFromStartOfDefaultReminderTimeArray() {
        setOldReminderTime(0);

        migrateToNewQuietHours();

        assertEquals(0, newReminderTime());
    }

    public void testMigrateFromEndOfDefaultReminderTimeArray() {
        setOldReminderTime(23);

        migrateToNewQuietHours();

        assertEquals(23 * MILLIS_PER_HOUR, newReminderTime());
    }

    public void testMigrateFromStartOfQuietHourStartArray() {
        setOldQuietHoursStart(1);

        migrateToNewQuietHours();

        assertEquals(MILLIS_PER_HOUR, newQuietHoursStartTime());
    }

    public void testMigrateFromEndOfQuietHoursStartArray() {
        setOldQuietHoursStart(23);

        migrateToNewQuietHours();

        assertEquals(23 * MILLIS_PER_HOUR, newQuietHoursStartTime());
    }

    public void testMigrateFromStartOfQuietHoursEndArray() {
        setOldQuietHoursStart(1);
        setOldQuietHoursEnd(0);

        migrateToNewQuietHours();

        assertEquals(0, newQuietHoursEndTime());
    }

    public void testMigrateFromEndOfQuietHoursEndArray() {
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
