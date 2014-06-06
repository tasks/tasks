package com.todoroo.astrid.utility;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;

import org.tasks.R;
import org.tasks.preferences.Preferences;

import java.util.concurrent.TimeUnit;

public class AstridDefaultPreferenceSpecTest extends AndroidTestCase {

    @SuppressLint("NewApi")
    private static final int MILLIS_PER_HOUR = (int) TimeUnit.HOURS.toMillis(1);

    Preferences preferences;
    AstridDefaultPreferenceSpec spec;

    @Override
    public void setUp() {
        preferences = new Preferences(getContext());
        spec = new AstridDefaultPreferenceSpec(getContext(), preferences);

        preferences.clear();

        assertFalse(hasMigrated());
    }

    public void testQuietHoursEnabledAfterMigration() {
        setOldQuietHoursStart(1);

        spec.migrateToNewQuietHours();

        assertTrue(quietHoursEnabled());
    }

    public void testQuietHoursDisabledAfterMigration() {
        preferences.setString(getContext().getString(R.string.p_rmd_quietStart_old), "");

        spec.migrateToNewQuietHours();

        assertFalse(quietHoursEnabled());
    }

    public void testMigrateFromStartOfDefaultReminderTimeArray() {
        setOldReminderTime(0);

        spec.migrateToNewQuietHours();

        assertEquals(0, newReminderTime());
    }

    public void testMigrateFromEndOfDefaultReminderTimeArray() {
        setOldReminderTime(23);

        spec.migrateToNewQuietHours();

        assertEquals(23 * MILLIS_PER_HOUR, newReminderTime());
    }

    public void testMigrateFromStartOfQuietHourStartArray() {
        setOldQuietHoursStart(1);

        spec.migrateToNewQuietHours();

        assertEquals(MILLIS_PER_HOUR, newQuietHoursStartTime());
    }

    public void testMigrateFromEndOfQuietHoursStartArray() {
        setOldQuietHoursStart(23);

        spec.migrateToNewQuietHours();

        assertEquals(23 * MILLIS_PER_HOUR, newQuietHoursStartTime());
    }

    public void testMigrateFromStartOfQuietHoursEndArray() {
        setOldQuietHoursStart(1);
        setOldQuietHoursEnd(0);

        spec.migrateToNewQuietHours();

        assertEquals(0, newQuietHoursEndTime());
    }

    public void testMigrateFromEndOfQuietHoursEndArray() {
        setOldQuietHoursStart(1);
        setOldQuietHoursEnd(23);

        spec.migrateToNewQuietHours();

        assertEquals(23 * MILLIS_PER_HOUR, newQuietHoursEndTime());
    }

    private boolean quietHoursEnabled() {
        return preferences.getBoolean(R.string.p_rmd_enable_quiet);
    }

    private boolean hasMigrated() {
        return preferences.getBoolean(R.string.p_rmd_hasMigrated);
    }

    private void setOldQuietHoursStart(int index) {
        preferences.setStringFromInteger(R.string.p_rmd_quietStart_old, index);
    }

    private void setOldQuietHoursEnd(int index) {
        preferences.setStringFromInteger(R.string.p_rmd_quietEnd_old, index);
    }

    private void setOldReminderTime(int index) {
        preferences.setStringFromInteger(R.string.p_rmd_time_old, index);
    }

    private int newQuietHoursStartTime() {
        return preferences.getInt(R.string.p_rmd_quietStart);
    }

    private int newQuietHoursEndTime() {
        return preferences.getInt(R.string.p_rmd_quietEnd);
    }

    private int newReminderTime() {
        return preferences.getInt(R.string.p_rmd_time);
    }
}
