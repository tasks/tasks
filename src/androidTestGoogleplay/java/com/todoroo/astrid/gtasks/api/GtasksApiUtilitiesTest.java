package com.todoroo.astrid.gtasks.api;

import android.test.AndroidTestCase;

import com.google.api.services.tasks.model.Task;

import org.tasks.time.DateTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
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

    public void testAddHideUntilTimeNullLinks() throws ParseException {
        Long time = 123456789L;
        Task gtask = new Task();

        GtasksApiUtilities.addHideUntilTime(gtask, time);

        validateHideUntilEntry(time, gtask.getLinks());
    }

    public void testAddHideUntilTimeNonNullLinks() throws ParseException{
        long time = 123456789L;
        Task gtask = new Task();
        gtask.setLinks(new LinkedList<Task.Links>());

        GtasksApiUtilities.addHideUntilTime(gtask, time);

        validateHideUntilEntry(time, gtask.getLinks());
    }

    private void validateHideUntilEntry(long time, List<Task.Links> links) throws ParseException {
        assertEquals(1, links.size());

        Task.Links link = links.get(0);
        assertEquals(GtasksApiUtilities.LINK_TYPE, link.getType());
        assertEquals(GtasksApiUtilities.ASTRID_URL, link.getLink());

        String description = link.getDescription();
        assertTrue(description.startsWith(GtasksApiUtilities.HIDE_UNTIL + ": "));
        String datestring = description.replace(GtasksApiUtilities.HIDE_UNTIL + ": ", "");

        // Verify that we can parse the correct time out of that element
        Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH).parse(datestring);
        long javatime = date.getTime();
        long unixtime = javatime / 1000;

        assertEquals(time, unixtime);
    }

    public void testParseLinksNullList() {
        com.todoroo.astrid.data.Task task = new com.todoroo.astrid.data.Task();
        GtasksApiUtilities.parseLinks(null, task);

        assertFalse(task.hasHideUntilDate());
    }

    public void testParseLinksGarbage() {
        Task.Links taskLink = new Task.Links();
        taskLink.setType(GtasksApiUtilities.LINK_TYPE);
        taskLink.setLink(GtasksApiUtilities.ASTRID_URL);
        taskLink.setDescription(GtasksApiUtilities.HIDE_UNTIL + ": garbase");

        List<Task.Links> links = new LinkedList<>();
        links.add(taskLink);

        com.todoroo.astrid.data.Task task = new com.todoroo.astrid.data.Task();
        GtasksApiUtilities.parseLinks(links, task);

        assertFalse(task.hasHideUntilDate());
    }

    public void testMarshalHideUntilRoundtrip() {
        final Long TIME = 1234567890L;

        // Marshal hide-until metadata
        Task gtask = new Task();
        gtask.setLinks(new LinkedList<Task.Links>());
        GtasksApiUtilities.addHideUntilTime(gtask, TIME);

        // Unmarshal hide-until metadata
        com.todoroo.astrid.data.Task astridTask = new com.todoroo.astrid.data.Task();
        assertFalse(astridTask.hasHideUntilDate());
        GtasksApiUtilities.parseLinks(gtask.getLinks(), astridTask);

        assertTrue(astridTask.hasHideUntilDate());
        assertEquals(TIME, astridTask.getHideUntil());
    }
}
