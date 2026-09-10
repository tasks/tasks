package org.tasks.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.api.TasksContract.Alarms
import org.tasks.api.TasksContract.Lists
import org.tasks.api.TasksContract.Places
import org.tasks.api.TasksContract.Tags
import org.tasks.api.TasksContract.Tasks

class ApiPagingTest : ApiTestCase() {

    private fun tags(query: TagQuery) = runBlocking { engine.findTags(query) }

    private fun lists(query: ListQuery) = runBlocking { engine.findLists(query) }

    private fun places(query: PlaceQuery) = runBlocking { engine.findPlaces(query) }

    private fun alarms(query: AlarmQuery) = runBlocking { engine.findAlarms(query) }

    private fun accounts(query: AccountQuery) = runBlocking { engine.findAccounts(query) }

    @Test
    fun aLimitOfZeroCountsWithoutCollectingAnything() {
        repeat(7) { insert(Tags.PATH, Tags.NAME to "tag $it") }

        val counted = tags(TagQuery(limit = 0))

        assertEquals(7, counted.total)
        assertTrue(counted.rows.isEmpty())
        assertTrue(counted.hasMore)
    }

    @Test
    fun aPageReportsWhereItStartsAndWhetherMoreRemain() {
        repeat(7) { insert(Tags.PATH, Tags.NAME to "tag $it") }

        val first = tags(TagQuery(limit = 3))
        val last = tags(TagQuery(limit = 3, offset = 6))

        assertEquals(3, first.rows.size)
        assertEquals(7, first.total)
        assertEquals(0, first.offset)
        assertTrue(first.hasMore)
        assertEquals(1, last.rows.size)
        assertEquals(6, last.offset)
        assertFalse(last.hasMore)
    }

    @Test
    fun anOmittedLimitIsTheDefaultAndAnOversizedOneIsCapped() {
        repeat(3) { insert(Tags.PATH, Tags.NAME to "tag $it") }

        assertEquals(TasksContract.DEFAULT_LIMIT, pageLimit(null))
        assertEquals(TasksContract.MAX_LIMIT, pageLimit(1_000_000))
        assertEquals(0, pageLimit(-1))
        assertEquals(0, pageOffset(-5))
        assertEquals(3, tags(TagQuery(limit = 1_000_000)).rows.size)
    }

    @Test
    fun everyCollectionFiltersById() = runBlockingTest {
        val tag = insert(Tags.PATH, Tags.NAME to "errands")
        insert(Tags.PATH, Tags.NAME to "calls")
        val place = insert(Places.PATH, Places.LATITUDE to 1.0, Places.LONGITUDE to 2.0)
        insert(Places.PATH, Places.LATITUDE to 3.0, Places.LONGITUDE to 4.0)
        val other = newList("Elsewhere")
        val accountId = account().id

        assertEquals(listOf(tag), tags(TagQuery(ids = listOf(tag))).rows.map { it.id })
        assertEquals(listOf(place), places(PlaceQuery(ids = listOf(place))).rows.map { it.id })
        assertEquals(listOf(other), lists(ListQuery(ids = listOf(other))).rows.map { it.id })
        assertEquals(
            listOf(accountId),
            accounts(AccountQuery(ids = listOf(accountId))).rows.map { it.id },
        )
    }

    @Test
    fun listsFilterByAccessAndAccount() = runBlockingTest {
        val shared = readOnlyList()
        val remote = remoteList()
        val remoteAccount = lists(ListQuery(ids = listOf(remote))).rows.single().accountId

        assertEquals(
            listOf(shared),
            lists(ListQuery(access = listOf(Lists.ACCESS_READ_ONLY))).rows.map { it.id },
        )
        assertEquals(
            listOf(remote),
            lists(ListQuery(accountIds = listOf(remoteAccount!!))).rows.map { it.id },
        )
    }

    @Test
    fun remindersFilterByTaskTypeAndPlace() = runBlockingTest {
        val place = insert(Places.PATH, Places.LATITUDE to 1.0, Places.LONGITUDE to 2.0)
        val filed = newTask("filed", Tasks.PLACE_ID to place)
        val timed = newTask("timed")
        insert(
            Alarms.PATH,
            Alarms.TASK_ID to timed,
            Alarms.TYPE to Alarms.TYPE_DATE_TIME,
            Alarms.TRIGGER_AT to DAY,
        )
        insert(
            Alarms.PATH,
            Alarms.TASK_ID to filed,
            Alarms.TYPE to Alarms.TYPE_LOCATION_ARRIVAL,
            Alarms.PLACE_ID to place,
        )

        assertEquals(2, alarms(AlarmQuery()).total)
        assertEquals(1, alarms(AlarmQuery(taskIds = listOf(timed))).total)
        assertEquals(1, alarms(AlarmQuery(types = listOf(Alarms.TYPE_LOCATION_ARRIVAL))).total)
        assertEquals(1, alarms(AlarmQuery(placeIds = listOf(place))).total)
    }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
    }
}
