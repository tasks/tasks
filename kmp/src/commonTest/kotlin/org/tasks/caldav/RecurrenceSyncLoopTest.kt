package org.tasks.caldav

import kotlin.test.Test
import kotlin.test.assertEquals
import org.tasks.caldav.iCalendar.Companion.applyLocal
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.icalendar.parseVTodos
import org.tasks.icalendar.serialize
import org.tasks.repeats.Recur

class RecurrenceSyncLoopTest {
    @Test
    fun aRuleTheAppDoesNotModelSurvivesTheReadAndPushLoop() {
        val task = read(HEBREW)

        assertEquals(HEBREW, task.recurrence)
        assertEquals(Recur.parse(HEBREW), push(task))
    }

    @Test
    fun anOrdinaryRuleSurvivesTheReadAndPushLoop() {
        val task = read("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE")

        assertEquals("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE", task.recurrence)
        assertEquals(Recur.parse("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE"), push(task))
    }

    @Test
    fun editingTheRecurrenceReplacesPartsTheAppDoesNotModel() {
        val task = read(HEBREW).also { it.recurrence = "FREQ=WEEKLY" }

        assertEquals(Recur.parse("FREQ=WEEKLY"), push(task))
    }

    private fun read(rrule: String): Task =
        Task().applyRemote(remote = parseVTodos(ics(rrule)).single(), local = null)

    private fun push(task: Task): Recur? =
        parseVTodos(ics(HEBREW))
            .single()
            .also { it.applyLocal(CaldavTask(task = 1, calendar = "c", remoteId = "uid"), task) }
            .serialize()
            .let { parseVTodos(it).single().rRule }

    private fun ics(rrule: String) = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Example//EN
        BEGIN:VTODO
        UID:uid
        SUMMARY:Light the candles
        DUE:20260301T090000Z
        RRULE:$rrule
        END:VTODO
        END:VCALENDAR
    """.trimIndent()

    companion object {
        private const val HEBREW = "RSCALE=HEBREW;FREQ=MONTHLY;INTERVAL=2;SKIP=FORWARD"
    }
}
