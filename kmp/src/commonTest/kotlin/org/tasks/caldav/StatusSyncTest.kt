package org.tasks.caldav

import org.tasks.caldav.iCalendar.Companion.applyLocal
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.entity.Task.Status.Companion.CANCELLED
import org.tasks.data.entity.Task.Status.Companion.IN_PROCESS
import org.tasks.data.entity.Task.Status.Companion.NEEDS_ACTION
import org.tasks.icalendar.TodoStatus
import org.tasks.icalendar.VTodo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatusSyncTest {
    @Test
    fun newRemoteInProcess() {
        assertEquals(IN_PROCESS, merge(stored = NEEDS_ACTION, cached = null, remote = TodoStatus.IN_PROCESS))
    }

    @Test
    fun newRemoteCancelled() {
        assertEquals(CANCELLED, merge(stored = NEEDS_ACTION, cached = null, remote = TodoStatus.CANCELLED))
    }

    @Test
    fun missingRemoteStatusIsNeedsAction() {
        assertEquals(NEEDS_ACTION, merge(stored = IN_PROCESS, cached = TodoStatus.IN_PROCESS, remote = null))
    }

    @Test
    fun remoteChangeAppliesWhenUnchangedLocally() {
        assertEquals(
            NEEDS_ACTION,
            merge(stored = IN_PROCESS, cached = TodoStatus.IN_PROCESS, remote = TodoStatus.NEEDS_ACTION),
        )
    }

    @Test
    fun localChangeWins() {
        assertEquals(
            CANCELLED,
            merge(stored = CANCELLED, cached = TodoStatus.NEEDS_ACTION, remote = TodoStatus.IN_PROCESS),
        )
    }

    @Test
    fun remoteCompletionKeepsLastStatus() {
        assertEquals(
            IN_PROCESS,
            merge(stored = IN_PROCESS, cached = TodoStatus.IN_PROCESS, remote = TodoStatus.COMPLETED),
        )
    }

    @Test
    fun remoteCompletedAtWithoutStatusKeepsLastStatus() {
        val task = Task(status = IN_PROCESS).applyRemote(
            remote = VTodo(completedAt = 1_700_000_000_000),
            local = VTodo(status = TodoStatus.IN_PROCESS),
        )
        assertEquals(IN_PROCESS, task.status)
    }

    @Test
    fun remoteUncompleteAfterCompletedBaseAppliesRemote() {
        assertEquals(
            NEEDS_ACTION,
            merge(
                stored = IN_PROCESS,
                cached = TodoStatus.COMPLETED,
                remote = TodoStatus.NEEDS_ACTION,
                completed = true,
            ),
        )
    }

    @Test
    fun localUncompleteAfterCompletedBaseKeepsLocalStatus() {
        assertEquals(
            CANCELLED,
            merge(stored = CANCELLED, cached = TodoStatus.COMPLETED, remote = TodoStatus.NEEDS_ACTION),
        )
    }

    @Test
    fun pushInProcess() {
        assertEquals(TodoStatus.IN_PROCESS, push(Task(status = IN_PROCESS), VTodo()).status)
    }

    @Test
    fun pushCancelled() {
        assertEquals(TodoStatus.CANCELLED, push(Task(status = CANCELLED), VTodo()).status)
    }

    @Test
    fun completedWinsOverStatus() {
        val vtodo = push(Task(status = IN_PROCESS, completionDate = 1_700_000_000_000), VTodo())
        assertEquals(TodoStatus.COMPLETED, vtodo.status)
        assertEquals(100, vtodo.percentComplete)
    }

    @Test
    fun uncompleteRestoresLastStatus() {
        val remote = VTodo(
            status = TodoStatus.COMPLETED,
            completedAt = 1_700_000_000_000,
            percentComplete = 100,
        )
        val vtodo = push(Task(status = IN_PROCESS), remote)
        assertEquals(TodoStatus.IN_PROCESS, vtodo.status)
        assertNull(vtodo.completedAt)
        assertNull(vtodo.percentComplete)
    }

    @Test
    fun uncompleteWithoutLastStatusClearsStatus() {
        val remote = VTodo(status = TodoStatus.COMPLETED, completedAt = 1_700_000_000_000)
        assertNull(push(Task(), remote).status)
    }

    @Test
    fun doNotAddNeedsActionToTasksWithoutStatus() {
        assertNull(push(Task(), VTodo()).status)
    }

    @Test
    fun keepExistingNeedsAction() {
        assertEquals(
            TodoStatus.NEEDS_ACTION,
            push(Task(), VTodo(status = TodoStatus.NEEDS_ACTION)).status,
        )
    }

    @Test
    fun clearingInProcessRemovesStatus() {
        assertNull(push(Task(), VTodo(status = TodoStatus.IN_PROCESS)).status)
    }

    @Test
    fun clearingCancelledRemovesStatus() {
        assertNull(push(Task(), VTodo(status = TodoStatus.CANCELLED)).status)
    }

    @Test
    fun unknownRemoteStatusIsNeedsAction() {
        assertEquals(NEEDS_ACTION, merge(stored = NEEDS_ACTION, cached = null, remote = "X-WAITING"))
    }

    @Test
    fun keepUnknownStatusWhenNeedsAction() {
        assertEquals("X-WAITING", push(Task(), VTodo(status = "X-WAITING")).status)
    }

    @Test
    fun replaceUnknownStatusWithOurs() {
        assertEquals(TodoStatus.IN_PROCESS, push(Task(status = IN_PROCESS), VTodo(status = "X-WAITING")).status)
    }

    @Test
    fun completeReplacesUnknownStatus() {
        val vtodo = push(Task(completionDate = 1_700_000_000_000), VTodo(status = "X-WAITING"))
        assertEquals(TodoStatus.COMPLETED, vtodo.status)
    }

    @Test
    fun keepPercentCompleteWhileInProcess() {
        val vtodo = push(Task(status = IN_PROCESS), VTodo(status = TodoStatus.IN_PROCESS, percentComplete = 40))
        assertEquals(40, vtodo.percentComplete)
    }

    @Test
    fun roundTripCompleteThenUncomplete() {
        val task = Task(status = IN_PROCESS)
        var remote = push(task, VTodo())

        val completed = task.copy(completionDate = 1_700_000_000_000)
        val afterComplete = push(completed, remote.copy())
        assertEquals(TodoStatus.COMPLETED, afterComplete.status)

        val otherDevice = Task(status = IN_PROCESS).applyRemote(afterComplete, remote)
        assertEquals(IN_PROCESS, otherDevice.status)

        remote = afterComplete
        val uncompleted = otherDevice.copy(completionDate = 0)
        assertEquals(TodoStatus.IN_PROCESS, push(uncompleted, remote.copy()).status)
    }

    private fun merge(stored: Int, cached: String?, remote: String?, completed: Boolean = false): Int =
        Task(status = stored, completionDate = if (completed) 1_700_000_000_000 else 0)
            .applyRemote(
                remote = VTodo(status = remote),
                local = cached?.let { VTodo(status = it) },
            )
            .status

    private fun push(task: Task, remote: VTodo): VTodo =
        remote.apply { applyLocal(CaldavTask(task = task.id, calendar = "calendar"), task) }
}
