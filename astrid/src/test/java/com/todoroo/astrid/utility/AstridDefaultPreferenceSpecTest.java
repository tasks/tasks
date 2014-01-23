package com.todoroo.astrid.utility;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.tasks.R;

import static com.todoroo.andlib.utility.Preferences.getBoolean;
import static com.todoroo.andlib.utility.Preferences.getStringValue;
import static com.todoroo.andlib.utility.Preferences.setStringFromInteger;
import static com.todoroo.astrid.utility.AstridDefaultPreferenceSpec.migrateToNewQuietHours;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.tasks.TestUtilities.clearPreferences;

@RunWith(RobolectricTestRunner.class)
public class AstridDefaultPreferenceSpecTest {

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
        setOldQuietHoursStart(0);

        migrateToNewQuietHours();

        assertFalse(quietHoursEnabled());
    }

    @Test
    public void migrateFromStartOfDefaultReminderTimeArray() {
        setOldReminderTime(0);

        migrateToNewQuietHours();

        assertEquals("9", newReminderTime());
    }

    @Test
    public void migrateFromEndOfDefaultReminderTimeArray() {
        setOldReminderTime(23);

        migrateToNewQuietHours();

        assertEquals("8", newReminderTime());
    }

    @Test
    public void migrateFromStartOfQuietHourStartArray() {
        setOldQuietHoursStart(1);

        migrateToNewQuietHours();

        assertEquals("20", newQuietHoursStartTime());
    }

    @Test
    public void migrateFromEndOfQuietHoursStartArray() {
        setOldQuietHoursStart(24);

        migrateToNewQuietHours();

        assertEquals("19", newQuietHoursStartTime());
    }

    @Test
    public void migrateFromStartOfQuietHoursEndArray() {
        setOldQuietHoursStart(1);
        setOldQuietHoursEnd(0);

        migrateToNewQuietHours();

        assertEquals("9", newQuietHoursEndTime());
    }

    @Test
    public void migrateFromEndOfQuietHoursEndArray() {
        setOldQuietHoursStart(1);
        setOldQuietHoursEnd(23);

        migrateToNewQuietHours();

        assertEquals("8", newQuietHoursEndTime());
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

    private String newQuietHoursStartTime() {
        return getStringValue(R.string.p_rmd_quietStart);
    }

    private String newQuietHoursEndTime() {
        return getStringValue(R.string.p_rmd_quietEnd);
    }

    private String newReminderTime() {
        return getStringValue(R.string.p_rmd_time);
    }
}
