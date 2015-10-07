package org.tasks.date;

import org.tasks.time.DateTime;

import java.util.Date;
import java.util.TimeZone;

public class DateTimeUtils {

    public static Date newDate(long date) {
        return new Date(date);
    }

    public static Date newDate(int year, int month, int day) {
        return new Date(year - 1900, month - 1, day);
    }

    public static DateTime newDateUtc(int year, int month, int day, int hour, int minute, int second) {
        return new DateTime(year, month, day, hour, minute, second, 0, TimeZone.getTimeZone("GMT"));
    }

    public static DateTime newDateTime() {
        return new DateTime();
    }

    public static DateTime newDateTime(Date date) {
        return newDateTime(date.getTime());
    }

    public static DateTime newDateTime(long timestamp) {
        return new DateTime(timestamp);
    }
}
