package org.tasks.googleapis

import com.google.api.client.util.DateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone

class GtasksDatesParityTest {
    private val original = TimeZone.getDefault()

    @After
    fun tearDown() = TimeZone.setDefault(original)

    @Test
    fun completionTimesFormatLikeTheJavaClient() = everyZone { zone, millis ->
        assertEquals(
            JavaClientDates.unixTimeToGtasksCompletionTime(millis)?.toStringRfc3339(),
            GtasksDates.unixTimeToGtasksCompletionTime(millis, zone),
        )
    }

    @Test
    fun dueDatesFormatLikeTheJavaClient() = everyZone { zone, millis ->
        assertEquals(
            JavaClientDates.unixTimeToGtasksDueDate(millis)?.toStringRfc3339(),
            GtasksDates.unixTimeToGtasksDueDate(millis, zone),
        )
    }

    @Test
    fun dueDatesParseLikeTheJavaClient() = everyZone { zone, millis ->
        val due = JavaClientDates.unixTimeToGtasksDueDate(millis)!!.toStringRfc3339()
        assertEquals(
            JavaClientDates.gtasksDueTimeToUnixTime(DateTime(due)),
            GtasksDates.gtasksDueTimeToUnixTime(due, zone),
        )
    }

    @Test
    fun completionTimesParseLikeTheJavaClient() = everyZone { _, millis ->
        val completed = JavaClientDates.unixTimeToGtasksCompletionTime(millis)!!.toStringRfc3339()
        assertEquals(
            JavaClientDates.gtasksCompletedTimeToUnixTime(DateTime(completed)),
            GtasksDates.gtasksCompletedTimeToUnixTime(completed),
        )
    }

    @Test
    fun serverTimestampsParseLikeTheJavaClient() {
        listOf(
            "2026-11-24T00:00:00.000Z",
            "2026-11-24T17:45:12.345Z",
            "2026-11-24T17:45:12Z",
            "2026-11-24T17:45:12.3Z",
            "2026-11-24T17:45:12.123456789Z",
            "2026-11-24T17:45:12.345+05:30",
            "2026-11-24T17:45:12.345-08:00",
            "2026-11-24T17:45:12.345",
            "2026-11-24T17:45:12",
            "2026-11-24",
        ).forEach {
            assertEquals(it, DateTime(it).value, GtasksDates.parse(it))
        }
    }

    @Test
    fun negativeTimesHaveNoDate() {
        assertEquals(null, GtasksDates.unixTimeToGtasksCompletionTime(-1))
        assertEquals(null, GtasksDates.unixTimeToGtasksDueDate(-1))
    }

    @Test
    fun missingDatesAreZero() {
        assertEquals(0L, GtasksDates.gtasksDueTimeToUnixTime(null))
        assertEquals(0L, GtasksDates.gtasksCompletedTimeToUnixTime(null))
    }

    private fun everyZone(block: (kotlinx.datetime.TimeZone, Long) -> Unit) {
        ZONES.forEach { id ->
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            val zone = kotlinx.datetime.TimeZone.of(id)
            instants(id).forEach { millis -> block(zone, millis) }
        }
    }

    private fun instants(id: String): List<Long> {
        val zoneId = ZoneId.of(id)
        val local = LOCAL_TIMES.map { it.atZone(zoneId).toInstant().toEpochMilli() }
        val transitions = zoneId.rules.transitions
            .filter { it.instant.epochSecond in 1_500_000_000L..1_900_000_000L }
            .flatMap { transition ->
                val at = transition.instant.toEpochMilli()
                listOf(-HOUR - 1, -1L, 0L, 1L, HOUR, DAY - 1, DAY).map { at + it }
            }
        return local + transitions + listOf(0L, 999L, 1_000L, 1_799_999_999_999L)
    }

    companion object {
        private const val HOUR = 3_600_000L
        private const val DAY = 24 * HOUR

        private val ZONES = listOf(
            "UTC",
            "America/Chicago",
            "America/New_York",
            "America/Los_Angeles",
            "America/Sao_Paulo",
            "America/St_Johns",
            "Europe/London",
            "Europe/Berlin",
            "Asia/Kolkata",
            "Asia/Kathmandu",
            "Asia/Tokyo",
            "Australia/Lord_Howe",
            "Pacific/Auckland",
            "Pacific/Chatham",
            "Pacific/Kiritimati",
            "Pacific/Pago_Pago",
        )

        private val LOCAL_TIMES = listOf(
            LocalDateTime.of(2014, 1, 8, 8, 53, 20, 109_000_000),
            LocalDateTime.of(2018, 11, 4, 0, 30),
            LocalDateTime.of(2024, 2, 29, 23, 59, 59, 999_000_000),
            LocalDateTime.of(2026, 3, 8, 2, 30),
            LocalDateTime.of(2026, 11, 1, 1, 30),
            LocalDateTime.of(2026, 12, 31, 0, 0),
            LocalDateTime.of(2026, 6, 15, 12, 0),
        )
    }
}

@Suppress("DEPRECATION")
private object JavaClientDates {
    fun unixTimeToGtasksCompletionTime(time: Long): DateTime? =
        if (time < 0) null else DateTime(Date(time), TimeZone.getDefault())

    fun gtasksCompletedTimeToUnixTime(gtasksCompletedTime: DateTime?): Long =
        gtasksCompletedTime?.value ?: 0

    fun unixTimeToGtasksDueDate(time: Long): DateTime? {
        if (time < 0) {
            return null
        }
        val date = Date(time / 1000 * 1000)
        date.hours = 0
        date.minutes = 0
        date.seconds = 0
        date.time = date.time - date.timezoneOffset * 60000
        return DateTime(date, TimeZone.getTimeZone("GMT"))
    }

    fun gtasksDueTimeToUnixTime(gtasksDueTime: DateTime?): Long {
        if (gtasksDueTime == null) {
            return 0
        }
        val date = Date(gtasksDueTime.value)
        return date.time + date.timezoneOffset * 60000
    }
}
