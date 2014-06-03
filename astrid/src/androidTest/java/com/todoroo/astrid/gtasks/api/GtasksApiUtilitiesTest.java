package com.todoroo.astrid.gtasks.api;

import android.test.AndroidTestCase;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Locale;
import java.util.TimeZone;

import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.gtasksCompletedTimeToUnixTime;
import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.gtasksDueTimeToUnixTime;
import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.unixTimeToGtasksCompletionTime;
import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.unixTimeToGtasksDueDate;

public class GtasksApiUtilitiesTest extends AndroidTestCase {

    private static final Locale defaultLocale = Locale.getDefault();
    private static final DateTimeZone defaultDateTimeZone = DateTimeZone.getDefault();

    @Override
    public void setUp() {
        Locale.setDefault(Locale.US);
        DateTimeZone.setDefault(DateTimeZone.forID("America/Chicago"));
    }

    @Override
    public void tearDown() {
        Locale.setDefault(defaultLocale);
        DateTimeZone.setDefault(defaultDateTimeZone);
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
                new DateTime(2014, 1, 8, 0, 0, 0, 0, DateTimeZone.UTC).getMillis(),
                unixTimeToGtasksDueDate(now.getMillis()).getValue());
    }

    public void disabled_testConvertGoogleDueDateToUnixTime() {
        com.google.api.client.util.DateTime googleDueDate =
                new com.google.api.client.util.DateTime(
                        new DateTime(2014, 1, 8, 0, 0, 0, 0).toDate(), TimeZone.getTimeZone("UTC"));

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
