/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import java.util.Date;

import com.todoroo.andlib.utility.DateUtilities;

/**
 * PermaSql allows for creating SQL statements that can be saved and used
 * later without dates getting stale. It also allows these values to be
 * used in
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class PermaSql {

    // --- placeholder strings

    /** value to be replaced with the current time as long */
    public static final String VALUE_NOW = "NOW()"; //$NON-NLS-1$

    /** value to be replaced by end of day as long */
    public static final String VALUE_EOD = "EOD()"; //$NON-NLS-1$

    /** value to be replaced by noon today as long */
    public static final String VALUE_NOON = "NOON()"; //$NON-NLS-1$

    /** value to be replaced by end of day yesterday as long */
    public static final String VALUE_EOD_YESTERDAY = "EODY()"; //$NON-NLS-1$

    /** value to be replaced by noon yesterday as long */
    public static final String VALUE_NOON_YESTERDAY = "NOONY()"; //$NON-NLS-1$

    /** value to be replaced by end of day tomorrow as long */
    public static final String VALUE_EOD_TOMORROW = "EODT()"; //$NON-NLS-1$

    /** value to be replaced by noon tomorrow as long */
    public static final String VALUE_NOON_TOMORROW = "NOONT()"; //$NON-NLS-1$

    /** value to be replaced by end of day day after tomorrow as long */
    public static final String VALUE_EOD_DAY_AFTER = "EODTT()"; //$NON-NLS-1$

    /** value to be replaced by noon day after tomorrow as long */
    public static final String VALUE_NOON_DAY_AFTER = "NOONTT()"; //$NON-NLS-1$

    /** value to be replaced by end of day next week as long */
    public static final String VALUE_EOD_NEXT_WEEK = "EODW()"; //$NON-NLS-1$

    /** value to be replaced by noon next week as long */
    public static final String VALUE_NOON_NEXT_WEEK = "NOONW()"; //$NON-NLS-1$

    /** value to be replaced by approximate end of day next month as long */
    public static final String VALUE_EOD_NEXT_MONTH = "EODM()"; //$NON-NLS-1$

    /** value to be replaced by approximate noon next month as long */
    public static final String VALUE_NOON_NEXT_MONTH = "NOONM()"; //$NON-NLS-1$

    /** Replace placeholder strings with actual */
    public static String replacePlaceholders(String value) {
        if(value.contains(VALUE_NOW))
            value = value.replace(VALUE_NOW, Long.toString(DateUtilities.now()));
        if(value.contains(VALUE_EOD) || value.contains(VALUE_EOD_DAY_AFTER) ||
                value.contains(VALUE_EOD_NEXT_WEEK) || value.contains(VALUE_EOD_TOMORROW) ||
                value.contains(VALUE_EOD_YESTERDAY) || value.contains(VALUE_EOD_NEXT_MONTH)) {
            value = replaceEodValues(value);
        }
        if(value.contains(VALUE_NOON) || value.contains(VALUE_NOON_DAY_AFTER) ||
                value.contains(VALUE_NOON_NEXT_WEEK) || value.contains(VALUE_NOON_TOMORROW) ||
                value.contains(VALUE_NOON_YESTERDAY) || value.contains(VALUE_NOON_NEXT_MONTH)) {
            value = replaceNoonValues(value);
        }
        return value;
    }

    private static String replaceEodValues(String value) {
        Date date = new Date();
        date.setHours(23);
        date.setMinutes(59);
        date.setSeconds(59);
        long time = date.getTime() / 1000l * 1000l; // chop milliseconds off
        value = value.replace(VALUE_EOD_YESTERDAY, Long.toString(time - DateUtilities.ONE_DAY));
        value = value.replace(VALUE_EOD, Long.toString(time));
        value = value.replace(VALUE_EOD_TOMORROW, Long.toString(time + DateUtilities.ONE_DAY));
        value = value.replace(VALUE_EOD_DAY_AFTER, Long.toString(time + 2 * DateUtilities.ONE_DAY));
        value = value.replace(VALUE_EOD_NEXT_WEEK, Long.toString(time + 7 * DateUtilities.ONE_DAY));
        value = value.replace(VALUE_EOD_NEXT_MONTH, Long.toString(time + 30 * DateUtilities.ONE_DAY));
        return value;
    }

    private static String replaceNoonValues(String value) {
        Date date = new Date();
        date.setHours(12);
        date.setMinutes(0);
        date.setSeconds(0);
        long time = date.getTime() / 1000l * 1000l; // chop milliseconds off
        value = value.replace(VALUE_NOON_YESTERDAY, Long.toString(time - DateUtilities.ONE_DAY));
        value = value.replace(VALUE_NOON, Long.toString(time));
        value = value.replace(VALUE_NOON_TOMORROW, Long.toString(time + DateUtilities.ONE_DAY));
        value = value.replace(VALUE_NOON_DAY_AFTER, Long.toString(time + 2 * DateUtilities.ONE_DAY));
        value = value.replace(VALUE_NOON_NEXT_WEEK, Long.toString(time + 7 * DateUtilities.ONE_DAY));
        value = value.replace(VALUE_NOON_NEXT_MONTH, Long.toString(time + 30 * DateUtilities.ONE_DAY));
        return value;
    }

}
