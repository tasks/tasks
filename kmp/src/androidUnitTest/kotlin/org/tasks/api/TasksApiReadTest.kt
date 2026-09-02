package org.tasks.api

import android.content.ContentResolver
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.api.TasksContract.Accounts
import org.tasks.api.TasksContract.Lists
import org.tasks.api.TasksContract.Tasks
import org.tasks.time.DateTimeUtils2.currentTimeMillis

class TasksApiReadTest : ApiTestCase() {
    @Test
    fun insertedTaskComesBack() {
        val id = newTask("Renew passport")

        assertEquals("Renew passport", query(Tasks.PATH).string(Tasks.TITLE))
        assertEquals(id, query(Tasks.PATH).long(TasksContract.ID))
    }

    @Test
    fun itemUriReturnsOneRow() {
        val id = newTask("one")
        newTask("two")

        val cursor = resolver.query(itemUri(Tasks.PATH, id), null, null, null, null)!!
        assertEquals("one", cursor.string(Tasks.TITLE))
    }

    @Test
    fun missingItemIsAnEmptyCursor() {
        val cursor = resolver.query(itemUri(Tasks.PATH, 9999), null, null, null, null)!!
        assertEquals(0, cursor.rows())
    }

    @Test
    fun deletedTaskIsInvisibleEverywhere() {
        val id = newTask("gone")
        insert(
            TasksContract.Alarms.PATH,
            TasksContract.Alarms.TASK_ID to id,
            TasksContract.Alarms.TYPE to TasksContract.Alarms.TYPE_RELATIVE_DUE,
            TasksContract.Alarms.OFFSET_MS to -1000L,
        )

        assertEquals(1, delete(Tasks.PATH, id))

        assertEquals(0, query(Tasks.PATH).rows())
        assertEquals(0, query(Tasks.PATH, "?limit=0").total())
        assertEquals(0, resolver.query(itemUri(Tasks.PATH, id), null, null, null, null)!!.rows())
        assertEquals(0, query(TasksContract.Alarms.PATH).rows())
    }

    @Test
    fun limitDefaultsToOneHundred() {
        repeat(105) { newTask("task $it") }

        assertEquals(100, query(Tasks.PATH).rows())
        assertEquals(105, query(Tasks.PATH).total())
    }

    @Test
    fun limitZeroCountsWithoutFetching() {
        repeat(7) { newTask("task $it") }

        val cursor = query(Tasks.PATH, "?limit=0")
        assertEquals(7, cursor.extras.getInt(ContentResolver.EXTRA_TOTAL_COUNT, -1))
        assertEquals(0, cursor.count)
        cursor.close()
    }

    @Test
    fun totalCountIgnoresLimitAndOffset() {
        repeat(10) { newTask("task $it") }

        assertEquals(10, query(Tasks.PATH, "?limit=3&offset=4").total())
        assertEquals(3, query(Tasks.PATH, "?limit=3&offset=4").rows())
    }

    @Test
    fun offsetPastTheEndReportsTheRealTotal() {
        repeat(3) { newTask("task $it") }

        val cursor = query(Tasks.PATH, "?limit=5&offset=10")
        assertEquals(0, cursor.count)
        assertEquals(3, cursor.extras.getInt(ContentResolver.EXTRA_TOTAL_COUNT, -1))
        cursor.close()
    }

    @Test
    fun pagingNeverSkipsOrRepeats() {
        val ids = (0 until 20).map { newTask("same") }

        val paged = (0 until 20 step 6).flatMap { offset ->
            query(Tasks.PATH, "?sort=due&limit=6&offset=$offset").longs(TasksContract.ID)
        }
        assertEquals(ids, paged)
    }

    @Test
    fun itemUriSetsNoTotalCount() {
        val id = newTask()
        val cursor = resolver.query(itemUri(Tasks.PATH, id), null, null, null, null)!!
        assertEquals(-1, cursor.extras.getInt(ContentResolver.EXTRA_TOTAL_COUNT, -1))
        cursor.close()
    }

    @Test
    fun completionIsExpressedWithDates() {
        val done = newTask("done")
        newTask("todo")
        update(Tasks.PATH, done, Tasks.COMPLETED_AT to currentTimeMillis())

        assertEquals(listOf("todo"), query(Tasks.PATH, "?completed_before=1").strings(Tasks.TITLE))
        assertEquals(listOf("done"), query(Tasks.PATH, "?completed_after=0").strings(Tasks.TITLE))
    }

    @Test
    fun rangesAreExclusive() {
        val now = currentTimeMillis()
        newTask("scheduled", Tasks.DUE_DATE to now)
        newTask("unscheduled")

        assertEquals(listOf("scheduled"), query(Tasks.PATH, "?due_after=0").strings(Tasks.TITLE))
        assertEquals(emptyList<String>(), query(Tasks.PATH, "?due_after=$now").strings(Tasks.TITLE))
        assertEquals(
            listOf("scheduled", "unscheduled"),
            query(Tasks.PATH, "?due_before=${now + 1}").strings(Tasks.TITLE),
        )
    }

    @Test
    fun repeatableParametersOrWithinThemselves() {
        val a = newTask("a")
        val b = newTask("b")
        newTask("c")

        assertEquals(listOf("a", "b"), query(Tasks.PATH, "?_id=$a&_id=$b").strings(Tasks.TITLE))
    }

    @Test
    fun parentZeroMeansTopLevel() {
        val parent = newTask("parent")
        newTask("child", Tasks.PARENT_ID to parent)

        assertEquals(listOf("parent"), query(Tasks.PATH, "?parent_id=0").strings(Tasks.TITLE))
        assertEquals(listOf("child"), query(Tasks.PATH, "?parent_id=$parent").strings(Tasks.TITLE))
    }

    @Test
    fun searchMatchesTitleAndNotesOnly() {
        newTask("shopping", Tasks.NOTES to "milk")
        newTask("other", Tasks.NOTES to "nothing")

        assertEquals(listOf("shopping"), query(Tasks.PATH, "?search=shop").strings(Tasks.TITLE))
        assertEquals(listOf("shopping"), query(Tasks.PATH, "?search=MILK").strings(Tasks.TITLE))
    }

    @Test
    fun searchTreatsWildcardsLiterally() {
        newTask("100% done")
        newTask("nothing here")

        assertEquals(listOf("100% done"), query(Tasks.PATH, "?search=100%25").strings(Tasks.TITLE))
        assertEquals(emptyList<String>(), query(Tasks.PATH, "?search=%25zzz%25").strings(Tasks.TITLE))
    }

    @Test
    fun priorityIsAStringEnum() {
        newTask("urgent", Tasks.PRIORITY to Tasks.PRIORITY_HIGH)
        newTask("whenever", Tasks.PRIORITY to Tasks.PRIORITY_LOW)

        assertEquals(Tasks.PRIORITY_HIGH, query(Tasks.PATH, "?priority=high").string(Tasks.PRIORITY))
        assertEquals(
            listOf("urgent", "whenever"),
            query(Tasks.PATH, "?priority=high&priority=low&sort=priority").strings(Tasks.TITLE),
        )
    }

    @Test
    fun sortDescReversesAndStaysTotal() {
        val a = newTask("a", Tasks.DUE_DATE to day(3))
        val b = newTask("b", Tasks.DUE_DATE to day(1))
        val c = newTask("c", Tasks.DUE_DATE to day(2))

        assertEquals(listOf(b, c, a), query(Tasks.PATH, "?sort=due").longs(TasksContract.ID))
        assertEquals(listOf(a, c, b), query(Tasks.PATH, "?sort=due&sort_desc=1").longs(TasksContract.ID))
    }

    @Test
    fun theStandardSortArgsAreHonored() {
        val a = newTask("a", Tasks.DUE_DATE to day(3))
        val b = newTask("b", Tasks.DUE_DATE to day(1))
        val c = newTask("c", Tasks.DUE_DATE to day(2))
        val args = Bundle().apply {
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(Tasks.SORT_DUE))
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
        }

        resolver.query(uri(Tasks.PATH), null, args, null)!!.use { cursor ->
            val index = cursor.getColumnIndexOrThrow(TasksContract.ID)
            val ids = buildList { while (cursor.moveToNext()) add(cursor.getLong(index)) }
            assertEquals(listOf(a, c, b), ids)
        }
    }

    @Test
    fun completedFiltersBothWays() {
        val open = newTask("open")
        val done = newTask("done", Tasks.COMPLETED_AT to currentTimeMillis())

        assertEquals(listOf(open), query(Tasks.PATH, "?completed=0").longs(TasksContract.ID))
        assertEquals(listOf(done), query(Tasks.PATH, "?completed=1").longs(TasksContract.ID))
        assertEquals(2, query(Tasks.PATH).rows())
        assertThrows<IllegalArgumentException> { query(Tasks.PATH, "?completed=yes") }
    }

    @Test
    fun listFilterUsesTheListId() {
        newTask("here")

        assertEquals(listOf("here"), query(Tasks.PATH, "?list_id=$listId").strings(Tasks.TITLE))
        assertEquals(emptyList<String>(), query(Tasks.PATH, "?list_id=9999").strings(Tasks.TITLE))
    }

    @Test
    fun aTaskRowCarriesItsOwnRelationships() {
        insert(TasksContract.Tags.PATH, TasksContract.Tags.NAME to "admin")
        insert(TasksContract.Tags.PATH, TasksContract.Tags.NAME to "urgent")
        val tagIds = query(TasksContract.Tags.PATH).longs(TasksContract.ID)
        insert(
            TasksContract.Places.PATH,
            TasksContract.Places.NAME to "Home",
            TasksContract.Places.LATITUDE to 51.5,
            TasksContract.Places.LONGITUDE to -0.1,
        )
        val placeId = query(TasksContract.Places.PATH).long(TasksContract.ID)

        val parent = newTask("parent")
        tagIds.forEach {
            insert(
                TasksContract.TaskTags.PATH,
                TasksContract.TaskTags.TASK_ID to parent,
                TasksContract.TaskTags.TAG_ID to it,
            )
        }
        newTask("done child", Tasks.PARENT_ID to parent, Tasks.COMPLETED_AT to currentTimeMillis())
        newTask("open child", Tasks.PARENT_ID to parent)
        insert(
            TasksContract.Alarms.PATH,
            TasksContract.Alarms.TASK_ID to parent,
            TasksContract.Alarms.TYPE to TasksContract.Alarms.TYPE_LOCATION_ARRIVAL,
            TasksContract.Alarms.PLACE_ID to placeId,
        )

        resolver.query(itemUri(Tasks.PATH, parent), null, null, null, null)!!.use {
            assertTrue(it.moveToFirst())
            assertEquals(
                tagIds.sorted(),
                it.getString(it.getColumnIndexOrThrow(Tasks.TAG_IDS))
                    .split(",").map(String::toLong).sorted(),
            )
            assertEquals(placeId, it.getLong(it.getColumnIndexOrThrow(Tasks.PLACE_ID)))
            assertEquals(2, it.getInt(it.getColumnIndexOrThrow(Tasks.CHILD_COUNT)))
            assertEquals(1, it.getInt(it.getColumnIndexOrThrow(Tasks.UNCOMPLETED_CHILD_COUNT)))
            assertEquals(listId, it.getLong(it.getColumnIndexOrThrow(Tasks.LIST_ID)))
        }
    }

    @Test
    fun aTaskWithNoRelationshipsCarriesEmptyValues() {
        newTask("bare")

        query(Tasks.PATH).use {
            assertTrue(it.moveToFirst())
            assertEquals("", it.getString(it.getColumnIndexOrThrow(Tasks.TAG_IDS)))
            assertEquals(0L, it.getLong(it.getColumnIndexOrThrow(Tasks.PLACE_ID)))
            assertEquals(0, it.getInt(it.getColumnIndexOrThrow(Tasks.CHILD_COUNT)))
        }
    }

    @Test
    fun childCountsFollowTheParentFilter() {
        val parent = newTask("parent")
        newTask("child", Tasks.PARENT_ID to parent)
        val grandchild = newTask("grandchild")
        update(Tasks.PATH, grandchild, Tasks.PARENT_ID to query(Tasks.PATH, "?parent_id=$parent").long(TasksContract.ID))

        assertEquals(1, query(Tasks.PATH, "?_id=$parent").int(Tasks.CHILD_COUNT))
        assertEquals(1, query(Tasks.PATH, "?parent_id=$parent").rows())
    }

    @Test
    fun noColumnIsEverNull() {
        newTask("bare")

        query(Tasks.PATH).use { cursor ->
            assertTrue(cursor.moveToFirst())
            (0 until cursor.columnCount).forEach {
                assertFalse(cursor.getColumnName(it), cursor.isNull(it))
            }
        }
        query(Lists.PATH).use { cursor ->
            assertTrue(cursor.moveToFirst())
            (0 until cursor.columnCount).forEach {
                assertFalse(cursor.getColumnName(it), cursor.isNull(it))
            }
        }
        query(Accounts.PATH).use { cursor ->
            assertTrue(cursor.moveToFirst())
            (0 until cursor.columnCount).forEach {
                assertFalse(cursor.getColumnName(it), cursor.isNull(it))
            }
        }
    }

    @Test
    fun everyContractColumnIsServed() {
        newTask("x")
        TasksContract.COLLECTIONS.forEach { path ->
            query(path, "?limit=0").use { cursor ->
                assertEquals(path, TasksContract.columnsFor(path), cursor.columnNames.toList())
            }
        }
    }

    @Test
    fun projectionSelectsColumnsByName() {
        newTask("projected")

        query(Tasks.PATH, projection = arrayOf(Tasks.TITLE, TasksContract.ID)).use { cursor ->
            assertEquals(listOf(Tasks.TITLE, TasksContract.ID), cursor.columnNames.toList())
            assertTrue(cursor.moveToFirst())
            assertEquals("projected", cursor.getString(0))
        }
    }

    @Test
    fun queryArgsBundleIsEquivalentToUriParameters() {
        repeat(5) { newTask("task $it") }

        val args = Bundle().apply {
            putInt(ContentResolver.QUERY_ARG_LIMIT, 2)
            putInt(ContentResolver.QUERY_ARG_OFFSET, 1)
        }
        resolver.query(uri(Tasks.PATH), null, args, null)!!.use {
            assertEquals(2, it.count)
            assertEquals(5, it.extras.getInt(ContentResolver.EXTRA_TOTAL_COUNT))
        }
    }

    @Test
    fun accountsAreReadable() {
        query(Accounts.PATH).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(Accounts.TYPE_LOCAL, cursor.getString(cursor.getColumnIndexOrThrow(Accounts.TYPE)))
        }
    }

    @Test
    fun anAccountSaysWhetherItsServerAdvancesRecurringTasks() {
        assertEquals(0, query(Accounts.PATH).int(Accounts.REPEATS_ON_SERVER))
    }

    @Test
    fun accountsAreAddressableByRowId() {
        val id = query(Accounts.PATH).long(TasksContract.ID)

        resolver.query(itemUri(Accounts.PATH, id), null, null, null, null)!!.use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.count)
            assertEquals(Accounts.TYPE_LOCAL, it.getString(it.getColumnIndexOrThrow(Accounts.TYPE)))
        }
    }

    @Test
    fun listsCarryTheirAccessLevel() {
        val readOnly = runBlockingTest { readOnlyList() }

        assertEquals(
            listOf(Lists.ACCESS_OWNER),
            query(Lists.PATH, "?_id=$listId").strings(Lists.ACCESS),
        )
        assertEquals(
            listOf(Lists.ACCESS_READ_ONLY),
            query(Lists.PATH, "?access=read_only").strings(Lists.ACCESS),
        )
        assertEquals(
            listOf(readOnly),
            query(Lists.PATH, "?access=read_only").longs(TasksContract.ID),
        )
    }

    @Test
    fun mimeTypesAreReal() {
        assertEquals(Tasks.TYPE_DIR, resolver.getType(uri(Tasks.PATH)))
        assertEquals(Tasks.TYPE_ITEM, resolver.getType(itemUri(Tasks.PATH, 1)))
        assertEquals(Lists.TYPE_DIR, resolver.getType(uri(Lists.PATH)))
    }

    @Test
    fun observersFireForCollections() {
        val observer = CountingObserver()
        resolver.registerContentObserver(uri(Tasks.PATH), true, observer)
        try {
            newTask("wakes the observer")
            assertTrue(observer.await())
            assertFalse(observer.selfChange)
        } finally {
            resolver.unregisterContentObserver(observer)
        }
    }

    @Test
    fun aTagWriteWakesTaskObservers() {
        val task = newTask("tagged")
        insert(TasksContract.Tags.PATH, TasksContract.Tags.NAME to "admin")
        val tagId = query(TasksContract.Tags.PATH).long(TasksContract.ID)

        val observer = CountingObserver()
        resolver.registerContentObserver(uri(Tasks.PATH), false, observer)
        try {
            insert(
                TasksContract.TaskTags.PATH,
                TasksContract.TaskTags.TASK_ID to task,
                TasksContract.TaskTags.TAG_ID to tagId,
            )
            assertTrue(observer.await())
        } finally {
            resolver.unregisterContentObserver(observer)
        }
        assertEquals(listOf("tagged"), query(Tasks.PATH, "?tag_id=$tagId").strings(Tasks.TITLE))
    }

    @Test
    fun aSyncErrorReadsBackAsACoarseCode() {
        assertEquals("", query(Accounts.PATH).string(Accounts.ERROR))

        setAccountError("HTTP 401 Unauthorized at https://caldav.example.com/dav/u/alex")
        assertEquals(
            Accounts.ERROR_UNAUTHORIZED,
            query(Accounts.PATH).string(Accounts.ERROR),
        )

        setAccountError("HTTP 402 Payment Required")
        assertEquals(
            Accounts.ERROR_PAYMENT_REQUIRED,
            query(Accounts.PATH).string(Accounts.ERROR),
        )

        setAccountError("HTTP 451")
        assertEquals(
            Accounts.ERROR_TERMS_REQUIRED,
            query(Accounts.PATH).string(Accounts.ERROR),
        )

        setAccountError("Unable to resolve host caldav.example.com: no address associated")
        assertEquals(Accounts.ERROR_FAILED, query(Accounts.PATH).string(Accounts.ERROR))

        setAccountError("")
        assertEquals("", query(Accounts.PATH).string(Accounts.ERROR))
    }

}
