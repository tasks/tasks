package com.todoroo.andlib.utility;

import android.test.AndroidTestCase;

import org.joda.time.DateTime;

import java.util.Locale;

import static com.todoroo.andlib.utility.DateUtilities.getRelativeDay;
import static org.joda.time.DateTime.now;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.Freeze.thaw;

public class RelativeDayTest extends AndroidTestCase {

    private static Locale defaultLocale;
    private static final DateTime now = new DateTime(2013, 12, 31, 11, 9, 42, 357);

    @Override
    public void setUp() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        freezeAt(now);
    }

    @Override
    public void tearDown() {
        Locale.setDefault(defaultLocale);
        thaw();
    }

    public void testRelativeDayIsToday() {
        checkRelativeDay(now(), "today", "today");
    }

    public void testRelativeDayIsTomorrow() {
        checkRelativeDay(now().plusDays(1), "tomorrow", "tmrw");
    }

    public void testRelativeDayIsYesterday() {
        checkRelativeDay(now().minusDays(1), "yesterday", "yest");
    }

    public void testRelativeDayTwo() {
        checkRelativeDay(now().minusDays(2), "Sunday", "Sun");
        checkRelativeDay(now().plusDays(2), "Thursday", "Thu");
    }

    public void testRelativeDaySix() {
        checkRelativeDay(now().minusDays(6), "Wednesday", "Wed");
        checkRelativeDay(now().plusDays(6), "Monday", "Mon");
    }

    public void testRelativeDayOneWeek() {
        checkRelativeDay(now().minusDays(7), "Dec 24", "Dec 24");
    }

    public void testRelativeDayOneWeekNextYear() {
        checkRelativeDay(now().plusDays(7), "Jan 7\n2014", "Jan 7\n2014");
    }

    private void checkRelativeDay(DateTime now, String full, String abbreviated) {
        assertEquals(full, getRelativeDay(getContext(), now.getMillis(), false));
        assertEquals(abbreviated, getRelativeDay(getContext(), now.getMillis()));
    }
}
