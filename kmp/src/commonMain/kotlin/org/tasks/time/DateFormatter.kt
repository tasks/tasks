package org.tasks.kmp.org.tasks.time

import org.jetbrains.compose.resources.getString
import org.tasks.data.entity.Task.Companion.hasDueTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_DAY
import org.tasks.time.plusDays
import org.tasks.time.startOfDay
import org.tasks.time.year
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.tmrw
import tasks.kmp.generated.resources.today
import tasks.kmp.generated.resources.today_lowercase
import tasks.kmp.generated.resources.tomorrow
import tasks.kmp.generated.resources.tomorrow_abbrev_lowercase
import tasks.kmp.generated.resources.tomorrow_lowercase
import tasks.kmp.generated.resources.yest
import tasks.kmp.generated.resources.yesterday
import tasks.kmp.generated.resources.yesterday_abbrev_lowercase
import tasks.kmp.generated.resources.yesterday_lowercase
import kotlin.math.abs

class DateFormatter internal constructor(
    private val strings: RelativeDayStrings,
    private val platform: PlatformDateFormatter,
    private val is24HourFormat: Boolean,
) {
    private var yearRegex: Pair<Int, Regex>? = null

    fun relativeDateTime(
        date: Long,
        style: DateStyle = DateStyle.MEDIUM,
        alwaysDisplayFullDate: Boolean = false,
        lowercase: Boolean = false,
    ): String {
        if (alwaysDisplayFullDate || !isWithinSixDays(date)) {
            return if (hasDueTime(date))
                fullDateTime(date, style)
            else
                fullDate(date, style)
        }

        val day = relativeDay(date, isAbbreviated(style), lowercase)
        return if (hasDueTime(date)) {
            val time = time(date)
            if (currentTimeMillis().startOfDay() == date.startOfDay())
                time
            else
                "$day $time"
        } else {
            day
        }
    }

    fun relativeDay(
        date: Long,
        style: DateStyle = DateStyle.MEDIUM,
        alwaysDisplayFullDate: Boolean = false,
        lowercase: Boolean = false,
    ): String =
        if (alwaysDisplayFullDate || !isWithinSixDays(date)) {
            fullDate(date, style)
        } else {
            relativeDay(date, isAbbreviated(style), lowercase)
        }

    fun fullDate(date: Long, style: DateStyle = DateStyle.LONG): String =
        stripYear(platform.date(date, style), currentTimeMillis().year)

    fun fullDateTime(date: Long, style: DateStyle = DateStyle.LONG): String =
        stripYear(platform.fullDateTime(date, is24HourFormat, style), currentTimeMillis().year)

    fun time(date: Long): String = platform.time(date, is24HourFormat)

    fun dayOfWeek(date: Long, style: TextStyle): String = platform.dayOfWeek(date, style)

    private fun relativeDay(date: Long, abbreviated: Boolean, lowercase: Boolean): String {
        val startOfToday = currentTimeMillis().startOfDay()
        val startOfDate = date.startOfDay()

        if (startOfToday == startOfDate) {
            return if (lowercase) strings.todayLowercase else strings.today
        }

        if (startOfToday.plusDays(1) == startOfDate) {
            return if (abbreviated) {
                if (lowercase) strings.tomorrowAbbrevLowercase else strings.tomorrowAbbrev
            } else {
                if (lowercase) strings.tomorrowLowercase else strings.tomorrow
            }
        }

        if (startOfDate.plusDays(1) == startOfToday) {
            return when {
                abbreviated ->
                    if (lowercase) strings.yesterdayAbbrevLowercase else strings.yesterdayAbbrev

                lowercase ->
                    strings.yesterdayLowercase

                else ->
                    strings.yesterday
            }
        }

        return dayOfWeek(date, if (abbreviated) TextStyle.SHORT else TextStyle.FULL)
    }

    private fun stripYear(date: String, year: Int): String {
        val cached = yearRegex
        val regex = if (cached != null && cached.first == year) {
            cached.second
        } else {
            "(?: de |, |/| |\\u00a0)?$year(?:年|년 |[\\s\\u00a0]г\\.)?"
                .toRegex()
                .also { yearRegex = year to it }
        }
        return date.replace(regex, "")
    }

    private fun isAbbreviated(style: DateStyle): Boolean =
        style == DateStyle.SHORT || style == DateStyle.MEDIUM

    private fun isWithinSixDays(date: Long): Boolean {
        val startOfToday = currentTimeMillis().startOfDay()
        val startOfDate = date.startOfDay()
        return abs((startOfToday - startOfDate).toDouble()) <= ONE_DAY * 6
    }

    private class Cache(
        val localeTag: String,
        val formatters: Map<Boolean, DateFormatter>,
    )

    companion object {
        @Volatile
        private var cache: Cache? = null

        fun cachedOrNull(is24HourFormat: Boolean): DateFormatter? =
            cache
                ?.takeIf { it.localeTag == currentLocaleTag() }
                ?.formatters
                ?.get(is24HourFormat)

        suspend fun create(is24HourFormat: Boolean): DateFormatter {
            cachedOrNull(is24HourFormat)?.let { return it }
            val formatter =
                DateFormatter(RelativeDayStrings.load(), PlatformDateFormatter(), is24HourFormat)
            val localeTag = currentLocaleTag()
            val existing = cache?.takeIf { it.localeTag == localeTag }?.formatters.orEmpty()
            cache = Cache(localeTag, existing + (is24HourFormat to formatter))
            return formatter
        }
    }
}

internal data class RelativeDayStrings(
    val today: String,
    val todayLowercase: String,
    val tomorrow: String,
    val tomorrowAbbrev: String,
    val tomorrowLowercase: String,
    val tomorrowAbbrevLowercase: String,
    val yesterday: String,
    val yesterdayAbbrev: String,
    val yesterdayLowercase: String,
    val yesterdayAbbrevLowercase: String,
) {
    companion object {
        suspend fun load(): RelativeDayStrings = RelativeDayStrings(
            today = getString(Res.string.today),
            todayLowercase = getString(Res.string.today_lowercase),
            tomorrow = getString(Res.string.tomorrow),
            tomorrowAbbrev = getString(Res.string.tmrw),
            tomorrowLowercase = getString(Res.string.tomorrow_lowercase),
            tomorrowAbbrevLowercase = getString(Res.string.tomorrow_abbrev_lowercase),
            yesterday = getString(Res.string.yesterday),
            yesterdayAbbrev = getString(Res.string.yest),
            yesterdayLowercase = getString(Res.string.yesterday_lowercase),
            yesterdayAbbrevLowercase = getString(Res.string.yesterday_abbrev_lowercase),
        )
    }
}
