/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility

import android.content.Context
import android.text.format.DateFormat
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.entity.Task.Companion.hasDueTime
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.time.DateTime
import org.tasks.time.ONE_DAY
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

object DateUtilities {
    var is24HourOverride: Boolean? = null

    /* ======================================================================
   * =========================================================== formatters
   * ====================================================================== */
    private fun is24HourFormat(context: Context): Boolean {
        return if (BuildConfig.DEBUG && is24HourOverride != null
        ) is24HourOverride!!
        else DateFormat.is24HourFormat(context)
    }

    @JvmStatic
    fun getTimeString(context: Context, date: DateTime): String {
        val value = if (is24HourFormat(context)) {
            "HH:mm"
        } else if (date.minuteOfHour == 0) {
            "h a"
        } else {
            "h:mm a"
        }
        return date.toString(value)
    }

    fun getLongDateString(date: DateTime, locale: Locale): String {
        return getFullDate(date, locale, FormatStyle.LONG)
    }

    /**
     * @param date date to format
     * @return date, with month, day, and year
     */
    fun getDateString(context: Context, date: DateTime): String {
        return getRelativeDay(
            context, date.millis, Locale.getDefault(), FormatStyle.MEDIUM
        )
    }

    fun getWeekday(date: DateTime, locale: Locale?): String {
        return date.toLocalDate()!!
            .dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    }

    /** @return weekday
     */
    fun getWeekdayShort(date: DateTime, locale: Locale?): String {
        return date.toLocalDate()!!
            .dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    }

    fun getLongDateStringWithTime(timestamp: Long, locale: Locale): String {
        return getFullDateTime(newDateTime(timestamp), locale, FormatStyle.LONG)
    }

    fun getRelativeDateTime(
        context: Context, date: Long, locale: Locale, style: FormatStyle
    ): String {
        return getRelativeDateTime(context, date, locale, style, false, false)
    }

    fun getRelativeDateTime(
        context: Context, date: Long, locale: Locale, style: FormatStyle, lowercase: Boolean
    ): String {
        return getRelativeDateTime(context, date, locale, style, false, lowercase)
    }

    fun getRelativeDateTime(
        context: Context,
        date: Long,
        locale: Locale,
        style: FormatStyle,
        alwaysDisplayFullDate: Boolean,
        lowercase: Boolean
    ): String {
        if (alwaysDisplayFullDate || !isWithinSixDays(date)) {
            return if (hasDueTime(date)
            ) getFullDateTime(newDateTime(date), locale, style)
            else getFullDate(newDateTime(date), locale, style)
        }

        val day = getRelativeDay(context, date, locale, isAbbreviated(style), lowercase)
        if (hasDueTime(date)) {
            val time = getTimeString(context, newDateTime(date))
            return if (newDateTime().startOfDay()
                    .equals(newDateTime(date).startOfDay())
            ) time else String.format("%s %s", day, time)
        } else {
            return day
        }
    }

    private fun isAbbreviated(style: FormatStyle): Boolean {
        return style == FormatStyle.SHORT || style == FormatStyle.MEDIUM
    }

    fun getRelativeDay(
        context: Context,
        date: Long,
        locale: Locale,
        style: FormatStyle
    ): String {
        return getRelativeDay(context, date, locale, style, false, false)
    }

    fun getRelativeDay(
        context: Context,
        date: Long,
        locale: Locale,
        style: FormatStyle,
        alwaysDisplayFullDate: Boolean,
        lowercase: Boolean
    ): String {
        if (alwaysDisplayFullDate) {
            return getFullDate(newDateTime(date), locale, style)
        }
        return if (isWithinSixDays(date)
        ) getRelativeDay(context, date, locale, isAbbreviated(style), lowercase)
        else getFullDate(newDateTime(date), locale, style)
    }

    private fun getFullDate(date: DateTime, locale: Locale, style: FormatStyle): String {
        return stripYear(
            DateTimeFormatter.ofLocalizedDate(style)
                .withLocale(locale)
                .format(date.toLocalDate()),
            newDateTime().year
        )
    }

    private fun getFullDateTime(date: DateTime, locale: Locale, style: FormatStyle): String {
        return stripYear(
            DateTimeFormatter.ofLocalizedDateTime(style, FormatStyle.SHORT)
                .withLocale(locale)
                .format(date.toLocalDateTime()),
            newDateTime().year
        )
    }

    private fun stripYear(date: String, year: Int): String {
        return date.replace("(?: de |, |/| )?$year(?:年|년 | г\\.)?".toRegex(), "")
    }

    private fun getRelativeDay(
        context: Context,
        date: Long,
        locale: Locale,
        abbreviated: Boolean,
        lowercase: Boolean
    ): String {
        val startOfToday = newDateTime().startOfDay()
        val startOfDate = newDateTime(date).startOfDay()

        if (startOfToday.equals(startOfDate)) {
            return context.getString(if (lowercase) R.string.today_lowercase else R.string.today)
        }

        if (startOfToday.plusDays(1).equals(startOfDate)) {
            return context.getString(
                if (abbreviated
                ) if (lowercase) R.string.tomorrow_abbrev_lowercase else R.string.tmrw
                else if (lowercase) R.string.tomorrow_lowercase else R.string.tomorrow
            )
        }

        if (startOfDate.plusDays(1).equals(startOfToday)) {
            return context.getString(
                if (abbreviated
                ) if (lowercase) R.string.yesterday_abbrev_lowercase else R.string.yest
                else if (lowercase) R.string.yesterday_lowercase else R.string.yesterday
            )
        }

        val dateTime = newDateTime(date)
        return if (abbreviated
        ) getWeekdayShort(dateTime, locale)
        else getWeekday(dateTime, locale)
    }

    private fun isWithinSixDays(date: Long): Boolean {
        val startOfToday = newDateTime().startOfDay()
        val startOfDate = newDateTime(date).startOfDay()
        return abs((startOfToday.millis - startOfDate.millis).toDouble()) <= ONE_DAY * 6
    }
}
