package org.tasks.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.api.TasksContract.Tasks

class TaskQueryTest : ApiTestCase() {

    private fun find(query: TaskQuery): ApiPage<TaskRow> = runBlocking { engine.findTasks(query) }

    private fun titles(query: TaskQuery): List<String> = find(query).rows.map { it.title }

    private fun message(block: () -> Unit): String =
        runCatching(block).exceptionOrNull()?.message.orEmpty()

    @Test
    fun openIsTheDefault() {
        newTask("Open")
        newTask("Done", Tasks.COMPLETED_AT to DAY)

        assertEquals(listOf("Open"), titles(TaskQuery()))
        assertEquals(listOf("Done"), titles(TaskQuery(status = "completed")))
        assertEquals(2, find(TaskQuery(status = "any")).total)
    }

    @Test
    fun anUnknownStatusNamesTheLegalValues() {
        val message = message { TaskQuery(status = "pending") }

        assertTrue(message, message.contains("open|completed|any"))
    }

    @Test
    fun anUnknownDueFilterNamesTheLegalValues() {
        val message = message { TaskQuery(due = "tomorow") }

        assertTrue(message, message.contains(DUE_FILTERS.joinToString("|")))
    }

    @Test
    fun anUnusablePatternSaysSoRatherThanThrowingRaw() {
        val message = message { TaskQuery(matches = "[unclosed") }

        assertTrue(message, message.contains("'matches' is not a valid regular expression"))
    }

    @Test
    fun aBlankArgumentMeansTheSameAsAnAbsentOne() {
        newTask("Open")
        newTask("Done", Tasks.COMPLETED_AT to DAY)

        assertEquals(listOf("Open"), titles(TaskQuery(status = "", due = "", sort = "", matches = "")))
    }

    @Test
    fun aDueBeforeBoundOnItsOwnSkipsUnscheduledTasks() {
        newTask("Someday")
        newTask("Due", Tasks.DUE_DATE to DAY)

        assertEquals(listOf("Due"), titles(TaskQuery(dueBefore = DAY * 2)))
    }

    @Test
    fun theNoDueDateFilterStillFindsUnscheduledTasks() {
        newTask("Someday")
        newTask("Due", Tasks.DUE_DATE to DAY)

        assertEquals(listOf("Someday"), titles(TaskQuery(due = "no_due_date")))
        assertEquals(listOf("Due"), titles(TaskQuery(due = "has_due_date")))
    }

    @Test
    fun anExplicitBoundWinsOverTheConvenienceFilter() {
        newTask("Due", Tasks.DUE_DATE to DAY)

        assertEquals(listOf("Due"), titles(TaskQuery(due = "no_due_date", dueBefore = DAY * 2)))
    }

    @Test
    fun tagIdsNarrowsToTasksCarryingThoseTags() {
        val tagged = newTask("Errand")
        newTask("Untagged")
        val errands = insert(TasksContract.Tags.PATH, TasksContract.Tags.NAME to "errands")
        insert(
            TasksContract.TaskTags.PATH,
            TasksContract.TaskTags.TASK_ID to tagged,
            TasksContract.TaskTags.TAG_ID to errands,
        )

        assertEquals(listOf("Errand"), titles(TaskQuery(tagIds = listOf(errands))))
        assertEquals(1, runBlocking { engine.countTasks(TaskQuery(tagIds = listOf(errands))) })
    }

    @Test
    fun filteringOnCompletionDatesImpliesCompletedTasks() {
        newTask("Open")
        newTask("Done", Tasks.COMPLETED_AT to DAY)

        assertEquals(listOf("Done"), titles(TaskQuery(completedBefore = DAY * 2)))
    }

    @Test
    fun matchesTestsTitleAndNotesByDefault() {
        newTask("Pack tent")
        newTask("Buy firewood", Tasks.NOTES to "from the camp store")

        assertEquals(listOf("Buy firewood"), titles(TaskQuery(matches = "camp store")))
    }

    @Test
    fun matchFieldsNarrowsWhichTextIsTested() {
        newTask("Buy firewood", Tasks.NOTES to "from the camp store")

        assertEquals(
            emptyList<String>(),
            titles(TaskQuery(matches = "camp store", matchFields = listOf("title"))),
        )
    }

    @Test
    fun matchingIgnoresCaseUnlessToldOtherwise() {
        newTask("Renew Passport")

        assertEquals(1, find(TaskQuery(matches = "passport")).total)
        assertEquals(0, find(TaskQuery(matches = "passport", matchCase = true)).total)
    }

    @Test
    fun aPatternIsUnanchoredUnlessYouAnchorIt() {
        newTask("Pack tent")
        newTask("Repack tent")

        assertEquals(2, find(TaskQuery(matches = "Pack tent")).total)
        assertEquals(listOf("Pack tent"), titles(TaskQuery(matches = "^Pack tent$")))
    }

    @Test
    fun aScannedPageStillReportsTheTrueTotal() {
        repeat(3) { newTask("Task $it") }

        val page = find(TaskQuery(matches = "Task", limit = 1))

        assertEquals(1, page.rows.size)
        assertEquals(3, page.total)
        assertTrue(page.hasMore)
    }

    @Test
    fun limitZeroCountsWithoutCollectingAnything() {
        repeat(3) { newTask("Task $it") }

        val page = find(TaskQuery(limit = 0))

        assertEquals(3, page.total)
        assertTrue(page.rows.isEmpty())
    }

    @Test
    fun theLastPageReportsNoMore() {
        repeat(3) { newTask("Task $it") }

        assertFalse(find(TaskQuery(limit = 2, offset = 2)).hasMore)
    }

    @Test
    fun aSubtaskThatLandedOnItsParentsListIsReported() {
        assertTrue(movedToParentList(parentId = 5L, listId = 2L, landedOn = 3L))
        assertFalse(movedToParentList(parentId = 5L, listId = 2L, landedOn = 2L))
        assertFalse(movedToParentList(parentId = 0L, listId = 2L, landedOn = 3L))
        assertFalse(movedToParentList(parentId = null, listId = 2L, landedOn = 3L))
        assertFalse(movedToParentList(parentId = 5L, listId = null, landedOn = 3L))
    }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
    }
}
