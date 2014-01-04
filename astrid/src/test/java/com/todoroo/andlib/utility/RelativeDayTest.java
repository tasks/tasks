package com.todoroo.andlib.utility;

import android.content.Context;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Locale;

import static com.todoroo.andlib.utility.DateUtilities.getRelativeDay;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.robolectric.Robolectric.getShadowApplication;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.Freeze.thaw;

@RunWith(RobolectricTestRunner.class)
public class RelativeDayTest {

    private static Locale defaultLocale;
    private static final DateTime now = new DateTime(2013, 12, 31, 11, 9, 42, 357);

    @Before
    public void before() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        freezeAt(now);
    }

    @After
    public void after() {
        Locale.setDefault(defaultLocale);
        thaw();
    }

    @Test
    public void relativeDayIsToday() {
        checkRelativeDay(now(), "today", "today");
    }

    @Test
    public void relativeDayIsTomorrow() {
        checkRelativeDay(now().plusDays(1), "tomorrow", "tmrw");
    }

    @Test
    public void relativeDayIsYesterday() {
        checkRelativeDay(now().minusDays(1), "yesterday", "yest");
    }

    @Test
    public void relativeDayTwo() {
        checkRelativeDay(now().minusDays(2), "Sunday", "Sun");
        checkRelativeDay(now().plusDays(2), "Thursday", "Thu");
    }

    @Test
    public void relativeDayOneWeek() {
        checkRelativeDay(now().minusDays(7), "Tuesday", "Tue");
        checkRelativeDay(now().plusDays(7), "Tuesday", "Tue");
    }

    @Test
    public void relativeDayMoreThanOneWeek() {
        checkRelativeDay(now().minusDays(8), "Dec 23", "Dec 23");
    }

    @Test
    public void relativeDayNextYear() {
        checkRelativeDay(now().plusDays(8), "Jan 8\n2014", "Jan 8\n2014");
    }

    private void checkRelativeDay(DateTime now, String full, String abbreviated) {
        final Context context = getShadowApplication().getApplicationContext();
        assertEquals(full, getRelativeDay(context, now.getMillis(), false));
        assertEquals(abbreviated, getRelativeDay(context, now.getMillis()));
    }
}
