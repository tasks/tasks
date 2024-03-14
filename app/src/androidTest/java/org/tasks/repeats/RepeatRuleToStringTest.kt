package org.tasks.repeats

import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.Freeze
import org.tasks.TestUtilities.withTZ
import org.tasks.analytics.Firebase
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.time.DateTime
import java.text.ParseException
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class RepeatRuleToStringTest : InjectingTestCase() {
    @Inject lateinit var firebase: Firebase

    @Test
    fun daily() {
        assertEquals("Repeats daily", toString("RRULE:FREQ=DAILY"))
    }

    @Test
    fun weekly() {
        assertEquals("Repeats weekly", toString("RRULE:FREQ=WEEKLY;INTERVAL=1"))
    }

    @Test
    fun weeklyPlural() {
        assertEquals("Repeats every 2 weeks", toString("RRULE:FREQ=WEEKLY;INTERVAL=2"))
    }

    @Test
    fun weeklyByDay() {
        assertEquals(
                "Repeats weekly on Mon, Tue, Wed, Thu, Fri",
                toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,TU,WE,TH,FR"))
    }

    @Test
    fun printDaysInRepeatRuleOrder() {
        assertEquals(
                "Repeats weekly on Fri, Thu, Wed, Tue, Mon",
                toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=FR,TH,WE,TU,MO"))
    }

    @Test
    fun useLocaleForDays() {
        assertEquals(
                "Wiederholt sich wöchentlich Sa., So.",
                toString("de", "RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=SA,SU"))
    }

    @Test
    fun everyFifthTuesday() {
        assertEquals(
                "Repeats monthly on every fifth Tuesday",
                toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=5TU")
        )
    }

    @Test
    fun everyLastWednesday() {
        assertEquals(
                "Repeats monthly on every last Wednesday",
                toString("RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=-1WE")
        )
    }

    @Test
    fun everyFirstThursday() {
        assertEquals(
                "Repeats every 2 months on every first Thursday",
                toString("RRULE:FREQ=MONTHLY;INTERVAL=2;BYDAY=1TH")
        )
    }

    @Test
    fun repeatUntilPositiveOffset() {
        Freeze.freezeAt(DateTime(2021, 1, 4)) {
            withTZ(BERLIN) {
                assertEquals(
                    "Repeats daily, ends on February 23",
                    toString("RRULE:FREQ=DAILY;UNTIL=20210223;INTERVAL=1")
                )
            }
        }
    }

    @Test
    fun repeatUntilNoOffset() {
        Freeze.freezeAt(DateTime(2021, 1, 4)) {
            withTZ(LONDON) {
                assertEquals(
                    "Repeats daily, ends on February 23",
                    toString("RRULE:FREQ=DAILY;UNTIL=20210223;INTERVAL=1")
                )
            }
        }
    }

    @Test
    fun repeatUntilNegativeOffset() {
        Freeze.freezeAt(DateTime(2021, 1, 4)) {
            withTZ(NEW_YORK) {
                assertEquals(
                    "Repeats daily, ends on February 23",
                    toString("RRULE:FREQ=DAILY;UNTIL=20210223;INTERVAL=1")
                )
            }
        }
    }

    private fun toString(rrule: String): String? {
        return toString(null, rrule)
    }

    private fun toString(language: String?, rrule: String): String? {
        return try {
            val locale = language?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault()
            val configuration = context.resources.configuration.apply {
                setLocale(locale)
            }
            RepeatRuleToString(context.createConfigurationContext(configuration), locale, firebase)
                    .toString(rrule)
        } catch (e: ParseException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private val BERLIN = TimeZone.getTimeZone("Europe/Berlin")
        private val LONDON = TimeZone.getTimeZone("Europe/London")
        private val NEW_YORK = TimeZone.getTimeZone("America/New_York")
    }
}