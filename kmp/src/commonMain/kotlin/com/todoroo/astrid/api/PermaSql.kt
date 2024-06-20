/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api

import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_DAY
import org.tasks.time.endOfDay
import org.tasks.time.noon

/**
 * PermaSql allows for creating SQL statements that can be saved and used later without dates
 * getting stale. It also allows these values to be used in
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
object PermaSql {
    // --- placeholder strings
    /** value to be replaced by end of day as long  */
    const val VALUE_EOD: String = "EOD()" // $NON-NLS-1$

    /** value to be replaced by noon today as long  */
    const val VALUE_NOON: String = "NOON()" // $NON-NLS-1$

    /** value to be replaced by end of day yesterday as long  */
    const val VALUE_EOD_YESTERDAY: String = "EODY()" // $NON-NLS-1$

    /** value to be replaced by end of day tomorrow as long  */
    const val VALUE_EOD_TOMORROW: String = "EODT()" // $NON-NLS-1$

    /** value to be replaced by end of day day after tomorrow as long  */
    const val VALUE_EOD_DAY_AFTER: String = "EODTT()" // $NON-NLS-1$

    /** value to be replaced by end of day next week as long  */
    const val VALUE_EOD_NEXT_WEEK: String = "EODW()" // $NON-NLS-1$

    /** value to be replaced by approximate end of day next month as long  */
    const val VALUE_EOD_NEXT_MONTH: String = "EODM()" // $NON-NLS-1$

    /** value to be replaced with the current time as long  */
    const val VALUE_NOW: String = "NOW()" // $NON-NLS-1$

    /** value to be replaced by noon yesterday as long  */
    private const val VALUE_NOON_YESTERDAY = "NOONY()" // $NON-NLS-1$

    /** value to be replaced by noon tomorrow as long  */
    private const val VALUE_NOON_TOMORROW = "NOONT()" // $NON-NLS-1$

    /** value to be replaced by noon day after tomorrow as long  */
    private const val VALUE_NOON_DAY_AFTER = "NOONTT()" // $NON-NLS-1$

    /** value to be replaced by noon next week as long  */
    private const val VALUE_NOON_NEXT_WEEK = "NOONW()" // $NON-NLS-1$

    /** value to be replaced by approximate noon next month as long  */
    private const val VALUE_NOON_NEXT_MONTH = "NOONM()" // $NON-NLS-1$

    /** Replace placeholder strings with actual  */
    fun replacePlaceholdersForQuery(value: String): String {
        var value = value
        if (value.contains(VALUE_NOW)) {
            value = value.replace(VALUE_NOW, currentTimeMillis().toString())
        }
        if (value.contains(VALUE_EOD)
            || value.contains(VALUE_EOD_DAY_AFTER)
            || value.contains(VALUE_EOD_NEXT_WEEK)
            || value.contains(VALUE_EOD_TOMORROW)
            || value.contains(VALUE_EOD_YESTERDAY)
            || value.contains(VALUE_EOD_NEXT_MONTH)
        ) {
            value = replaceEodValues(value)
        }
        if (value.contains(VALUE_NOON)
            || value.contains(VALUE_NOON_DAY_AFTER)
            || value.contains(VALUE_NOON_NEXT_WEEK)
            || value.contains(VALUE_NOON_TOMORROW)
            || value.contains(VALUE_NOON_YESTERDAY)
            || value.contains(VALUE_NOON_NEXT_MONTH)
        ) {
            value = replaceNoonValues(value)
        }
        return value
    }

    fun replacePlaceholdersForNewTask(value: String): String {
        var value = value
        if (value.contains(VALUE_NOW)) {
            value = value.replace(VALUE_NOW, currentTimeMillis().toString())
        }
        if (value.contains(VALUE_EOD)
            || value.contains(VALUE_EOD_DAY_AFTER)
            || value.contains(VALUE_EOD_NEXT_WEEK)
            || value.contains(VALUE_EOD_TOMORROW)
            || value.contains(VALUE_EOD_YESTERDAY)
            || value.contains(VALUE_EOD_NEXT_MONTH)
        ) {
            value = replaceEodValues(value, currentTimeMillis().noon())
        }
        if (value.contains(VALUE_NOON)
            || value.contains(VALUE_NOON_DAY_AFTER)
            || value.contains(VALUE_NOON_NEXT_WEEK)
            || value.contains(VALUE_NOON_TOMORROW)
            || value.contains(VALUE_NOON_YESTERDAY)
            || value.contains(VALUE_NOON_NEXT_MONTH)
        ) {
            value = replaceNoonValues(value)
        }
        return value
    }

    private fun replaceEodValues(
        value: String,
        dateTime: Long = currentTimeMillis().endOfDay()
    ): String {
        var value = value
        value = value.replace(VALUE_EOD_YESTERDAY, (dateTime - ONE_DAY).toString())
        value = value.replace(VALUE_EOD, dateTime.toString())
        value = value.replace(VALUE_EOD_TOMORROW, (dateTime + ONE_DAY).toString())
        value = value.replace(VALUE_EOD_DAY_AFTER, (dateTime + 2 * ONE_DAY).toString())
        value = value.replace(VALUE_EOD_NEXT_WEEK, (dateTime + 7 * ONE_DAY).toString())
        value = value.replace(VALUE_EOD_NEXT_MONTH, (dateTime + 30 * ONE_DAY).toString())
        return value
    }

    private fun replaceNoonValues(value: String): String {
        var value = value
        val time = currentTimeMillis().noon()
        value = value.replace(VALUE_NOON_YESTERDAY, (time - ONE_DAY).toString())
        value = value.replace(VALUE_NOON, time.toString())
        value = value.replace(VALUE_NOON_TOMORROW, (time + ONE_DAY).toString())
        value = value.replace(VALUE_NOON_DAY_AFTER, (time + 2 * ONE_DAY).toString())
        value = value.replace(VALUE_NOON_NEXT_WEEK, (time + 7 * ONE_DAY).toString())
        value = value.replace(VALUE_NOON_NEXT_MONTH, (time + 30 * ONE_DAY).toString())
        return value
    }
}
