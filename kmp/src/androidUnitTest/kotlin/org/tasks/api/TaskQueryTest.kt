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

    @Test
    fun openIsTheDefault() {
        newTask("Open")
        newTask("Done", Tasks.COMPLETED_AT to DAY)

        assertEquals(listOf("Open"), titles(TaskQuery(completed = taskStatus(null))))
        assertEquals(listOf("Done"), titles(TaskQuery(completed = taskStatus("completed"))))
        assertEquals(2, find(TaskQuery(completed = taskStatus("any"))).total)
    }

    @Test
    fun anUnknownStatusNamesTheLegalValues() {
        val message = runCatching { taskStatus("pending") }.exceptionOrNull()?.message.orEmpty()

        assertTrue(message, message.contains("open|completed|any"))
    }

    @Test
    fun aDueBeforeBoundOnItsOwnSkipsUnscheduledTasks() {
        newTask("Someday")
        newTask("Due", Tasks.DUE_DATE to DAY)

        assertEquals(listOf("Due"), titles(TaskQuery(dueBefore = DAY * 2)))
    }

    @Test
    fun theNoDueDateWindowStillFindsUnscheduledTasks() {
        newTask("Someday")
        newTask("Due", Tasks.DUE_DATE to DAY)

        val window = dueWindow("no_due_date")!!

        assertEquals(
            listOf("Someday"),
            titles(TaskQuery(dueAfter = window.first, dueBefore = window.second)),
        )
    }

    @Test
    fun anUnknownDueFilterNamesTheLegalValues() {
        val message = runCatching { dueWindow("tomorow") }.exceptionOrNull()?.message.orEmpty()

        assertTrue(message, message.contains(DUE_FILTERS.joinToString("|")))
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
        assertEquals(1, find(TaskQuery(tagIds = listOf(errands), limit = 0)).total)
    }

    @Test
    fun filteringOnCompletionDatesImpliesCompletedTasks() {
        newTask("Open")
        newTask("Done", Tasks.COMPLETED_AT to DAY)

        val completed = taskStatus(null, completionBounded = true)

        assertEquals(listOf("Done"), titles(TaskQuery(completed = completed, completedBefore = DAY * 2)))
    }

    @Test
    fun matchesTestsTitleAndNotesByDefault() {
        newTask("Pack tent")
        newTask("Buy firewood", Tasks.NOTES to "from the camp store")

        assertEquals(listOf("Buy firewood"), titles(TaskQuery(matches = Regex("camp store"))))
    }

    @Test
    fun matchFieldsNarrowsWhichTextIsTested() {
        newTask("Buy firewood", Tasks.NOTES to "from the camp store")

        assertEquals(
            emptyList<String>(),
            titles(TaskQuery(matches = Regex("camp store"), matchFields = setOf(TaskText.Title))),
        )
    }

    @Test
    fun aPatternIsUnanchoredUnlessYouAnchorIt() {
        newTask("Pack tent")
        newTask("Repack tent")

        assertEquals(2, find(TaskQuery(matches = Regex("Pack tent", IGNORE_CASE))).total)
        assertEquals(listOf("Pack tent"), titles(TaskQuery(matches = Regex("^Pack tent$"))))
    }

    @Test
    fun aScannedPageStillReportsTheTrueTotal() {
        repeat(3) { newTask("Task $it") }

        val page = find(TaskQuery(matches = Regex("Task"), limit = 1))

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

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
        val IGNORE_CASE = setOf(RegexOption.IGNORE_CASE)
    }
}
