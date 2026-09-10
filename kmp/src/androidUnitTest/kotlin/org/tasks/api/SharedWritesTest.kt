package org.tasks.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.api.TasksContract.Reminders
import org.tasks.api.TasksContract.Lists
import org.tasks.api.TasksContract.Places
import org.tasks.api.TasksContract.Tags
import org.tasks.api.TasksContract.Tasks

class SharedWritesTest : ApiTestCase() {

    private fun rows(path: String, query: String = "") = query(path, query).rows()

    private fun task(id: Long): TaskRow? = runBlocking { engine.taskRow(id) }

    @Test
    fun deletingAMissingRowChangesNothing() {
        assertEquals(0, delete(Tasks.PATH, 9_999L))
        assertEquals(0, delete(Lists.PATH, 9_999L))
    }

    @Test
    fun deletingAListTakesItsTasksAndSparesEveryOther() = runBlockingTest {
        val other = newList("Elsewhere")
        val doomedTask = newTask("doomed")
        val keptTask = insert(Tasks.PATH, Tasks.TITLE to "kept", Tasks.LIST_ID to other)

        assertEquals(1, delete(Lists.PATH, listId))

        assertNull(task(doomedTask))
        assertEquals("kept", task(keptTask)!!.title)
    }

    @Test
    fun deletingATagUntagsTasksWithoutDeletingThem() {
        val id = newTask("tagged")
        val tag = insert(Tags.PATH, Tags.NAME to "errands")
        insert(TasksContract.TaskTags.PATH, TasksContract.TaskTags.TASK_ID to id, TasksContract.TaskTags.TAG_ID to tag)

        assertEquals(1, delete(Tags.PATH, tag))

        assertEquals(emptyList<Long>(), task(id)!!.tagIds)
    }

    @Test
    fun deletingAPlaceUnfilesTasksAndDropsOnlyItsLocationReminders() {
        val place = insert(Places.PATH, Places.LATITUDE to 1.0, Places.LONGITUDE to 2.0)
        val id = newTask("filed", Tasks.PLACE_ID to place)
        insert(Reminders.PATH, Reminders.TASK_ID to id, Reminders.TYPE to "location_arrival", Reminders.PLACE_ID to place)
        insert(Reminders.PATH, Reminders.TASK_ID to id, Reminders.TYPE to "date_time", Reminders.TRIGGER_AT to DAY)

        assertEquals(1, delete(Places.PATH, place))

        assertNull("the task survives, unfiled", task(id)!!.placeId)
        assertEquals(1, rows(Reminders.PATH, "?task_id=$id"))
    }

    @Test
    fun theLastOccurrenceOfASeriesCompletesForReal() {
        val id = newTask(
            "Water plants",
            Tasks.RECURRENCE to "FREQ=WEEKLY;COUNT=2",
            Tasks.DUE_DATE to DAY,
        )

        update(Tasks.PATH, id, Tasks.COMPLETED_AT to DAY)
        assertNull("still mid-series", task(id)!!.completed)

        update(Tasks.PATH, id, Tasks.COMPLETED_AT to DAY)
        assertTrue("series exhausted, so it sticks", (task(id)!!.completed ?: 0L) > 0L)
    }

    @Test
    fun tagsAreAddedAndRemovedAsADelta() = runBlockingTest {
        val one = newTask("one")
        val two = newTask("two")
        val errands = insert(Tags.PATH, Tags.NAME to "errands")
        val calls = insert(Tags.PATH, Tags.NAME to "calls")

        listOf(one, two).forEach {
            writer.editTaskTags(it, current = emptySet(), add = listOf(errands), remove = emptyList())
        }
        assertEquals(listOf(errands), task(one)!!.tagIds)
        assertEquals(listOf(errands), task(two)!!.tagIds)

        val again = writer.editTaskTags(one, setOf(errands), add = listOf(errands), remove = emptyList())
        assertEquals("adding a tag it already carries counts as nothing", 0, again.added)

        writer.editTaskTags(one, setOf(errands), add = listOf(calls), remove = listOf(errands))
        assertEquals(listOf(calls), task(one)!!.tagIds)
    }

    @Test
    fun aListCanBeRestyledWithoutTouchingItsName() = runBlockingTest {
        val id = newList("Renovation")

        writer.updateList(id, ListWrite(color = 123, icon = "construction").toValues())

        val row = runBlocking { engine.queryById(Lists.PATH, id).first().toListRow() }
        assertEquals("Renovation", row.title)
        assertEquals(123, row.color)
        assertEquals("construction", row.icon)
    }

    @Test
    fun aPlaceUpdateLeavesUnsentFieldsAlone() = runBlockingTest {
        val id = insert(
            Places.PATH,
            Places.LATITUDE to 1.0,
            Places.LONGITUDE to 2.0,
            Places.NAME to "Yard",
            Places.ADDRESS to "123 Main St",
        )

        writer.updatePlace(id, PlaceWrite(name = "Lumber yard", radius = 300).toValues())

        val row = runBlocking { engine.queryById(Places.PATH, id).first().toPlaceRow() }
        assertEquals("Lumber yard", row.name)
        assertEquals(300, row.radius)
        assertEquals("address was not sent", "123 Main St", row.address)
    }

    @Test
    fun aDeleteReportsWhatWentWithIt() = runBlockingTest {
        val place = insert(Places.PATH, Places.LATITUDE to 1.0, Places.LONGITUDE to 2.0)
        val tag = insert(Tags.PATH, Tags.NAME to "errands")
        val list = newList("Doomed")
        val parent = insert(Tasks.PATH, Tasks.TITLE to "parent", Tasks.LIST_ID to list)
        insert(Tasks.PATH, Tasks.TITLE to "child", Tasks.PARENT_ID to parent)
        val done = insert(
            Tasks.PATH,
            Tasks.TITLE to "done",
            Tasks.LIST_ID to list,
            Tasks.COMPLETED_AT to DAY,
        )
        insert(TasksContract.TaskTags.PATH, TasksContract.TaskTags.TASK_ID to done, TasksContract.TaskTags.TAG_ID to tag)
        insert(Tasks.PATH, Tasks.TITLE to "filed", Tasks.LIST_ID to list, Tasks.PLACE_ID to place)

        assertEquals(Deletion(1, 1), engine.deleteTask(writer, parent))
        assertEquals(Deletion(1, 1), engine.deleteTag(writer, tag))
        assertEquals(Deletion(1, 1), engine.deletePlace(writer, place))
        assertEquals("a completed task still goes", Deletion(1, 2), engine.deleteList(writer, list))
    }

    @Test
    fun deletingSomethingThatIsNotThereAffectsNothing() = runBlockingTest {
        assertEquals(Deletion(0, 0), engine.deleteTask(writer, 9_999L))
        assertEquals(Deletion(0, 0), engine.deleteList(writer, 9_999L))
    }

    @Test
    fun changingSomethingThatIsNotThereSaysSoRatherThanReportingNoChange() {
        assertThrows<ApiRowNotFound> {
            runBlocking { writer.changeTag(9_999L, TagWrite(name = "Gone")) }
        }
        assertThrows<ApiRowNotFound> {
            runBlocking { writer.changePlace(9_999L, PlaceWrite(name = "Gone")) }
        }
        val notFound = assertThrows<ApiRowNotFound> {
            runBlocking { writer.changeList(9_999L, ListWrite(title = "Gone")) }
        }

        assertEquals(Lists.PATH, notFound.path)
        assertEquals(9_999L, notFound.id)
    }

    @Test
    fun changingARowThatIsThereReportsTheChange() = runBlockingTest {
        val id = newList("Renovation")

        assertEquals(1, writer.changeList(id, ListWrite(title = "Kitchen")))

        val row = runBlocking { engine.queryById(Lists.PATH, id).first().toListRow() }
        assertEquals("Kitchen", row.title)
    }

    @Test
    fun remindersAreAddedAndRemovedInOneCall() = runBlockingTest {
        val id = newTask("Dentist", Tasks.DUE_DATE to DAY)
        val existing = insert(
            Reminders.PATH,
            Reminders.TASK_ID to id,
            Reminders.TYPE to Reminders.TYPE_DATE_TIME,
            Reminders.TRIGGER_AT to DAY,
        )

        val edit = engine.setTaskReminders(
            writer,
            taskId = id,
            add = listOf(ReminderWrite(type = Reminders.TYPE_RELATIVE_DUE, offsetMs = -HOUR)),
            removeReminderIds = listOf(existing, existing),
        )

        assertEquals(1, edit.addedIds.size)
        assertEquals("a repeated removal is collapsed", 1, edit.removed)
        assertEquals(listOf(Reminders.TYPE_RELATIVE_DUE), edit.reminders.map { it.type })
    }

    @Test
    fun aRejectedReminderAddLeavesTheExistingOnesAlone() = runBlockingTest {
        val id = newTask("Dentist", Tasks.DUE_DATE to DAY)
        val existing = insert(
            Reminders.PATH,
            Reminders.TASK_ID to id,
            Reminders.TYPE to Reminders.TYPE_DATE_TIME,
            Reminders.TRIGGER_AT to DAY,
        )

        assertThrows<IllegalArgumentException> {
            runBlocking {
                engine.setTaskReminders(
                    writer,
                    taskId = id,
                    add = listOf(
                        ReminderWrite(type = Reminders.TYPE_LOCATION_ARRIVAL, placeId = 9_999L)
                    ),
                    removeReminderIds = listOf(existing),
                )
            }
        }

        assertEquals(1, runBlocking { engine.findReminders(ReminderQuery(taskIds = listOf(id))) }.total)
    }

    @Test
    fun aBlankEnumFieldIsNotWritten() = runBlockingTest {
        val id = newTask("Pack tent", Tasks.PRIORITY to Tasks.PRIORITY_HIGH)
        val before = task(id)!!

        writer.updateTask(id, TaskWrite(priority = "", repeatFrom = "").toValues())

        assertEquals(before.priority, task(id)!!.priority)
        assertEquals(before.repeatFrom, task(id)!!.repeatFrom)
    }

    @Test
    fun deletedTasksAreInvisible() {
        val id = newTask("gone")

        delete(Tasks.PATH, id)

        assertNull(task(id))
    }

    @Test
    fun taskIdsReReadsAKnownSetAndCombinesWithOtherFilters() {
        val one = newTask("one", Tasks.PRIORITY to Tasks.PRIORITY_HIGH)
        val two = newTask("two")
        newTask("three")

        assertEquals(2, runBlocking { engine.findTasks(TaskQuery(ids = listOf(one, two))) }.total)
        assertEquals(
            listOf("one"),
            runBlocking {
                engine.findTasks(TaskQuery(ids = listOf(one, two), priorities = listOf("high")))
            }.rows.map { it.title },
        )
    }

    @Test
    fun parentIdsExpandALevelAndZeroMeansTopLevel() {
        val parent = newTask("parent")
        newTask("child", Tasks.PARENT_ID to parent)
        val other = newTask("other")

        assertEquals(
            listOf("child"),
            runBlocking { engine.findTasks(TaskQuery(parentIds = listOf(parent))) }.rows.map { it.title },
        )
        assertEquals(
            setOf("parent", "other"),
            runBlocking { engine.findTasks(TaskQuery(parentIds = listOf(0L))) }.rows.map { it.title }.toSet(),
        )
        assertEquals(other, task(other)!!.id)
    }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000

        const val HOUR = 60L * 60 * 1000
    }
}
