package org.tasks.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.kmp.org.tasks.time.adjustTimePattern
import org.tasks.kmp.org.tasks.time.formatFullDateTimeString
import org.tasks.kmp.org.tasks.time.formatTimeString
import org.tasks.kmp.org.tasks.time.fullDateTimePattern
import org.tasks.kmp.org.tasks.time.resolveTimePattern
import org.tasks.kmp.org.tasks.time.sanitizeTimePattern
import org.tasks.kmp.org.tasks.time.validTimePattern
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.MissingResourceException

class TimeFormatterTest {
    private val us = Locale.US
    private val ja = Locale.JAPANESE
    private val dateTime = LocalDateTime.of(2026, 3, 9, 13, 47)
    private val onTheHour = LocalDateTime.of(2026, 3, 9, 13, 0)
    private val dateStyles =
        listOf(FormatStyle.FULL, FormatStyle.LONG, FormatStyle.MEDIUM, FormatStyle.SHORT)

    @Test
    fun keepOrdinaryPatterns() {
        listOf("HH:mm", "h:mm a", "H.mm", "ah:mm").forEach {
            assertEquals(it, sanitizeTimePattern(it, us))
        }
    }

    @Test
    fun keepPatternsThatSpellPartOfTheTimeOut() {
        listOf(
            "HH 'h' mm",
            "HH 'h' mm 'min' ss 's'",
            "H:mm 'ч'.",
            "a 'ga' h:mm",
            "H'h': mm",
            "ཆུ་ཚོད་ h སྐར་མ་ mm a",
        ).forEach { assertEquals(it, sanitizeTimePattern(it, us)) }
    }

    @Test
    fun replaceFieldsIcuCouldNotPlace() {
        assertEquals("h:mm a", sanitizeTimePattern("HH '├minute: 'm'┤'", us))
        assertEquals("h:mm a", sanitizeTimePattern("HH ├minute: m┤", us))
        assertEquals("h:mm a", sanitizeTimePattern("HH '(minute: 'm')'", us))
        assertEquals("h:mm a", sanitizeTimePattern("HH (minute: m)", us))
        assertEquals("h:mm a", sanitizeTimePattern("HH:mm ├AM/PM: a┤", us))
    }

    @Test
    fun fallBackAmPmFirstForLanguagesThatLeadWithIt() {
        assertEquals("ah:mm", sanitizeTimePattern("HH ├minute: m┤", ja))
        assertEquals("ah:mm", validTimePattern("HH ├minute: m┤", ja))
    }

    @Test
    fun keepPatternsJavaTimeCanCompile() {
        listOf("HH:mm", "h:mm a", "a h:mm", "HH 'h' mm").forEach {
            assertEquals(it, validTimePattern(it, us))
        }
    }

    @Test
    fun replacePatternsJavaTimeCannotCompile() {
        listOf(
            "HH ├minute: m┤",
            "HH {2}: m",
            "HH 'unclosed quote m",
        ).forEach { assertEquals("h:mm a", validTimePattern(it, us)) }
    }

    @Test
    fun replacePatternsJavaTimeCompilesButCannotFormat() {
        listOf(
            "HH dag m",
            "HH:mm y",
            "HH:mm VV",
        ).forEach { assertEquals("h:mm a", validTimePattern(it, us)) }
    }

    @Test
    fun resolvePatternFromHealthyData() {
        assertEquals("HH:mm", resolveTimePattern(us) { "HH:mm" })
        assertEquals("h:mm a", resolveTimePattern(us) { "HH (minute: m)" })
        assertEquals("h:mm a", resolveTimePattern(us) { "HH dag m" })
    }

    @Test
    fun fallBackWhenThePatternLookupThrows() {
        assertEquals("h:mm a", resolveTimePattern(us) { throw missingResource() })
        assertEquals("ah:mm", resolveTimePattern(ja) { throw missingResource() })
    }

    @Test
    fun everyAdjustedFallbackCompiles() {
        listOf(us, ja).forEach { locale ->
            listOf(true, false).forEach { is24Hour ->
                listOf(true, false).forEach { onTheHour ->
                    val pattern = adjustTimePattern(
                        validTimePattern("HH ├minute: m┤", locale),
                        locale,
                        is24Hour,
                        onTheHour,
                    )
                    LocalTime.of(13, 0).format(DateTimeFormatter.ofPattern(pattern, locale))
                }
            }
        }
    }

    @Test
    fun adjustFallbackTo24Hour() {
        assertEquals("HH:mm", adjustTimePattern("h:mm a", us, is24HourFormat = true, onTheHour = false))
        assertEquals("HH:mm", adjustTimePattern("ah:mm", ja, is24HourFormat = true, onTheHour = false))
    }

    @Test
    fun keepAmPmBeforeTimeWhenConvertingTo12Hour() {
        assertEquals("ah:mm", adjustTimePattern("HH:mm", ja, is24HourFormat = false, onTheHour = false))
        assertEquals("h:mm a", adjustTimePattern("HH:mm", us, is24HourFormat = false, onTheHour = false))
    }

    @Test
    fun dropMinutesOnTheHour() {
        assertEquals("h a", adjustTimePattern("h:mm a", us, is24HourFormat = false, onTheHour = true))
        assertEquals("ah", adjustTimePattern("ah:mm", ja, is24HourFormat = false, onTheHour = true))
    }

    @Test
    fun swapTheTimeInTheDateTimePattern() {
        assertEquals(
            "MMMM d, y 'at' HH:mm",
            fullDateTimePattern("MMMM d, y 'at' h:mm a", "h:mm a", "HH:mm"),
        )
        assertEquals(
            "HH:mm y'年'M'月'd'日'",
            fullDateTimePattern("H:mm y'年'M'月'd'日'", "H:mm", "HH:mm"),
        )
    }

    @Test
    fun replaceFieldsIcuCouldNotPlaceInTheDateTimePattern() {
        val raw = "HH ├minute: m┤"
        val pattern = fullDateTimePattern("d MMMM y 'г'., $raw", raw, "h:mm a")
        assertEquals("d MMMM y 'г'., h:mm a", pattern)
        dateTime.format(DateTimeFormatter.ofPattern(pattern!!, us))
    }

    @Test
    fun noDateTimePatternWhenTheTimeIsNotEmbedded() {
        assertNull(fullDateTimePattern("MMMM d, y 'at' h:mm a", "HH:mm", "HH:mm"))
        assertNull(fullDateTimePattern("MMMM d, y", "", "HH:mm"))
    }

    @Test
    fun fullDateTimeKeepsTheLocalizedFormat() = withLocale(us) {
        listOf(dateTime, onTheHour).forEach { value ->
            listOf(true, false).forEach { is24Hour ->
                dateStyles.forEach { dateStyle ->
                    val localized = DateTimeFormatter
                        .ofLocalizedDateTime(dateStyle, FormatStyle.SHORT)
                        .withLocale(us)
                        .format(value)
                    val localeTime = value.toLocalTime()
                        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(us))
                    assertEquals(
                        localized.replace(localeTime, formatTimeString(value, is24Hour)),
                        formatFullDateTimeString(value, is24Hour, dateStyle),
                    )
                }
            }
        }
    }

    @Test
    fun fullDateTimeUsesTheAdjustedTime() = withLocale(us) {
        dateStyles.forEach { dateStyle ->
            assertContains("13:47", formatFullDateTimeString(dateTime, true, dateStyle))
            assertContains("1:47", formatFullDateTimeString(dateTime, false, dateStyle))
            val full = formatFullDateTimeString(onTheHour, false, dateStyle)
            assertContains(formatTimeString(onTheHour, false), full)
            assertEquals("minutes not dropped in $full", false, full.contains(":00"))
        }
    }

    @Test
    fun cachedPatternsVaryByHourFormatAndOnTheHour() = withLocale(us) {
        val combos = listOf(dateTime to true, dateTime to false, onTheHour to true, onTheHour to false)
        val first = combos.map { (value, is24Hour) -> formatTimeString(value, is24Hour) }
        val cached = combos.map { (value, is24Hour) -> formatTimeString(value, is24Hour) }
        assertEquals(first, cached)
        assertContains("13:47", first[0])
        assertContains("1:47", first[1])
        assertContains("13:00", first[2])
        assertEquals("minutes not dropped in ${first[3]}", false, first[3].contains(":00"))
    }

    @Test
    fun cachedFullDateTimeFormattersVaryByStyleAndTimePattern() = withLocale(us) {
        val combos = dateStyles.flatMap { dateStyle ->
            listOf(dateTime to true, dateTime to false, onTheHour to false)
                .map { (value, is24Hour) -> Triple(value, is24Hour, dateStyle) }
        }
        val first = combos.map { (value, is24Hour, dateStyle) ->
            formatFullDateTimeString(value, is24Hour, dateStyle)
        }
        val cached = combos.map { (value, is24Hour, dateStyle) ->
            formatFullDateTimeString(value, is24Hour, dateStyle)
        }
        assertEquals(first, cached)
        assertEquals(combos.size, first.distinct().size)
    }

    @Test
    fun cachedFullDateTimeFormattersVaryByLocale() {
        val en = withLocale(us) { formatFullDateTimeString(dateTime, true, FormatStyle.LONG) }
        val jp = withLocale(ja) { formatFullDateTimeString(dateTime, true, FormatStyle.LONG) }
        assertEquals(en, withLocale(us) { formatFullDateTimeString(dateTime, true, FormatStyle.LONG) })
        assertEquals(jp, withLocale(ja) { formatFullDateTimeString(dateTime, true, FormatStyle.LONG) })
        assertEquals("$en and $jp are not localized separately", false, en == jp)
    }

    private fun missingResource() =
        MissingResourceException("no data", "DateTimePatternGenerator", "time")

    private fun assertContains(expected: String, actual: String) =
        assertEquals("$expected not found in $actual", true, actual.contains(expected))

    private fun <T> withLocale(locale: Locale, block: () -> T): T {
        val previous = Locale.getDefault()
        Locale.setDefault(locale)
        try {
            return block()
        } finally {
            Locale.setDefault(previous)
        }
    }
}
