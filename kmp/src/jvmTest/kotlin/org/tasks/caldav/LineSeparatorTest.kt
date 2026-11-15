package org.tasks.caldav

import at.bitfire.dav4jvm.ktor.DavCalendar
import at.bitfire.dav4jvm.ktor.responses
import at.bitfire.dav4jvm.property.caldav.CalendarData
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.caldav.LineSeparatorFixtures.CUT_OFF_NOTES
import org.tasks.caldav.LineSeparatorFixtures.DAMAGED
import org.tasks.caldav.LineSeparatorFixtures.NOTES
import org.tasks.caldav.LineSeparatorFixtures.VTODO
import org.tasks.caldav.LineSeparatorFixtures.multiget
import org.tasks.data.entity.Task
import org.tasks.icalendar.isWellFormedICalendar
import org.tasks.icalendar.parseVTodos
import org.tasks.icalendar.serialize

class LineSeparatorTest {
    private val url = Url("https://dav.example.com/calendars/user/list/task.ics")

    private fun downloaded(): String = runBlocking {
        val engine = MockEngine {
            respond(
                multiget(url.encodedPath),
                HttpStatusCode.MultiStatus,
                headersOf(HttpHeaders.ContentType, "application/xml; charset=utf-8"),
            )
        }
        DavCalendar(HttpClient(engine), Url("https://dav.example.com/calendars/user/list/"))
            .multiget(listOf(url))
            .responses()
            .toList()
            .single()[CalendarData::class]!!
            .iCalendar!!
    }

    @Test
    fun multigetKeepsLineSeparators() {
        val vtodo = downloaded()

        assertEquals(VTODO.replace("\r\n", "\n"), vtodo)
        assertTrue(vtodo.isWellFormedICalendar())
        with(parseVTodos(vtodo).single()) {
            assertEquals(NOTES, description)
            assertEquals(9, priority)
        }
    }

    @Test
    fun downloadedCopyRoundTrips() {
        val serialized = parseVTodos(downloaded()).single().serialize()

        assertTrue(serialized.isWellFormedICalendar())
        with(parseVTodos(serialized).single()) {
            assertEquals(NOTES, description)
            assertEquals(9, priority)
            assertEquals(listOf("X-APPLE-SORT-ORDER" to "123456"), unknownProperties.map { it.name to it.value })
        }
    }

    @Test
    fun damagedCopyIsDetected() {
        assertFalse(DAMAGED.isWellFormedICalendar())
        with(parseVTodos(DAMAGED).single()) {
            assertEquals(CUT_OFF_NOTES, description)
        }
    }

    @Test
    fun downloadingAgainRepairsUneditedFields() {
        val damaged = parseVTodos(DAMAGED).single()
        assertEquals(0, damaged.priority)
        val task = Task(title = damaged.summary, notes = damaged.description, priority = Task.Priority.NONE)

        task.applyRemote(remote = parseVTodos(downloaded()).single(), local = damaged)

        assertEquals(NOTES, task.notes)
        assertEquals(Task.Priority.LOW, task.priority)
    }

    @Test
    fun downloadingAgainKeepsEditedNotes() {
        val damaged = parseVTodos(DAMAGED).single()
        val task = Task(title = damaged.summary, notes = "edited", priority = Task.Priority.NONE)

        task.applyRemote(remote = parseVTodos(downloaded()).single(), local = damaged)

        assertEquals("edited", task.notes)
    }
}
