package org.tasks.date;

import android.test.AndroidTestCase;

import org.tasks.Snippet;
import org.tasks.time.DateTime;

import java.util.TimeZone;

import static org.tasks.Freeze.freezeAt;
import static org.tasks.date.DateTimeUtils.newDateUtc;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

public class DateTimeUtilsTest extends AndroidTestCase {

    private final DateTime now = new DateTime(2014, 1, 1, 15, 17, 53, 0);

    public void testGetCurrentTime() {
        freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(now.getMillis(), currentTimeMillis());
        }});
    }

    public void testCreateNewUtcDate() {
        DateTime utc = now.toUTC();
        DateTime actual = newDateUtc(utc.getYear(), utc.getMonthOfYear(), utc.getDayOfMonth(), utc.getHourOfDay(), utc.getMinuteOfHour(), utc.getSecondOfMinute());
        assertEquals(utc.getMillis(), actual.getMillis());
    }

    public void testIllegalInstant() {
        new DateTime(2015, 7, 24, 0, 0, 0, 0, TimeZone.getTimeZone("Africa/Cairo"));
        new DateTime(2015, 10, 18, 0, 0, 0, 0, TimeZone.getTimeZone("America/Sao_Paulo"));
        new DateTime(2015, 10, 4, 0, 0, 0, 0, TimeZone.getTimeZone("America/Asuncion"));
    }
}
