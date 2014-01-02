package org.tasks.date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.tasks.Snippet;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.date.DateTimeUtils.currentTimeMillis;
import static org.tasks.date.DateTimeUtils.newDate;
import static org.tasks.date.DateTimeUtils.newDateUtc;

@RunWith(RobolectricTestRunner.class)
public class DateTimeUtilsTest {

    private final DateTime now = new DateTime(2014, 1, 1, 15, 17, 53, 0);
    private final Date nowAsDate = new Date(114, 0, 1, 15, 17, 53);

    @Test
    public void getCurrentTime() {
        freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(now.getMillis(), currentTimeMillis());
        }});
    }

    @Test
    public void getNowAsDate() {
        freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(nowAsDate, newDate());
        }});
    }

    @Test
    public void createNewDate() {
        assertEquals(new Date(114, 0, 1), newDate(2014, 1, 1));
    }

    @Test
    public void createNewDateTime() {
        assertEquals(new Date(114, 0, 1, 15, 17, 53), newDate(2014, 1, 1, 15, 17, 53));
    }

    @Test
    public void createNewUtcDate() {
        DateTime utc = now.toDateTime(DateTimeZone.UTC);
        Date actual = newDateUtc(utc.getYear(), utc.getMonthOfYear(), utc.getDayOfMonth(), utc.getHourOfDay(), utc.getMinuteOfHour(), utc.getSecondOfMinute());
        assertEquals(utc.getMillis(), actual.getTime());
    }
}
