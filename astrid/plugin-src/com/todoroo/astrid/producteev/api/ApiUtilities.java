package com.todoroo.astrid.producteev.api;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.SimpleTimeZone;

import com.todoroo.andlib.utility.DateUtilities;

/**
 * Utilities for working with API responses and JSON objects
 *
 * @author timsu
 *
 */
public final class ApiUtilities {

    private static final SimpleDateFormat timeParser = new SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss Z"); //$NON-NLS-1$

    static {
        // read and write dates in UTC
        Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "UTC")); //$NON-NLS-1$
        timeParser.setCalendar(cal);
    }

    /**
     * Utility method to convert PDV time to unix time
     *
     * @param date
     * @param defaultValue
     * @return
     */
    public static long producteevToUnixTime(String value, long defaultValue) {
        synchronized(timeParser) {
            try {
                return DateUtilities.dateToUnixtime(timeParser.parse(value));
            } catch (ParseException e) {
                return defaultValue;
            }
        }
    }

    /**
     * Utility method to convert unix time to PDV time
     * @param time
     * @return
     */
    public static String unixTimeToProducteev(long time) {
        synchronized(timeParser) {
            return timeParser.format(DateUtilities.unixtimeToDate(time));
        }
    }

}
