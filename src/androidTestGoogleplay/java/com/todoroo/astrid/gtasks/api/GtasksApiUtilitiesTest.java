package com.todoroo.astrid.gtasks.api;

import android.test.AndroidTestCase;

import org.tasks.time.DateTime;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.gtasksCompletedTimeToUnixTime;
import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.gtasksDueTimeToUnixTime;
import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.unixTimeToGtasksCompletionTime;
import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.unixTimeToGtasksDueDate;

public class GtasksApiUtilitiesTest extends AndroidTestCase {

    private static final Locale defaultLocale = Locale.getDefault();
    private static final TimeZone defaultDateTimeZone = TimeZone.getDefault();

    @Override
    public void setUp() {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));
    }

    @Override
    public void tearDown() {
        Locale.setDefault(defaultLocale);
        TimeZone.setDefault(defaultDateTimeZone);
    }

    public void testConvertUnixToGoogleCompletionTime() {
        long now = new DateTime(2014, 1, 8, 8, 53, 20, 109).getMillis();
        assertEquals(now, unixTimeToGtasksCompletionTime(now).getValue());
    }

    public void testConvertGoogleCompletedTimeToUnixTime() {
        long now = new DateTime(2014, 1, 8, 8, 53, 20, 109).getMillis();
        com.google.api.client.util.DateTime gtime = new com.google.api.client.util.DateTime(now);
        assertEquals(now, gtasksCompletedTimeToUnixTime(gtime));
    }

    public void testConvertDueDateTimeToGoogleDueDate() {
        DateTime now = new DateTime(2014, 1, 8, 8, 53, 20, 109);

        assertEquals(
                new DateTime(2014, 1, 8, 0, 0, 0, 0, TimeZone.getTimeZone("GMT")).getMillis(),
                unixTimeToGtasksDueDate(now.getMillis()).getValue());
    }

    public void disabled_testConvertGoogleDueDateToUnixTime() {
        com.google.api.client.util.DateTime googleDueDate =
                new com.google.api.client.util.DateTime(
                        new Date(new DateTime(2014, 1, 8, 0, 0, 0, 0).getMillis()), TimeZone.getTimeZone("GMT"));

        assertEquals(
                new DateTime(2014, 1, 8, 6, 0, 0, 0).getMillis(),
                gtasksDueTimeToUnixTime(googleDueDate));
    }

    public void testConvertToInvalidGtaskTimes() {
        assertNull(unixTimeToGtasksCompletionTime(-1));
        assertNull(unixTimeToGtasksDueDate(-1));
    }

    public void testConvertFromInvalidGtaskTimes() {
        assertEquals(0, gtasksCompletedTimeToUnixTime(null));
        assertEquals(0, gtasksDueTimeToUnixTime(null));
    }
}
