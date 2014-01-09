package com.todoroo.astrid.gtasks.api;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.TimeZone;

import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.gtasksCompletedTimeToUnixTime;
import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.gtasksDueTimeToUnixTime;
import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.unixTimeToGtasksCompletionTime;
import static com.todoroo.astrid.gtasks.api.GtasksApiUtilities.unixTimeToGtasksDueDate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class GtasksApiUtilitiesTest {

    private static final DateTimeZone defaultTimeZone = DateTimeZone.getDefault();

    @Before
    public void before() {
        DateTimeZone.setDefault(DateTimeZone.forID("America/Chicago"));
    }

    @After
    public void after() {
        DateTimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void convertUnixToGoogleCompletionTime() {
        long now = new DateTime(2014, 1, 8, 8, 53, 20, 109).getMillis();
        assertEquals(now, unixTimeToGtasksCompletionTime(now).getValue());
    }

    @Test
    public void convertGoogleCompletedTimeToUnixTime() {
        long now = new DateTime(2014, 1, 8, 8, 53, 20, 109).getMillis();
        com.google.api.client.util.DateTime gtime = new com.google.api.client.util.DateTime(now);
        assertEquals(now, gtasksCompletedTimeToUnixTime(gtime));
    }

    @Test
    public void convertDueDateTimeToGoogleDueDate() {
        DateTime now = new DateTime(2014, 1, 8, 8, 53, 20, 109);

        assertEquals(
                new DateTime(2014, 1, 8, 0, 0, 0, 0, DateTimeZone.UTC).getMillis(),
                unixTimeToGtasksDueDate(now.getMillis()).getValue());
    }

    @Test
    public void convertGoogleDueDateToUnixTime() {
        com.google.api.client.util.DateTime googleDueDate =
                new com.google.api.client.util.DateTime(
                        new DateTime(2014, 1, 8, 0, 0, 0, 0).toDate(), TimeZone.getTimeZone("UTC"));

        assertEquals(
                new DateTime(2014, 1, 8, 6, 0, 0, 0).getMillis(),
                gtasksDueTimeToUnixTime(googleDueDate));
    }

    @Test
    public void convertToInvalidGtaskTimes() {
        assertNull(unixTimeToGtasksCompletionTime(-1));
        assertNull(unixTimeToGtasksDueDate(-1));
    }

    @Test
    public void convertFromInvalidGtaskTimes() {
        assertEquals(0, gtasksCompletedTimeToUnixTime(null));
        assertEquals(0, gtasksDueTimeToUnixTime(null));
    }
}
