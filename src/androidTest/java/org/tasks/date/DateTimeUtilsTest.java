package org.tasks.date;

import android.test.AndroidTestCase;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.tasks.Snippet;

import java.util.Date;

import static org.tasks.Freeze.freezeAt;
import static org.tasks.TestUtilities.newDateTime;
import static org.tasks.date.DateTimeUtils.currentTimeMillis;
import static org.tasks.date.DateTimeUtils.newDate;
import static org.tasks.date.DateTimeUtils.newDateUtc;

public class DateTimeUtilsTest extends AndroidTestCase {

    private final DateTime now = new DateTime(2014, 1, 1, 15, 17, 53, 0);
    private final Date nowAsDate = new Date(114, 0, 1, 15, 17, 53);

    public void testGetCurrentTime() {
        freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(now.getMillis(), currentTimeMillis());
        }});
    }

    public void testGetNowAsDate() {
        freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(nowAsDate, newDate());
        }});
    }

    public void testCreateNewDate() {
        assertEquals(new Date(114, 0, 1), newDate(2014, 1, 1));
    }

    public void testCreateNewDateTime() {
        assertEquals(new Date(114, 0, 1, 15, 17, 53), newDateTime(2014, 1, 1, 15, 17, 53));
    }

    public void testCreateNewUtcDate() {
        DateTime utc = now.toDateTime(DateTimeZone.UTC);
        Date actual = newDateUtc(utc.getYear(), utc.getMonthOfYear(), utc.getDayOfMonth(), utc.getHourOfDay(), utc.getMinuteOfHour(), utc.getSecondOfMinute());
        assertEquals(utc.getMillis(), actual.getTime());
    }
}
