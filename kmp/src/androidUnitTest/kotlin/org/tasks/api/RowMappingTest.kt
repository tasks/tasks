package org.tasks.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.UUIDHelper
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.api.TasksContract.Accounts
import org.tasks.api.TasksContract.Alarms
import org.tasks.api.TasksContract.Lists
import org.tasks.api.TasksContract.Places
import org.tasks.api.TasksContract.Tags
import org.tasks.api.TasksContract.TaskTags
import org.tasks.api.TasksContract.Tasks

class RowMappingTest : ApiTestCase() {

    private fun <T> one(path: String, id: Long, map: (ApiRow) -> T): T =
        runBlocking { engine.queryById(path, id).first().let(map) }

    private fun alarm(taskId: Long): AlarmRow =
        runBlocking { engine.query(Alarms.PATH, args(Alarms.PARAM_TASK to taskId.toString())) }
            .single()
            .toAlarmRow()

    private fun args(vararg pairs: Pair<String, String>) =
        ApiQueryArgs.build(TasksContract.paramsFor(Alarms.PATH)) {
            pairs.forEach { (key, value) -> put(key, value) }
        }

    @Test
    fun aTaskNamesItsRelatedRowsByIdAndLeavesUnsetFieldsNull() = runBlockingTest {
        val place = insert(Places.PATH, Places.LATITUDE to 1.0, Places.LONGITUDE to 2.0)
        val tag = insert(Tags.PATH, Tags.NAME to "errands")
        val parent = newTask("parent")
        val id = newTask(
            "Buy milk",
            Tasks.PRIORITY to Tasks.PRIORITY_HIGH,
            Tasks.PARENT_ID to parent,
            Tasks.PLACE_ID to place,
        )
        insert(TaskTags.PATH, TaskTags.TASK_ID to id, TaskTags.TAG_ID to tag)

        val row = one(Tasks.PATH, id) { it.toTaskRow() }

        assertEquals("Buy milk", row.title)
        assertEquals(Tasks.PRIORITY_HIGH, row.priority)
        assertEquals(listId, row.listId)
        assertEquals(parent, row.parentId)
        assertEquals(place, row.placeId)
        assertEquals(listOf(tag), row.tagIds)
        assertNull(row.due)
        assertNull(row.notes)
        assertNull(row.recurrence)
        assertNull("an open task has no completion time", row.completed)
        assertFalse(row.isReadOnly)
    }

    @Test
    fun aTaskDefaultsToNoPriorityRatherThanAnEmptyString() {
        val row = one(Tasks.PATH, newTask("Plain")) { it.toTaskRow() }

        assertEquals(Tasks.PRIORITY_NONE, row.priority)
    }

    @Test
    fun aTaskOnAReadOnlyListSaysSo() = runBlockingTest {
        val list = newList("Shared")
        val id = insert(Tasks.PATH, Tasks.TITLE to "Shared", Tasks.LIST_ID to list)

        caldavDao.update(
            caldavDao.getCalendarById(list)!!.copy(access = CaldavCalendar.ACCESS_READ_ONLY)
        )

        assertTrue(one(Tasks.PATH, id) { it.toTaskRow() }.isReadOnly)
    }

    @Test
    fun aRelativeReminderCarriesAnOffsetAndNoTrigger() {
        val id = newTask("Dentist", Tasks.DUE_DATE to DAY)
        insert(
            Alarms.PATH,
            Alarms.TASK_ID to id,
            Alarms.TYPE to Alarms.TYPE_RELATIVE_START,
            Alarms.OFFSET_MS to -HOUR,
        )

        val row = alarm(id)

        assertEquals(Alarms.TYPE_RELATIVE_START, row.type)
        assertEquals(-HOUR, row.offsetMs)
        assertNull(row.triggerAt)
        assertNull(row.placeId)
        assertTrue(row.isRelative)
    }

    @Test
    fun aTimeReminderCarriesATriggerAndNoOffset() {
        val id = newTask("Dentist")
        insert(
            Alarms.PATH,
            Alarms.TASK_ID to id,
            Alarms.TYPE to Alarms.TYPE_DATE_TIME,
            Alarms.TRIGGER_AT to DAY,
        )

        val row = alarm(id)

        assertEquals(DAY, row.triggerAt)
        assertNull(row.offsetMs)
        assertFalse(row.isRelative)
    }

    @Test
    fun aLocationReminderCarriesAPlaceAndNoTiming() = runBlockingTest {
        val place = insert(Places.PATH, Places.LATITUDE to 1.0, Places.LONGITUDE to 2.0)
        val id = newTask("Pick up parcel", Tasks.PLACE_ID to place)
        insert(
            Alarms.PATH,
            Alarms.TASK_ID to id,
            Alarms.TYPE to Alarms.TYPE_LOCATION_ARRIVAL,
            Alarms.PLACE_ID to place,
        )

        val row = alarm(id)

        assertEquals(Alarms.TYPE_LOCATION_ARRIVAL, row.type)
        assertEquals(place, row.placeId)
        assertNull(row.triggerAt)
        assertNull(row.offsetMs)
    }

    @Test
    fun anOffsetIsDescribedInWords() {
        assertEquals("1 hour before start", describeOffset(-HOUR, Alarms.TYPE_RELATIVE_START))
        assertEquals("2 hours after due", describeOffset(2 * HOUR, Alarms.TYPE_RELATIVE_DUE))
        assertEquals("1 day before due", describeOffset(-DAY, Alarms.TYPE_RELATIVE_DUE))
        assertEquals("15 minutes before due", describeOffset(-15 * MINUTE, Alarms.TYPE_RELATIVE_DUE))
        assertEquals("at due time", describeOffset(0, Alarms.TYPE_RELATIVE_DUE))
    }

    @Test
    fun aListCarriesItsAccountAndWhetherItCanBeWrittenTo() = runBlockingTest {
        val mine = one(Lists.PATH, listId) { it.toListRow() }
        val shared = one(Lists.PATH, readOnlyList()) { it.toListRow() }

        assertEquals("My tasks", mine.title)
        assertEquals(account().id, mine.accountId)
        assertEquals(Lists.ACCESS_OWNER, mine.access)
        assertFalse(mine.isReadOnly)
        assertTrue(shared.isReadOnly)
    }

    @Test
    fun anAccountWithNoNameReportsNullRatherThanEmpty() = runBlockingTest {
        caldavDao.insert(
            CaldavAccount(
                uuid = UUIDHelper.newUUID(),
                accountType = CaldavAccount.TYPE_LOCAL,
                name = "",
            )
        )

        val rows = runBlocking { engine.query(Accounts.PATH, ApiQueryArgs.build(TasksContract.paramsFor(Accounts.PATH)) {}) }
            .map { it.toAccountRow() }

        assertNull(rows.single { it.name != "Local" }.name)
    }

    @Test
    fun aPlaceKeepsItsNameSeparateFromTheLabelItFallsBackTo() {
        val unnamed = insert(Places.PATH, Places.LATITUDE to 41.82, Places.LONGITUDE to -88.19)
        val named = insert(
            Places.PATH,
            Places.LATITUDE to 1.0,
            Places.LONGITUDE to 2.0,
            Places.NAME to "Trailhead",
        )

        assertNull(one(Places.PATH, unnamed) { it.toPlaceRow() }.name)
        assertTrue(one(Places.PATH, unnamed) { it.toPlaceRow() }.displayName.isNotEmpty())
        assertEquals("Trailhead", one(Places.PATH, named) { it.toPlaceRow() }.name)
    }

    @Test
    fun aTagKeepsAnUnsetColourNullRatherThanZero() {
        val plain = insert(Tags.PATH, Tags.NAME to "errands")
        val coloured = insert(Tags.PATH, Tags.NAME to "calls", Tags.COLOR to 123)

        assertNull(one(Tags.PATH, plain) { it.toTagRow() }.color)
        assertEquals(123, one(Tags.PATH, coloured) { it.toTagRow() }.color)
    }

    @Test
    fun onlyZeroMeansAnUnsetColumn() {
        val row = ApiRow(mapOf("unset" to 0, "negative" to 1), arrayOf(0L, -1L))

        assertNull(row.longOrNull("unset"))
        assertEquals(-1L, row.longOrNull("negative"))
    }

    private companion object {
        const val MINUTE = 60L * 1000
        const val HOUR = 60 * MINUTE
        const val DAY = 24 * HOUR
    }
}
