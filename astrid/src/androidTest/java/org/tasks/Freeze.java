package org.tasks;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import java.util.Date;

import static org.tasks.date.DateTimeUtils.currentTimeMillis;

public class Freeze {

    public static Freeze freezeClock() {
        return freezeAt(currentTimeMillis());
    }

    public static Freeze freezeAt(Date date) {
        return freezeAt(date.getTime());
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
