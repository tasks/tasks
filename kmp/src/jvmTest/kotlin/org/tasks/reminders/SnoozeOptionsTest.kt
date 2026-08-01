package org.tasks.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.compose.pickers.QuickPickTimes
import org.tasks.time.ONE_HOUR
import org.tasks.time.millisOfDay
import org.tasks.time.plusDays
import org.tasks.time.startOfDay
import org.tasks.time.startOfMinute
import org.tasks.time.withMillisOfDay
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.date_shortcut_afternoon
import tasks.kmp.generated.resources.date_shortcut_evening
import tasks.kmp.generated.resources.date_shortcut_hour
import tasks.kmp.generated.resources.date_shortcut_morning
import tasks.kmp.generated.resources.date_shortcut_night
import tasks.kmp.generated.resources.date_shortcut_tomorrow_afternoon
import tasks.kmp.generated.resources.date_shortcut_tomorrow_evening
import tasks.kmp.generated.resources.date_shortcut_tomorrow_morning
import tasks.kmp.generated.resources.date_shortcut_tomorrow_night

class SnoozeOptionsTest {
    private val times = QuickPickTimes(
        morning = hours(9),
        afternoon = hours(13),
        evening = hours(17),
        night = hours(20),
    )

    @Test
    fun firstOptionIsAlwaysAnHourFromNow() {
        val now = today(hours(7) + 30 * 60_000)

        val first = snoozeOptions(times, now).first()

        assertEquals(Res.string.date_shortcut_hour, first.label)
        assertEquals((now + ONE_HOUR).startOfMinute(), first.timestamp)
    }

    @Test
    fun oddSecondsAreRoundedOffTheHourOption() {
        val now = today(hours(7)) + 34_567

        val hour = snoozeOptions(times, now).first().timestamp

        assertEquals(0, hour % 60_000)
    }

    @Test
    fun earlyMorningOffersTheWholeDay() {
        val options = snoozeOptions(times, today(hours(6)))

        assertEquals(
            listOf(
                Res.string.date_shortcut_hour,
                Res.string.date_shortcut_morning,
                Res.string.date_shortcut_afternoon,
                Res.string.date_shortcut_evening,
                Res.string.date_shortcut_night,
            ),
            options.map { it.label },
        )
    }

    @Test
    fun shortcutsTooCloseToNowAreSkipped() {
        val options = snoozeOptions(times, today(hours(8) + 30 * 60_000))

        assertEquals(
            listOf(
                Res.string.date_shortcut_hour,
                Res.string.date_shortcut_afternoon,
                Res.string.date_shortcut_evening,
                Res.string.date_shortcut_night,
                Res.string.date_shortcut_tomorrow_morning,
            ),
            options.map { it.label },
        )
    }

    @Test
    fun lateNightRollsEntirelyIntoTomorrow() {
        val now = today(hours(23))

        val options = snoozeOptions(times, now)

        assertEquals(
            listOf(
                Res.string.date_shortcut_hour,
                Res.string.date_shortcut_tomorrow_morning,
                Res.string.date_shortcut_tomorrow_afternoon,
                Res.string.date_shortcut_tomorrow_evening,
                Res.string.date_shortcut_tomorrow_night,
            ),
            options.map { it.label },
        )
        val tomorrowMorning = options[1].timestamp
        assertEquals(now.startOfDay().plusDays(1), tomorrowMorning.startOfDay())
        assertEquals(hours(9), tomorrowMorning.millisOfDay)
    }

    @Test
    fun everyOptionIsInTheFuture() {
        listOf(0, 6, 8, 12, 16, 19, 23).forEach { hour ->
            val now = today(hours(hour))
            snoozeOptions(times, now).forEach {
                assertTrue("$hour:00 offered ${it.label} in the past", it.timestamp > now)
            }
        }
    }

    @Test
    fun alwaysOffersFiveTimes() {
        (0..23).forEach { hour ->
            assertEquals(5, snoozeOptions(times, today(hours(hour))).size)
        }
    }

    private fun hours(hours: Int) = hours * 60 * 60 * 1000

    private fun today(millisOfDay: Int) = BASE.withMillisOfDay(millisOfDay)

    companion object {
        private val BASE = 1_700_000_000_000L.startOfDay()
    }
}
