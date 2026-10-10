package org.tasks.sync.todoist

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TodoistSyncResponseTest {
    private val json = Json {
        ignoreUnknownKeys = true
        useAlternativeNames = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Test
    fun parsesFullSyncEnvelope() {
        val response = json.decodeFromString<TodoistSyncResponse>(
            """
            {
              "sync_token": "abc123",
              "full_sync": true,
              "projects": [
                {"id": "6X7rM8997g3RQmvh", "name": "Inbox", "color": "grey", "inbox_project": true},
                {"id": "2203306141", "name": "Work", "color": "blue"}
              ],
              "items": [
                {
                  "id": "2995104339",
                  "content": "Buy milk",
                  "description": "",
                  "project_id": "2203306141",
                  "priority": 4,
                  "labels": ["errands"],
                  "checked": false,
                  "due": {"date": "2026-10-09", "string": "oct 9", "lang": "en", "is_recurring": false}
                }
              ],
              "sections": [],
              "labels": [{"id": "1", "name": "errands", "color": "red"}]
            }
            """.trimIndent()
        )
        assertEquals("abc123", response.syncToken)
        assertTrue(response.fullSync)
        assertEquals(2, response.projects.size)
        assertTrue(response.projects.first().isInboxProject)
        assertFalse(response.projects[1].isInboxProject)
        val item = response.items.single()
        assertEquals("Buy milk", item.content)
        assertEquals(4, item.priority)
        assertEquals(listOf("errands"), item.labels)
        assertEquals("2026-10-09", item.due?.date)
    }

    @Test
    fun toleratesUnknownFields() {
        // Todoist adds fields regularly; an unknown one must not fail the sync.
        val response = json.decodeFromString<TodoistSyncResponse>(
            """
            {
              "sync_token": "t",
              "some_brand_new_field": {"nested": true},
              "items": [{"id": "1", "content": "x", "a_new_item_field": 7}]
            }
            """.trimIndent()
        )
        assertEquals("t", response.syncToken)
        assertEquals("x", response.items.single().content)
    }

    @Test
    fun acceptsRestFieldNamesForTheSameConcepts() {
        // REST responses say is_completed/created_at where sync says checked/added_at.
        val item = json.decodeFromString<TodoistItem>(
            """{"id": "1", "content": "x", "is_completed": true, "created_at": "2026-01-01T00:00:00Z"}"""
        )
        assertTrue(item.checked)
        assertEquals("2026-01-01T00:00:00Z", item.addedAt)
    }

    @Test
    fun defaultsMissingOptionalFields() {
        val item = json.decodeFromString<TodoistItem>("""{"id": "1"}""")
        assertEquals("", item.content)
        assertEquals("", item.description)
        assertEquals(TodoistItem.PRIORITY_DEFAULT, item.priority)
        assertEquals(emptyList(), item.labels)
        assertFalse(item.checked)
        assertFalse(item.isDeleted)
        assertNull(item.due)
        assertNull(item.parentId)
    }

    @Test
    fun readsOkCommandStatus() {
        val response = json.decodeFromString<TodoistSyncResponse>(
            """{"sync_token": "t", "sync_status": {"uuid-1": "ok"}}"""
        )
        assertEquals(TodoistSyncResponse.CommandResult.Ok, response.statusOf("uuid-1"))
    }

    @Test
    fun readsErrorCommandStatus() {
        val response = json.decodeFromString<TodoistSyncResponse>(
            """
            {
              "sync_token": "t",
              "sync_status": {
                "uuid-1": {"error_code": 42, "error": "Item not found", "error_tag": "ITEM_NOT_FOUND"}
              }
            }
            """.trimIndent()
        )
        val result = response.statusOf("uuid-1")
        assertTrue(result is TodoistSyncResponse.CommandResult.Error)
        assertEquals(42, result.code)
        assertEquals("Item not found", result.message)
        assertTrue(result.isNotFound)
    }

    @Test
    fun distinguishesRetryableErrorsFromNotFound() {
        val response = json.decodeFromString<TodoistSyncResponse>(
            """
            {
              "sync_token": "t",
              "sync_status": {"uuid-1": {"error_code": 15, "error": "Invalid argument"}}
            }
            """.trimIndent()
        )
        val result = response.statusOf("uuid-1") as TodoistSyncResponse.CommandResult.Error
        assertFalse(result.isNotFound)
    }

    @Test
    fun reportsMissingCommandStatus() {
        val response = json.decodeFromString<TodoistSyncResponse>("""{"sync_token": "t"}""")
        assertEquals(TodoistSyncResponse.CommandResult.Missing, response.statusOf("uuid-1"))
    }

    @Test
    fun readsTempIdMapping() {
        val response = json.decodeFromString<TodoistSyncResponse>(
            """{"sync_token": "t", "temp_id_mapping": {"temp-1": "2995104339"}}"""
        )
        assertEquals("2995104339", response.tempIdMapping["temp-1"])
    }

    @Test
    fun projectAppliesOntoCalendar() {
        val calendar = org.tasks.data.entity.CaldavCalendar(account = "acct")
        TodoistProject(id = "p1", name = "Work", color = "blue").applyTo(calendar)
        assertEquals("Work", calendar.name)
        assertEquals("p1", calendar.uuid)
        assertEquals("p1", calendar.url)
        assertEquals(org.tasks.data.entity.CaldavCalendar.ACCESS_OWNER, calendar.access)
        assertEquals(TodoistColors.toArgb("blue"), calendar.color)
    }

    @Test
    fun sharedProjectIsReadWriteNotOwner() {
        val calendar = org.tasks.data.entity.CaldavCalendar(account = "acct")
        TodoistProject(id = "p1", name = "Team", isShared = true).applyTo(calendar)
        assertEquals(org.tasks.data.entity.CaldavCalendar.ACCESS_READ_WRITE, calendar.access)
    }
}

class TodoistColorsTest {
    @Test
    fun mapsPaletteNamesToOpaqueArgb() {
        assertEquals(0xFFdb4035.toInt(), TodoistColors.toArgb("red"))
        assertEquals(0xFF4073ff.toInt(), TodoistColors.toArgb("blue"))
    }

    @Test
    fun returnsNullForUnknownName() {
        assertNull(TodoistColors.toArgb("chartreuse"))
    }

    @Test
    fun roundTripsEveryPaletteEntry() {
        listOf(
            "berry_red", "red", "orange", "yellow", "olive_green", "lime_green", "green",
            "mint_green", "teal", "sky_blue", "light_blue", "blue", "grape", "violet",
            "lavender", "magenta", "salmon", "charcoal", "grey", "taupe",
        ).forEach { name ->
            val argb = TodoistColors.toArgb(name)!!
            assertEquals(name, TodoistColors.toName(argb), name)
        }
    }

    @Test
    fun snapsArbitraryColorToNearestPaletteEntry() {
        // Pure red is closest to the "red" entry.
        assertEquals("red", TodoistColors.toName(0xFFFF0000.toInt()))
        assertEquals("blue", TodoistColors.toName(0xFF3366FF.toInt()))
    }

    @Test
    fun greyscaleStaysGreyscale() {
        // The palette has no dark neutral, so raw nearest-neighbour would map black
        // onto a dark green. Achromatic input must stay achromatic.
        listOf(0xFF000000, 0xFF101010, 0xFF808080, 0xFFC0C0C0, 0xFFFFFFFF).forEach {
            val name = TodoistColors.toName(it.toInt())
            assertTrue(
                name in setOf("charcoal", "grey", "taupe"),
                "expected an achromatic name for ${it.toString(16)}, got $name",
            )
        }
        assertEquals("charcoal", TodoistColors.toName(0xFF000000.toInt()))
    }

    @Test
    fun unsetColorFallsBackToDefault() {
        assertEquals(TodoistColors.DEFAULT_NAME, TodoistColors.toName(0))
    }
}
