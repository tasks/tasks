package org.tasks.date;

import java.util.Calendar;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.GregorianCalendar;

public class DateTimeUtils {

    public static long currentTimeMillis() {
        return org.joda.time.DateTimeUtils.currentTimeMillis();
    }

    public static Date newDate() {
        return newDate(currentTimeMillis());
    }

    public static Date newDate(long date) {
        return new Date(date);
    }

    public static Date newDate(int year, int month, int day) {
        return new Date(year - 1900, month - 1, day);
    }

    public static Date newDateWithoutHours(long date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(date);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        return calendar.getTime();
    }

    public static Date newDateUtc(int year, int month, int day, int hour, int minute, int second) {
        return newDate(Date.UTC(year - 1900, month - 1, day, hour, minute, second));
    }

    public static DateTime newDateTime(long timestamp) {
        return new DateTime(timestamp);
    }

    public static String printTimestamp(long timestamp) {
        return new Date(timestamp).toString();
    }
}
