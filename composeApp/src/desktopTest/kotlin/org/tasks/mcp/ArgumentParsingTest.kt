package org.tasks.mcp

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ArgumentParsingTest {

    private val absent = JsonObject(emptyMap())
    private val empty = buildJsonObject { put("notes", JsonPrimitive("")) }
    private val filled = buildJsonObject { put("notes", JsonPrimitive("hello")) }

    @Test
    fun anEmptyStringIsAValueAndNotAnAbsentArgument() {
        assertEquals("", empty.string("notes"))
        assertNull(absent.string("notes"))
        assertEquals("hello", filled.string("notes"))
    }

    @Test
    fun aRequiredStringRejectsEmptyAndAbsentDifferently() {
        assertEquals("hello", filled.requireString("notes"))
        assertEquals(
            "Argument 'notes' must not be empty",
            assertThrows(IllegalArgumentException::class.java) { empty.requireString("notes") }.message,
        )
        assertEquals(
            "Missing required argument 'notes'",
            assertThrows(IllegalArgumentException::class.java) { absent.requireString("notes") }.message,
        )
    }
}
