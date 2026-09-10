package org.tasks.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.api.TasksContract.Tags
import org.tasks.api.TasksContract.Tasks

class ApiBatchTest : ApiTestCase() {

    private fun task(id: Long): TaskRow? = runBlocking { engine.taskRow(id) }

    private fun total(): Int = runBlocking { engine.findTasks(TaskQuery(limit = 0)).total }

    @Test
    fun createTasksWritesEveryTaskInOneGo() = runBlockingTest {
        val creation = engine.createTasks(
            writer,
            listOf(
                TaskWrite(title = "Milk"),
                TaskWrite(title = "Eggs", priority = "high"),
                TaskWrite(title = "Bread"),
            ),
        )

        assertEquals(listOf("Milk", "Eggs", "Bread"), creation.ids.map { task(it)!!.title })
        assertEquals("high", task(creation.ids[1])!!.priority)
        assertEquals("the created rows come back in order", creation.ids, creation.tasks.map { it.id })
        assertEquals(emptyList<Long>(), creation.movedToParentListIds)
    }

    @Test
    fun aSubtaskLandsOnItsParentsListAndTheCreationSaysSo() = runBlockingTest {
        val elsewhere = newList("Elsewhere")
        val parent = newTask("parent")

        val creation = engine.createTasks(
            writer,
            listOf(
                TaskWrite(title = "child", parentId = parent, listId = elsewhere),
                TaskWrite(title = "top level", listId = elsewhere),
            ),
        )

        assertEquals(listOf(creation.ids[0]), creation.movedToParentListIds)
        assertEquals(task(parent)!!.listId, creation.tasks[0].listId)
        assertEquals(elsewhere, creation.tasks[1].listId)
    }

    @Test
    fun aRefusedCreateBatchWritesNothing() {
        assertThrows<IllegalArgumentException> {
            runBlocking {
                engine.createTasks(
                    writer,
                    listOf(
                        TaskWrite(title = "Fine"),
                        TaskWrite(title = "Doomed", parentId = 999_999L),
                    ),
                )
            }
        }

        assertEquals(0, total())
    }

    @Test
    fun aBatchAppliesEveryPatch() = runBlockingTest {
        val one = newTask("One")
        val two = newTask("Two")

        val revision = engine.updateTasks(
            writer,
            listOf(
                TaskUpdate(one, TaskWrite(title = "One enriched")),
                TaskUpdate(two, TaskWrite(notes = "context")),
            ),
        )

        assertEquals(listOf(1, 1), revision.rowsChanged)
        assertEquals("One enriched", task(one)!!.title)
        assertEquals("context", task(two)!!.notes)
        assertEquals(listOf("One enriched", "Two"), revision.tasks.map { it.title })
        assertEquals(emptyList<Long>(), revision.unchangedIds)
    }

    @Test
    fun reparentingOntoAnotherListsTaskMovesItAndTheRevisionSaysSo() = runBlockingTest {
        val elsewhere = newList("Elsewhere")
        val parent = insert(Tasks.PATH, Tasks.TITLE to "parent", Tasks.LIST_ID to elsewhere)
        val child = newTask("child")
        val loner = newTask("loner")

        val revision = engine.updateTasks(
            writer,
            listOf(
                TaskUpdate(child, TaskWrite(parentId = parent, listId = listId)),
                TaskUpdate(loner, TaskWrite(listId = elsewhere)),
            ),
        )

        assertEquals(listOf(child), revision.movedToParentListIds)
        assertEquals(elsewhere, task(child)!!.listId)
        assertEquals(elsewhere, task(loner)!!.listId)
    }

    @Test
    fun oneMissingEntryDoesNotHoldUpTheRest() = runBlockingTest {
        val fresh = newTask("Fresh")
        val gone = newTask("Gone")
        delete(Tasks.PATH, gone)

        val revision = engine.updateTasks(
            writer,
            listOf(
                TaskUpdate(fresh, TaskWrite(title = "Fresh enriched")),
                TaskUpdate(gone, TaskWrite(title = "never written")),
            ),
        )

        assertEquals(listOf(1, 0), revision.rowsChanged)
        assertEquals("Fresh enriched", task(fresh)!!.title)
        assertEquals(listOf(gone), revision.unchangedIds)
        assertEquals("only what applied is read back", listOf(fresh), revision.tasks.map { it.id })
    }

    @Test
    fun aBatchThatThrowsPartWayThroughLeavesNothingBehind() {
        val good = newTask("Good")
        val bad = newTask("Bad")

        assertThrows<IllegalArgumentException> {
            runBlocking {
                engine.updateTasks(
                    writer,
                    listOf(
                        TaskUpdate(good, TaskWrite(title = "Changed")),
                        TaskUpdate(bad, TaskWrite(parentId = 999_999L)),
                    ),
                )
            }
        }

        assertEquals("Good", task(good)!!.title)
    }

    @Test
    fun theSameTaskTwiceInAnUpdateBatchIsRefusedAndWritesNothing() {
        val id = newTask("One")

        val message = assertThrows<IllegalArgumentException> {
            runBlocking {
                engine.updateTasks(
                    writer,
                    listOf(
                        TaskUpdate(id, TaskWrite(title = "first")),
                        TaskUpdate(id, TaskWrite(notes = "second")),
                    ),
                )
            }
        }.message.orEmpty()

        assertTrue(message, message.contains("once per batch"))
        assertEquals("One", task(id)!!.title)
    }

    @Test
    fun aBatchWithNothingInItIsRefused() {
        val message = assertThrows<IllegalArgumentException> {
            runBlocking { engine.createTasks(writer, emptyList()) }
        }.message.orEmpty()

        assertTrue(message, message.contains("at least one entry"))
        assertThrows<IllegalArgumentException> {
            runBlocking { engine.updateTasks(writer, emptyList()) }
        }
        assertThrows<IllegalArgumentException> {
            runBlocking { engine.completeTasks(writer, emptyList(), completed = true) }
        }
    }

    @Test
    fun aPatchWithNothingToChangeIsRefusedAndTheRestOfTheBatchStaysPut() {
        val id = newTask("One")
        val before = task(id)!!.modified

        val message = assertThrows<IllegalArgumentException> {
            runBlocking {
                engine.updateTasks(
                    writer,
                    listOf(
                        TaskUpdate(id, TaskWrite(title = "Two")),
                        TaskUpdate(id + 1, TaskWrite()),
                    ),
                )
            }
        }.message.orEmpty()

        assertTrue(message, message.contains("no fields to change"))
        assertEquals("One", task(id)!!.title)
        assertEquals("an empty patch must not touch the row", before, task(id)!!.modified)
    }

    @Test
    fun aTagEditThatChangesNothingIsRefused() {
        val id = newTask("one")

        val message = assertThrows<IllegalArgumentException> {
            runBlocking { engine.setTaskTags(writer, listOf(id), emptyList(), emptyList()) }
        }.message.orEmpty()

        assertTrue(message, message.contains("Nothing to change"))
    }

    @Test
    fun aBatchIsCappedAndSaysSo() {
        val message = assertThrows<IllegalArgumentException> {
            runBlocking {
                engine.completeTasks(writer, (1L..MAX_BATCH + 1L).toList(), completed = true)
            }
        }.message.orEmpty()

        assertTrue(message, message.contains("at most $MAX_BATCH"))
    }

    @Test
    fun completingTheSameSeriesTwiceInOneBatchAdvancesItOnce() = runBlockingTest {
        val once = recurring()
        val twice = recurring()

        engine.completeTasks(writer, listOf(once), completed = true)
        val completion = engine.completeTasks(writer, listOf(twice, twice), completed = true)

        assertEquals("the repeated id is collapsed", listOf(twice), completion.taskIds)
        assertEquals(listOf(twice), completion.advancedTaskIds)
        assertNull("an advanced series comes back open", task(twice)!!.completed)
        assertEquals(task(once)!!.due, task(twice)!!.due)
        assertEquals(listOf(task(twice)), completion.tasks)
    }

    @Test
    fun completingATaskThatIsNotThereIsReportedNotHidden() = runBlockingTest {
        val id = newTask("here")

        val completion = engine.completeTasks(writer, listOf(id, 9_999L), completed = true)

        assertEquals(listOf(1, 0), completion.rowsChanged)
        assertEquals(listOf(9_999L), completion.unchangedIds)
        assertEquals(listOf(id), completion.tasks.map { it.id })
    }

    @Test
    fun tagsAreSetAcrossEveryTaskInOneCall() = runBlockingTest {
        val one = newTask("one")
        val two = newTask("two")
        val errands = insert(Tags.PATH, Tags.NAME to "errands")

        val change = engine.setTaskTags(
            writer,
            taskIds = listOf(one, two, one),
            add = listOf(errands, errands),
            remove = emptyList(),
        )

        assertEquals("the repeated task is collapsed", 2, change.edits.size)
        assertEquals("the repeated tag is added once", 1, change.edits.first().added)
        assertEquals(2, change.added)
        assertEquals(0, change.removed)
        assertEquals(listOf(one, two), change.tasks.map { it.id })
        assertEquals(listOf(errands), task(one)!!.tagIds)
        assertEquals(listOf(errands), task(two)!!.tagIds)
    }

    private fun recurring(): Long =
        newTask("Water plants", Tasks.RECURRENCE to "FREQ=WEEKLY", Tasks.DUE_DATE to DAY)

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
    }
}
