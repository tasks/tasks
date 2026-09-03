package org.tasks.api

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import androidx.core.content.contentValuesOf
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.api.TasksContract.Alarms
import org.tasks.api.TasksContract.TaskTags
import org.tasks.api.TasksContract.Tasks
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.getLocalList
import org.tasks.preferences.Preferences
import org.tasks.injection.InjectingTestCase
import javax.inject.Inject

@HiltAndroidTest
class TasksApiIntegrationTest : InjectingTestCase() {
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var preferences: Preferences

    private lateinit var resolver: ContentResolver
    private var listId: Long = 0

    @Before
    override fun setUp() {
        super.setUp()
        resolver = getApplicationContext<android.content.Context>().contentResolver
        listId = runBlocking { caldavDao.getLocalList().let { caldavDao.getCalendarByUuid(it.uuid!!)!!.id } }
    }

    @Test
    fun theProviderIsReachableThroughTheRealGraph() {
        val id = newTask("Renew passport")

        assertEquals(Tasks.TYPE_DIR, resolver.getType(uri(Tasks.PATH)))
        assertEquals("Renew passport", query(Tasks.PATH).string(Tasks.TITLE))
        assertEquals(listId, query(Tasks.PATH).long(Tasks.LIST_ID))
        assertEquals(1, resolver.delete(itemUri(Tasks.PATH, id), null, null))
    }

    @Test
    fun titleIsStoredVerbatim() {
        newTask("Buy milk tomorrow #errand")

        assertEquals("Buy milk tomorrow #errand", query(Tasks.PATH).string(Tasks.TITLE))
        assertEquals(0, query(TaskTags.PATH).rows())
    }

    @Test
    fun aNewTaskDoesNotInheritTheUsersDefaultReminders() {
        runBlocking { preferences.setDefaultAlarms(listOf(Alarm.whenDue(0))) }

        val id = newTask(
            "Renew passport",
            Tasks.DUE_DATE to 1_700_000_000_000L,
            Tasks.DUE_ALL_DAY to 0,
        )

        assertEquals(
            emptyList<String>(),
            query(Alarms.PATH, "?task_id=$id").strings(Alarms.TYPE),
        )
    }

    @Test
    fun aReadOnlyListRefusesWrites() {
        val readOnly = runBlocking {
            val calendar = CaldavCalendar(
                uuid = org.tasks.data.UUIDHelper.newUUID(),
                account = caldavDao.getLocalList().account,
                name = "Shared",
                access = CaldavCalendar.ACCESS_READ_ONLY,
            )
            caldavDao.insert(calendar)
            caldavDao.getCalendarByUuid(calendar.uuid!!)!!.id
        }

        assertThrows<UnsupportedOperationException> {
            resolver.insert(
                uri(Tasks.PATH),
                contentValuesOf(Tasks.TITLE to "nope", Tasks.LIST_ID to readOnly),
            )
        }
    }

    @Test
    fun aBatchAppliesAtomicallyThroughTheRealGraph() {
        val results = resolver.applyBatch(
            TasksContract.AUTHORITY,
            arrayListOf(
                ContentProviderOperation.newInsert(uri(Tasks.PATH))
                    .withValue(Tasks.TITLE, "Renew passport")
                    .withValue(Tasks.LIST_ID, listId)
                    .build(),
                ContentProviderOperation.newInsert(uri(Alarms.PATH))
                    .withValueBackReference(Alarms.TASK_ID, 0)
                    .withValue(Alarms.TYPE, Alarms.TYPE_RELATIVE_DUE)
                    .withValue(Alarms.OFFSET_MS, -1000L)
                    .build(),
            ),
        )

        val taskId = ContentUris.parseId(results[0].uri!!)
        assertEquals("Renew passport", query(Tasks.PATH).string(Tasks.TITLE))
        resolver.query(results[1].uri!!, null, null, null, null)!!.use {
            assertTrue(it.moveToFirst())
            assertEquals(taskId, it.getLong(it.getColumnIndexOrThrow(Alarms.TASK_ID)))
        }
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
    fun theBaseUriIsOneSignalForEverything() {
        val observer = CountingObserver()
        resolver.registerContentObserver(TasksContract.CONTENT_URI.toUri(), true, observer)
        try {
            newTask("wakes an observer on the base URI")
            assertTrue(observer.await())
        } finally {
            resolver.unregisterContentObserver(observer)
        }
    }

    private fun uri(path: String, query: String = ""): Uri =
        "${TasksContract.CONTENT_URI}/$path$query".toUri()

    private fun itemUri(path: String, id: Long): Uri = ContentUris.withAppendedId(uri(path), id)

    private fun query(path: String, query: String = ""): Cursor =
        resolver.query(uri(path, query), null, null, null, null)!!

    private fun newTask(title: String, vararg values: Pair<String, Any?>): Long =
        ContentUris.parseId(
            resolver.insert(uri(Tasks.PATH), contentValuesOf(Tasks.TITLE to title, *values))!!
        )

    private fun Cursor.string(column: String): String =
        use { it.moveToFirst(); it.getString(it.getColumnIndexOrThrow(column)) }

    private fun Cursor.long(column: String): Long =
        use { it.moveToFirst(); it.getLong(it.getColumnIndexOrThrow(column)) }

    private fun Cursor.rows(): Int = use { it.count }

    private fun Cursor.strings(column: String): List<String> = use { c ->
        val index = c.getColumnIndexOrThrow(column)
        buildList { while (c.moveToNext()) add(c.getString(index)) }
    }

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            if (t is T) return
            throw AssertionError("Expected ${T::class.java.simpleName} but was $t", t)
        }
        org.junit.Assert.fail("Expected ${T::class.java.simpleName}")
    }
}
