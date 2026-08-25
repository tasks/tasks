package org.tasks.ai

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiTaskPromptTest {

    private val itemSchema: JsonObject
        get() = TASK_SCHEMA["properties"]!!.jsonObject["tasks"]!!.jsonObject["items"]!!.jsonObject

    @Test
    fun rootDisallowsAdditionalProperties() {
        assertEquals(JsonPrimitive(false), TASK_SCHEMA["additionalProperties"])
    }

    @Test
    fun itemsDisallowAdditionalProperties() {
        assertEquals(JsonPrimitive(false), itemSchema["additionalProperties"])
    }

    @Test
    fun everyItemPropertyIsRequired() {
        val properties = itemSchema["properties"]!!.jsonObject.keys
        val required = itemSchema["required"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet()

        assertEquals(properties, required)
    }

    @Test
    fun optionalFieldsAcceptNull() {
        val properties = itemSchema["properties"]!!.jsonObject
        for (field in listOf("notes", "list", "due", "start", "priority", "recurrence")) {
            val type = properties[field]!!.jsonObject["type"]
            assertTrue("$field should be nullable", type is JsonArray)
            assertTrue(
                "$field should allow null",
                (type as JsonArray).map { it.jsonPrimitive.content }.contains("null"),
            )
        }
    }

    @Test
    fun titleIsAPlainRequiredString() {
        val title = itemSchema["properties"]!!.jsonObject["title"]!!.jsonObject
        assertEquals(JsonPrimitive("string"), title["type"])
    }

    @Test
    fun rootRequiresTasks() {
        assertEquals(
            listOf("tasks"),
            TASK_SCHEMA["required"]!!.jsonArray.map { it.jsonPrimitive.content },
        )
    }

    private fun messages() = buildMessages(
        input = "call the dentist next Tuesday at 2",
        listNames = listOf("Personal", "Work"),
        tagNames = listOf("errand", "urgent"),
        nowIso = "2026-08-25T09:30",
        timeZoneId = "America/Chicago",
    )

    @Test
    fun systemMessageStatesTheCurrentDateAndZone() {
        val system = messages().first { it.role == "system" }.content

        assertTrue(system.contains("2026-08-25T09:30"))
        assertTrue(system.contains("America/Chicago"))
    }

    @Test
    fun systemMessageListsCandidateListsAndTags() {
        val system = messages().first { it.role == "system" }.content

        assertTrue(system.contains("Personal"))
        assertTrue(system.contains("Work"))
        assertTrue(system.contains("errand"))
        assertTrue(system.contains("urgent"))
    }

    @Test
    fun userMessageCarriesTheRawInputOnly() {
        val user = messages().first { it.role == "user" }

        assertEquals("call the dentist next Tuesday at 2", user.content)
    }

    @Test
    fun emptyCandidateSetsAreRenderedExplicitly() {
        val system = buildMessages(
            input = "buy milk",
            listNames = emptyList(),
            tagNames = emptyList(),
            nowIso = "2026-08-25T09:30",
            timeZoneId = "UTC",
        ).first { it.role == "system" }.content

        assertTrue(system.contains("(none)"))
    }

    @Test
    fun promptDoesNotLeakAnythingBeyondInputListsAndTags() {
        val system = messages().first { it.role == "system" }.content

        // The disclosure promises only the input, list names, and tag names are transmitted.
        assertFalse(system.contains("@"))
    }
}
