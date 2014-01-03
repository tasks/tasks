package org.tasks;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import static org.joda.time.DateTime.now;

public class Freeze {

    public static Freeze freezeClock() {
        return freezeAt(now());
    }

    public static Freeze freezeAt(DateTime dateTime) {
        return freezeAt(dateTime.getMillis());
    }

    public static Freeze freezeAt(long millis) {
        DateTimeUtils.setCurrentMillisFixed(millis);
        return new Freeze();
    }

    public static void thaw() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    public void thawAfter(@SuppressWarnings("UnusedParameters") Snippet snippet) {
        thaw();
    }
}