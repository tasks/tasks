package com.todoroo.andlib.utility;

import android.test.AndroidTestCase;

import org.tasks.time.DateTime;

import java.util.Locale;

import static com.todoroo.andlib.utility.DateUtilities.getRelativeDay;
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
        checkRelativeDay(new DateTime(), "Today", "Today");
    }

    public void testRelativeDayIsTomorrow() {
        checkRelativeDay(new DateTime().plusDays(1), "Tomorrow", "Tmrw");
    }

    public void testRelativeDayIsYesterday() {
        checkRelativeDay(new DateTime().minusDays(1), "Yesterday", "Yest");
    }

    public void testRelativeDayTwo() {
        checkRelativeDay(new DateTime().minusDays(2), "Sunday", "Sun");
        checkRelativeDay(new DateTime().plusDays(2), "Thursday", "Thu");
    }

    public void testRelativeDaySix() {
        checkRelativeDay(new DateTime().minusDays(6), "Wednesday", "Wed");
        checkRelativeDay(new DateTime().plusDays(6), "Monday", "Mon");
    }

    public void testRelativeDayOneWeek() {
        checkRelativeDay(new DateTime().minusDays(7), "Dec 24", "Dec 24");
    }

    public void testRelativeDayOneWeekNextYear() {
        checkRelativeDay(new DateTime().plusDays(7), "Jan 7\n2014", "Jan 7\n2014");
    }

    private void checkRelativeDay(DateTime now, String full, String abbreviated) {
        assertEquals(full, getRelativeDay(getContext(), now.getMillis(), false));
        assertEquals(abbreviated, getRelativeDay(getContext(), now.getMillis(), true));
    }
}
