package org.tasks.repeats

import androidx.test.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ical.values.RRule
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.locale.Locale
import java.text.ParseException

@RunWith(AndroidJUnit4::class)
class RepeatRuleToStringTest {
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
                "Wiederholt sich wöchentlich am Sa., So.",
                toString("de", "RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=SA,SU"))
    }

    private fun toString(rrule: String): String {
        return toString(null, rrule)
    }

    private fun toString(language: String?, rrule: String): String {
        return try {
            val locale = Locale(java.util.Locale.getDefault(), language)
            RepeatRuleToString(locale.createConfigurationContext(InstrumentationRegistry.getTargetContext()), locale)
                    .toString(RRule(rrule))
        } catch (e: ParseException) {
            throw RuntimeException(e)
        }
    }
}