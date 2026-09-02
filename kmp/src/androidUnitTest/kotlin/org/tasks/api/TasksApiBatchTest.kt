package org.tasks.api

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.OperationApplicationException
import androidx.core.net.toUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.api.TasksContract.Alarms
import org.tasks.api.TasksContract.Lists
import org.tasks.api.TasksContract.Places
import org.tasks.api.TasksContract.Tags
import org.tasks.api.TasksContract.TaskTags
import org.tasks.api.TasksContract.Tasks
import java.util.concurrent.TimeUnit

class TasksApiBatchTest : ApiTestCase() {
    @Test
    fun aTaskAndItsChildrenLandTogether() {
        insert(Tags.PATH, Tags.NAME to "admin")
        val tagId = query(Tags.PATH).long(TasksContract.ID)

        val results = resolver.applyBatch(
            TasksContract.AUTHORITY,
            arrayListOf(
                ContentProviderOperation.newInsert(uri(Tasks.PATH))
                    .withValue(Tasks.TITLE, "Renew passport")
                    .withValue(Tasks.DUE_DATE, 1_700_000_000_000L)
                    .withValue(Tasks.DUE_ALL_DAY, 1)
                    .withValue(Tasks.LIST_ID, listId)
                    .build(),
                ContentProviderOperation.newInsert(uri(Alarms.PATH))
                    .withValueBackReference(Alarms.TASK_ID, 0)
                    .withValue(Alarms.TYPE, Alarms.TYPE_RELATIVE_DUE)
                    .withValue(Alarms.OFFSET_MS, -TimeUnit.DAYS.toMillis(7))
                    .build(),
                ContentProviderOperation.newInsert(uri(TaskTags.PATH))
                    .withValueBackReference(TaskTags.TASK_ID, 0)
                    .withValue(TaskTags.TAG_ID, tagId)
                    .build(),
            ),
        )

        assertEquals(3, results.size)
        val taskId = ContentUris.parseId(results[0].uri!!)
        assertEquals("Renew passport", query(Tasks.PATH).string(Tasks.TITLE))
        assertEquals("$tagId", query(Tasks.PATH, "?_id=$taskId").string(Tasks.TAG_IDS))
        assertEquals(1, query(Alarms.PATH, "?task_id=$taskId").rows())
        resolver.query(results[1].uri!!, null, null, null, null)!!.use {
            assertTrue(it.moveToFirst())
            assertEquals(taskId, it.getLong(it.getColumnIndexOrThrow(Alarms.TASK_ID)))
            assertEquals(
                -TimeUnit.DAYS.toMillis(7),
                it.getLong(it.getColumnIndexOrThrow(Alarms.OFFSET_MS)),
            )
        }
    }

    @Test
    fun anObjectGraphIsOneBatch() {
        val results = resolver.applyBatch(
            TasksContract.AUTHORITY,
            arrayListOf(
                ContentProviderOperation.newInsert(uri(Tags.PATH))
                    .withValue(Tags.NAME, "admin")
                    .build(),
                ContentProviderOperation.newInsert(uri(Places.PATH))
                    .withValue(Places.NAME, "Home")
                    .withValue(Places.LATITUDE, 51.5)
                    .withValue(Places.LONGITUDE, -0.1)
                    .build(),
                ContentProviderOperation.newInsert(uri(Tasks.PATH))
                    .withValue(Tasks.TITLE, "Renew passport")
                    .build(),
                ContentProviderOperation.newInsert(uri(TaskTags.PATH))
                    .withValueBackReference(TaskTags.TASK_ID, 2)
                    .withValueBackReference(TaskTags.TAG_ID, 0)
                    .build(),
                ContentProviderOperation.newInsert(uri(Alarms.PATH))
                    .withValueBackReference(Alarms.TASK_ID, 2)
                    .withValueBackReference(Alarms.PLACE_ID, 1)
                    .withValue(Alarms.TYPE, Alarms.TYPE_LOCATION_ARRIVAL)
                    .build(),
            ),
        )

        val tagId = ContentUris.parseId(results[0].uri!!)
        val placeId = ContentUris.parseId(results[1].uri!!)
        val taskId = ContentUris.parseId(results[2].uri!!)
        resolver.query(itemUri(Tasks.PATH, taskId), null, null, null, null)!!.use {
            assertTrue(it.moveToFirst())
            assertEquals("$tagId", it.getString(it.getColumnIndexOrThrow(Tasks.TAG_IDS)))
            assertEquals(placeId, it.getLong(it.getColumnIndexOrThrow(Tasks.PLACE_ID)))
        }
        assertEquals(
            listOf(Alarms.TYPE_LOCATION_ARRIVAL),
            query(Alarms.PATH, "?task_id=$taskId").strings(Alarms.TYPE),
        )
    }

    @Test
    fun aLocationReminderIsOneBatch() {
        insert(Places.PATH, Places.NAME to "Home", Places.LATITUDE to 51.5, Places.LONGITUDE to -0.1)
        val placeId = query(Places.PATH).long(TasksContract.ID)

        resolver.applyBatch(
            TasksContract.AUTHORITY,
            arrayListOf(
                ContentProviderOperation.newInsert(uri(Tasks.PATH))
                    .withValue(Tasks.TITLE, "Buy milk")
                    .build(),
                ContentProviderOperation.newInsert(uri(Alarms.PATH))
                    .withValueBackReference(Alarms.TASK_ID, 0)
                    .withValue(Alarms.PLACE_ID, placeId)
                    .withValue(Alarms.TYPE, Alarms.TYPE_LOCATION_DEPARTURE)
                    .build(),
            ),
        )

        assertEquals(
            listOf(Alarms.TYPE_LOCATION_DEPARTURE),
            query(Alarms.PATH).strings(Alarms.TYPE),
        )
        assertEquals(listOf("Buy milk"), query(Tasks.PATH, "?place_id=$placeId").strings(Tasks.TITLE))
    }

    @Test
    fun nothingIsAppliedWhenAnOperationFails() {
        assertThrows<IllegalArgumentException> {
            resolver.applyBatch(
                TasksContract.AUTHORITY,
                arrayListOf(
                    ContentProviderOperation.newInsert(uri(Tasks.PATH))
                        .withValue(Tasks.TITLE, "first")
                        .build(),
                    ContentProviderOperation.newInsert(uri(TaskTags.PATH))
                        .withValueBackReference(TaskTags.TASK_ID, 0)
                        .withValue(TaskTags.TAG_ID, "no such tag")
                        .build(),
                ),
            )
        }

        assertEquals(0, query(Tasks.PATH).rows())
    }

    @Test
    fun expectedCountTurnsAStaleUpdateIntoARollback() {
        val id = newTask("original")

        assertThrows<OperationApplicationException> {
            resolver.applyBatch(
                TasksContract.AUTHORITY,
                arrayListOf(
                    ContentProviderOperation.newInsert(uri(Tasks.PATH))
                        .withValue(Tasks.TITLE, "second")
                        .build(),
                    ContentProviderOperation.newUpdate(staleUri(id))
                        .withValue(Tasks.TITLE, "renamed")
                        .withExpectedCount(1)
                        .build(),
                ),
            )
        }

        assertEquals(listOf("original"), query(Tasks.PATH).strings(Tasks.TITLE))
    }

    @Test
    fun aConditionalUpdateAloneDoesNotRollBack() {
        val id = newTask("original")

        val results = resolver.applyBatch(
            TasksContract.AUTHORITY,
            arrayListOf(
                ContentProviderOperation.newInsert(uri(Tasks.PATH))
                    .withValue(Tasks.TITLE, "second")
                    .build(),
                ContentProviderOperation.newUpdate(staleUri(id))
                    .withValue(Tasks.TITLE, "renamed")
                    .build(),
            ),
        )

        assertEquals(0, results[1].count)
        assertEquals(2, query(Tasks.PATH).rows())
    }

    private fun staleUri(id: Long) =
        "${itemUri(Tasks.PATH, id)}?${TasksContract.PARAM_IF_MODIFIED_AT}=1".toUri()

    @Test
    fun listWritesAreRejectedInABatch() = runBlockingTest {
        val list = newList("Groceries")
        val account = account().id
        val writes = listOf(
            ContentProviderOperation.newInsert(uri(Lists.PATH))
                .withValue(Lists.ACCOUNT_ID, account)
                .withValue(Lists.TITLE, "Batched")
                .build(),
            ContentProviderOperation.newUpdate(itemUri(Lists.PATH, list))
                .withValue(Lists.TITLE, "Renamed")
                .build(),
            ContentProviderOperation.newDelete(itemUri(Lists.PATH, list)).build(),
        )

        writes.forEach {
            assertThrows<IllegalArgumentException> {
                resolver.applyBatch(TasksContract.AUTHORITY, arrayListOf(it))
            }
        }
        assertEquals("Groceries", query(Lists.PATH, "?_id=$list").string(Lists.TITLE))
    }
}
