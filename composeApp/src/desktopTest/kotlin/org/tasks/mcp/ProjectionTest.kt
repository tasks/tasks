package org.tasks.mcp

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectionTest {

    private val available = ApiTask.serializer().descriptor.fieldNames()

    private val row = buildJsonObject {
        put("id", JsonPrimitive(7))
        put("title", JsonPrimitive("Buy milk"))
        put("notes", JsonPrimitive("two litres"))
        put("list_id", JsonPrimitive(3))
    }

    @Test
    fun anAbsentFieldsArgumentKeepsEverything() {
        val projection = Projection.of(emptyList(), available)

        assertTrue(projection.isEverything)
        assertEquals(row, projection.apply(row))
        assertTrue(projection.includes("notes"))
    }

    @Test
    fun onlyTheAskedForFieldsSurvive() {
        val projection = Projection.of(listOf("title"), available)

        assertEquals(setOf("id", "child_count", "title"), projection.apply(row).jsonObject.keys)
    }

    @Test
    fun theIdComesBackWhetherItWasAskedForOrNot() {
        val projection = Projection.of(listOf("notes"), available)

        assertTrue(projection.includes("id"))
        assertEquals(setOf("id", "child_count", "notes"), projection.apply(row).jsonObject.keys)
    }

    @Test
    fun childCountComesBackWhetherItWasAskedForOrNot() {
        val projection = Projection.of(listOf("title"), available)

        assertTrue(projection.includes("child_count"))
        assertTrue("child_count" in projection.apply(row).jsonObject.keys)
    }

    @Test
    fun everyRowInAnArrayIsProjected() {
        val projection = Projection.of(listOf("title"), available)

        val projected = projection.apply(JsonArray(listOf(row, row))) as JsonArray

        assertEquals(2, projected.size)
        projected.forEach { assertEquals(setOf("id", "child_count", "title"), it.jsonObject.keys) }
    }

    @Test
    fun anUnknownFieldIsRejectedAndTheRealOnesAreNamed() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            Projection.of(listOf("title", "titel"), available)
        }

        assertTrue(failure.message!!, failure.message!!.contains("titel"))
        assertTrue(failure.message!!, failure.message!!.contains("list_id"))
    }

    @Test
    fun fieldNamesAreTheSerialNamesAModelSees() {
        assertTrue(available.toString(), "list_id" in available)
        assertTrue(available.toString(), "due_all_day" in available)
        assertTrue(available.toString(), "listId" !in available)
    }

    @Test
    fun everyRowTypeExposesItsOwnFields() {
        assertTrue("trigger_at" in ApiReminder.serializer().descriptor.fieldNames())
        assertTrue("display_name" in ApiPlace.serializer().descriptor.fieldNames())
        assertTrue("account_id" in ApiList.serializer().descriptor.fieldNames())
        assertTrue("repeats_on_server" in ApiAccount.serializer().descriptor.fieldNames())
    }

    @Test
    fun noRowCarriesARelatedRowsName() {
        assertTrue(available.toString(), "list_name" !in available)
        assertTrue(available.toString(), "tag_names" !in available)
        assertTrue(available.toString(), "place_name" !in available)
        assertTrue("place_name" !in ApiReminder.serializer().descriptor.fieldNames())
        assertTrue("account_name" !in ApiList.serializer().descriptor.fieldNames())
    }

    @Test
    fun anAskedForFieldIsPresentEvenWhenItIsEmpty() {
        val projection = Projection.of(listOf("title", "notes", "is_read_only"), available)
        val task = ApiTask(id = 7, title = "Buy milk", priority = "none")

        val encoded = projection.apply(projection.encoder().encodeToJsonElement(task)).jsonObject

        assertEquals(setOf("id", "child_count", "title", "notes", "is_read_only"), encoded.keys)
        assertEquals(JsonNull, encoded.getValue("notes"))
        assertEquals("false", encoded.getValue("is_read_only").toString())
    }

    @Test
    fun anUnprojectedRowStillLeavesItsEmptyFieldsOut() {
        val task = ApiTask(id = 7, title = "Buy milk", priority = "none")

        val encoded = Projection.EVERYTHING
            .apply(Projection.EVERYTHING.encoder().encodeToJsonElement(task))
            .jsonObject

        assertTrue(encoded.keys.toString(), "notes" !in encoded.keys)
        assertTrue(encoded.keys.toString(), "is_read_only" !in encoded.keys)
    }

    @Test
    fun aProjectionSaysWhatItKeptSoTheActivityLogCanShowIt() {
        assertEquals("", Projection.of(emptyList(), available).describe())
        assertTrue(Projection.of(listOf("title"), available).describe().contains("title"))
    }
}
