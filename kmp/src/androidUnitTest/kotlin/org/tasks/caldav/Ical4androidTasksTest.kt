package org.tasks.caldav

import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.Parameter
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.parameter.RelType
import net.fortuna.ical4j.model.property.Action
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Due
import net.fortuna.ical4j.model.property.RRule
import net.fortuna.ical4j.model.property.RelatedTo
import net.fortuna.ical4j.model.property.Status
import net.fortuna.ical4j.model.property.XProperty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.icalendar.ICalDate
import java.time.Duration
import at.bitfire.ical4android.Task as Ical4androidTask
import net.fortuna.ical4j.model.property.Duration as DurationProperty

class Ical4androidTasksTest {
    @Test
    fun roundTripKeepsTheFieldsTheModelHolds() {
        val original = task()

        val restored = original.toVTodo().toIcal4android()

        assertEquals("task-uid", restored.uid)
        assertEquals(3, restored.sequence)
        assertEquals("Light the candles", restored.summary)
        assertEquals("before sundown", restored.description)
        assertEquals("home", restored.location)
        assertEquals(5, restored.priority)
        assertEquals(50, restored.percentComplete)
        assertEquals("a comment", restored.comment)
        assertEquals(Status.VTODO_IN_PROCESS.value, restored.status?.value)
        assertEquals("20260116T100000Z", restored.due?.value)
        assertEquals("FREQ=MONTHLY;INTERVAL=2", restored.rRule?.value)
        assertEquals(listOf("home", "errands"), restored.categories.toList())
    }

    @Test
    fun roundTripKeepsPropertiesTheModelDoesNotUnderstand() {
        val restored = task().toVTodo().toIcal4android()

        assertEquals(
            listOf("X-APPLE-SORT-ORDER" to "12", "X-MOZ-LASTACK" to "20260116T100000Z"),
            restored.unknownProperties.map { it.name to it.value },
        )
    }

    @Test
    fun roundTripKeepsAlarms() {
        val restored = task().toVTodo().toIcal4android()

        val alarm = restored.alarms.single()
        assertEquals(Action.DISPLAY.value, alarm.action?.value)
        assertEquals("Light the candles", alarm.description?.value)
        assertEquals("-PT15M", alarm.trigger?.value)
    }

    @Test
    fun roundTripKeepsRelationships() {
        val restored = task().toVTodo().toIcal4android()

        val related = restored.relatedTo.single()
        assertEquals("parent-uid", related.value)
        assertEquals(
            RelType.PARENT.value,
            related.getParameter<RelType>(Parameter.RELTYPE)?.value,
        )
    }

    @Test
    fun dropsStartDateAfterTheDueDate() {
        val todo = Ical4androidTask().apply {
            uid = "task-uid"
            dtStart = DtStart(DateTime("20260116T120000Z"))
            due = Due(DateTime("20260116T100000Z"))
        }.toVTodo()

        assertNull(todo.dtStart)
        assertNotNull(todo.due)
    }

    @Test
    fun promotesAnAllDayStartWhenTheDueDateHasATime() {
        val todo = Ical4androidTask().apply {
            uid = "task-uid"
            dtStart = DtStart(Date("20260116"))
            due = Due(DateTime("20260117T100000Z"))
        }.toVTodo()

        assertTrue(todo.dtStart.toString(), todo.dtStart is ICalDate.DateTime)
    }

    @Test
    fun ignoresDurationWithoutAStartDate() {
        val todo = Ical4androidTask().apply {
            uid = "task-uid"
            duration = DurationProperty(Duration.ofHours(2))
        }.toVTodo()

        assertNull(todo.duration)
    }

    private fun task() = Ical4androidTask().apply {
        uid = "task-uid"
        sequence = 3
        summary = "Light the candles"
        description = "before sundown"
        location = "home"
        priority = 5
        percentComplete = 50
        status = Status.VTODO_IN_PROCESS
        comment = "a comment"
        due = Due(DateTime("20260116T100000Z"))
        rRule = RRule("FREQ=MONTHLY;INTERVAL=2")
        categories.add("home")
        categories.add("errands")
        relatedTo.add(RelatedTo("parent-uid").apply { parameters.add(RelType.PARENT) })
        unknownProperties.add(XProperty("X-APPLE-SORT-ORDER", "12"))
        unknownProperties.add(XProperty("X-MOZ-LASTACK", "20260116T100000Z"))
        alarms.add(
            VAlarm(Duration.ofMinutes(-15)).apply {
                properties.add(Action.DISPLAY)
                properties.add(Description("Light the candles"))
            }
        )
    }
}
