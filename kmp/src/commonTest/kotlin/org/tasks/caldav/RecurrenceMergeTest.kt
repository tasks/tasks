package org.tasks.caldav

import kotlin.test.Test
import kotlin.test.assertEquals
import org.tasks.icalendar.VTodo
import org.tasks.repeats.Recur
import org.tasks.data.entity.Task

class RecurrenceMergeTest {
    @Test
    fun remoteChangeAppliesWhenTheStoredRuleMatchesTheCachedOne() {
        assertEquals(
            "FREQ=WEEKLY",
            merge(cached = "FREQ=MONTHLY", stored = "FREQ=MONTHLY", remote = "FREQ=WEEKLY"),
        )
    }

    @Test
    fun remoteChangeAppliesToRulesStoredBeforeUnknownPartsWereKept() {
        assertEquals(
            HEBREW_WEEKLY,
            merge(cached = HEBREW_MONTHLY, stored = "FREQ=MONTHLY", remote = HEBREW_WEEKLY),
        )
    }

    @Test
    fun remoteChangeAppliesOnceTheStoredRuleCarriesTheUnknownParts() {
        assertEquals(
            HEBREW_WEEKLY,
            merge(cached = HEBREW_MONTHLY, stored = HEBREW_MONTHLY, remote = HEBREW_WEEKLY),
        )
    }

    @Test
    fun aLocalEditStillWinsOverTheRemoteRule() {
        assertEquals(
            "FREQ=DAILY",
            merge(cached = HEBREW_MONTHLY, stored = "FREQ=DAILY", remote = HEBREW_WEEKLY),
        )
        assertEquals(
            "FREQ=DAILY",
            merge(cached = "FREQ=MONTHLY", stored = "FREQ=DAILY", remote = "FREQ=WEEKLY"),
        )
    }

    @Test
    fun removingTheRecurrenceLocallyIsNotUndone() {
        assertEquals(null, merge(cached = "FREQ=DAILY", stored = null, remote = "FREQ=DAILY"))
        assertEquals(null, merge(cached = HEBREW_MONTHLY, stored = null, remote = HEBREW_MONTHLY))
    }

    private fun merge(cached: String, stored: String?, remote: String): String? =
        Task(recurrence = stored)
            .applyRemote(
                remote = VTodo(rRule = Recur.parse(remote)),
                local = VTodo(rRule = Recur.parse(cached)),
            )
            .recurrence

    companion object {
        private const val HEBREW_MONTHLY = "RSCALE=HEBREW;FREQ=MONTHLY;SKIP=FORWARD"
        private const val HEBREW_WEEKLY = "RSCALE=HEBREW;FREQ=WEEKLY;SKIP=FORWARD"
    }
}
