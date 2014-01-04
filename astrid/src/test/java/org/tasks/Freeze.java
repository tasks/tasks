package org.tasks;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import java.util.Date;

public class Freeze {

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