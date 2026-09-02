package org.tasks.api

import android.content.ContentValues
import androidx.core.net.toUri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.api.TasksContract.Accounts
import org.tasks.api.TasksContract.Alarms
import org.tasks.api.TasksContract.Tags
import org.tasks.api.TasksContract.Tasks

class TasksApiErrorTest : ApiTestCase() {
    @Test
    fun sqlIsRejectedRatherThanIgnored() {
        assertThrows<IllegalArgumentException> {
            resolver.query(uri(Tasks.PATH), null, "title = ?", arrayOf("x"), null)
        }
        assertThrows<IllegalArgumentException> {
            resolver.query(uri(Tasks.PATH), null, null, null, "title ASC")
        }
        assertThrows<IllegalArgumentException> {
            val args = android.os.Bundle().apply {
                putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, "1=1")
            }
            resolver.query(uri(Tasks.PATH), null, args, null)
        }
    }

    @Test
    fun anUnknownParameterIsAnError() {
        assertThrows<IllegalArgumentException> { query(Alarms.PATH, "?tasks=1") }
        assertThrows<IllegalArgumentException> { query(Tasks.PATH, "?state=active") }
        assertThrows<IllegalArgumentException> { query(Tags.PATH, "?search=admin") }
    }

    @Test
    fun theTaskRowReplacesThePerTaskTagLookup() {
        assertThrows<IllegalArgumentException> { query(TasksContract.TaskTags.PATH, "?task_id=1") }
    }

    @Test
    fun aParameterFromAnotherEndpointIsAnError() {
        assertThrows<IllegalArgumentException> { query(Tags.PATH, "?task_id=1") }
        assertThrows<IllegalArgumentException> { query(Tasks.PATH, "?type=snooze") }
    }

    @Test
    fun badEnumValuesAreRejected() {
        assertThrows<IllegalArgumentException> { query(Tasks.PATH, "?priority=urgent") }
        assertThrows<IllegalArgumentException> { query(Tasks.PATH, "?sort=manual") }
        assertThrows<IllegalArgumentException> { query(Alarms.PATH, "?type=geo_enter") }
        assertThrows<IllegalArgumentException> { query(TasksContract.Lists.PATH, "?access=admin") }
    }

    @Test
    fun badNumbersAreRejected() {
        assertThrows<IllegalArgumentException> { query(Tasks.PATH, "?limit=lots") }
        assertThrows<IllegalArgumentException> { query(Tasks.PATH, "?limit=-1") }
        assertThrows<IllegalArgumentException> { query(Tasks.PATH, "?due_after=soon") }
    }

    @Test
    fun unknownUrisThrow() {
        assertThrows<IllegalArgumentException> {
            resolver.query("${TasksContract.CONTENT_URI}/nope".toUri(), null, null, null, null)
        }
        assertThrows<IllegalArgumentException> {
            resolver.query("content://${TasksContract.AUTHORITY}/v1/tasks".toUri(), null, null, null, null)
        }
    }

    @Test
    fun unsupportedVerbsThrow() {
        val id = newTask("t")
        assertThrows<IllegalArgumentException> {
            resolver.update(uri(Tasks.PATH), ContentValues(), null, null)
        }
        assertThrows<IllegalArgumentException> {
            resolver.insert(itemUri(Tasks.PATH, id), ContentValues())
        }
        assertThrows<IllegalArgumentException> { resolver.delete(uri(Tasks.PATH), null, null) }
        assertThrows<IllegalArgumentException> {
            resolver.insert(uri(Accounts.PATH), ContentValues())
        }
        assertThrows<IllegalArgumentException> {
            resolver.update(itemUri(TasksContract.TaskTags.PATH, 1), ContentValues(), null, null)
        }
    }

    @Test
    fun unknownColumnsInAProjectionAreDropped() {
        newTask("t")

        resolver.query(uri(Tasks.PATH), arrayOf(Tasks.TITLE, "importance"), null, null, null)!!.use {
            assertEquals(listOf(Tasks.TITLE), it.columnNames.toList())
        }
    }

    @Test
    fun unknownValuesAreRejectedOnWrite() {
        assertThrows<IllegalArgumentException> {
            insert(Tasks.PATH, Tasks.TITLE to "t", "importance" to 1)
        }
        assertThrows<IllegalArgumentException> {
            insert(Tasks.PATH, Tasks.TITLE to "t", Tasks.CREATED_AT to 1L)
        }
        val id = newTask("t")
        assertThrows<IllegalArgumentException> { update(Tasks.PATH, id, "uid" to "x") }
        assertThrows<IllegalArgumentException> { query(Tasks.PATH, "?uid=x") }
    }

    @Test
    fun requiredValuesAreRequired() {
        assertThrows<IllegalArgumentException> { insert(Tasks.PATH, Tasks.NOTES to "no title") }
        assertThrows<IllegalArgumentException> { insert(Tasks.PATH, Tasks.TITLE to "  ") }
        assertThrows<IllegalArgumentException> { insert(Tags.PATH, Tags.COLOR to 1) }
        assertThrows<IllegalArgumentException> {
            insert(Alarms.PATH, Alarms.TYPE to Alarms.TYPE_SNOOZE, Alarms.TRIGGER_AT to 1L)
        }
    }

    @Test
    fun itemUrisTakeNoFilterParameters() {
        val id = newTask("t")
        assertThrows<IllegalArgumentException> {
            resolver.query("${itemUri(Tasks.PATH, id)}?limit=1".toUri(), null, null, null, null)
        }
    }

    @Test
    fun anIdThatIsNotANumberIsRejected() {
        assertThrows<IllegalArgumentException> {
            resolver.query("${TasksContract.CONTENT_URI}/${Tasks.PATH}/abc".toUri(), null, null, null, null)
        }
    }

    @Test
    fun writingToAnUnknownTaskIsRejected() {
        assertThrows<IllegalArgumentException> {
            insert(Alarms.PATH, Alarms.TASK_ID to 9999L, Alarms.TYPE to Alarms.TYPE_SNOOZE)
        }
        assertEquals(0, resolver.delete(itemUri(Alarms.PATH, 9999), null, null))
    }
}
