package org.tasks.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.compose.pickers.DAY_BEFORE_DUE
import org.tasks.compose.pickers.NO_TIME
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.time.DateTime

class TaskEditMergeTest {
    private val due = DateTime(2026, 3, 10, 12, 0).millis
    private val laterDue = DateTime(2026, 3, 17, 12, 0).millis

    private fun listOn(accountType: Int) = CaldavFilter(
        calendar = CaldavCalendar(account = "acct", uuid = "cal"),
        account = CaldavAccount(uuid = "acct", accountType = accountType),
    )

    private fun editorWithRelativeStart(accountType: Int): TaskEditViewModel.State {
        val resolved = DateTime(2026, 3, 9, 0, 0).millis
        val task = Task(id = 1, title = "t", dueDate = due, hideUntil = resolved)
        return TaskEditViewModel.State(
            isLoading = false,
            task = task,
            originalTask = task.copy(),
            list = listOn(accountType),
            originalList = listOn(accountType),
            startDay = DAY_BEFORE_DUE,
            startTime = NO_TIME,
            originalStartDay = DAY_BEFORE_DUE,
            originalStartTime = NO_TIME,
        )
    }

    private fun TaskEditViewModel.State.dueDateMovedExternally(): Task =
        originalTask.copy(dueDate = laterDue, modificationDate = originalTask.modificationDate + 1)

    @Test
    fun aRelativeStartFollowsAnExternalDueDateWhereTheBackendKeepsNoStartDateOfItsOwn() {
        val state = editorWithRelativeStart(CaldavAccount.TYPE_LOCAL)

        val merged = state.mergedWith(state.dueDateMovedExternally())

        assertEquals(DateTime(2026, 3, 16, 0, 0).millis, merged.task.hideUntil)
        assertEquals(DAY_BEFORE_DUE, merged.startDay)
    }

    @Test
    fun aRelativeStartTakesTheStoredDateWhereTheBackendKeepsOneOfItsOwn() {
        val state = editorWithRelativeStart(CaldavAccount.TYPE_CALDAV)

        val merged = state.mergedWith(state.dueDateMovedExternally())

        assertEquals(state.originalTask.hideUntil, merged.task.hideUntil)
    }

    @Test
    fun aStartDateChangedHereIsKeptWhateverTheBackendSays() {
        val state = editorWithRelativeStart(CaldavAccount.TYPE_CALDAV)
            .copy(startDay = DateTime(2026, 3, 1, 0, 0).millis)

        val merged = state.mergedWith(state.dueDateMovedExternally())

        assertEquals(DateTime(2026, 3, 1, 0, 0).millis, merged.task.hideUntil)
    }

    @Test
    fun aFieldNobodyTouchedHereTakesWhatTheDatabaseSays() {
        val state = editorWithRelativeStart(CaldavAccount.TYPE_CALDAV)

        val merged = state.mergedWith(state.originalTask.copy(title = "renamed elsewhere"))

        assertEquals("renamed elsewhere", merged.task.title)
    }

    @Test
    fun aFieldEditedHereKeepsWhatWasTypedIntoIt() {
        val state = editorWithRelativeStart(CaldavAccount.TYPE_CALDAV)
            .let { it.copy(task = it.task.copy(title = "typed here")) }

        val merged = state.mergedWith(state.originalTask.copy(title = "renamed elsewhere"))

        assertEquals("typed here", merged.task.title)
        assertEquals("renamed elsewhere", merged.originalTask.title)
    }

    @Test
    fun aRowDeletedUnderneathTheEditorIsReportedRatherThanMerged() {
        val state = editorWithRelativeStart(CaldavAccount.TYPE_CALDAV)
            .let { it.copy(task = it.task.copy(title = "typed here")) }

        val merged = state.mergedWith(state.originalTask.copy(deletionDate = 1_000L))

        assertEquals(true, merged.deleted)
        assertEquals("typed here", merged.task.title)
    }

    @Test
    fun aRowSayingWhatTheEditorAlreadyHasIsNotMergedAtAll() {
        val state = editorWithRelativeStart(CaldavAccount.TYPE_CALDAV)

        assertEquals(state, state.mergedWith(state.originalTask.copy(id = 99)))
    }
}
