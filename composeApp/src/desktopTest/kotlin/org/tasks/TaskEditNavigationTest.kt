package org.tasks

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.entity.Task

class TaskEditNavigationTest {
    private val list: NavKey = TaskListDestination
    private fun editor(id: Long) = TaskEditDestination(taskId = id, remoteId = "uuid-$id")

    @Test
    fun openingFromTheListWithNoEditorOpenJustOpensIt() {
        assertEquals(
            OpenTask.Replace,
            openTaskFromList(
                backStack = listOf(list),
                destination = editor(1),
                heldByEditor = false,
                doomedByEditor = false,
            ),
        )
    }

    @Test
    fun openingATaskThatIsNothingToDoWithTheOpenEditorReplacesIt() {
        assertEquals(
            OpenTask.Replace,
            openTaskFromList(
                backStack = listOf(list, editor(1)),
                destination = editor(2),
                heldByEditor = false,
                doomedByEditor = false,
            ),
        )
    }

    @Test
    fun openingASubtaskOfTheOpenEditorStacksOnTopOfIt() {
        assertEquals(
            OpenTask.Stack,
            openTaskFromList(
                backStack = listOf(list, editor(1)),
                destination = editor(2),
                heldByEditor = true,
                doomedByEditor = false,
            ),
        )
    }

    @Test
    fun openingASubtaskMarkedForDeletionDoesNothing() {
        assertEquals(
            OpenTask.Ignore,
            openTaskFromList(
                backStack = listOf(list, editor(1)),
                destination = editor(2),
                heldByEditor = true,
                doomedByEditor = true,
            ),
        )
    }

    @Test
    fun aDoomedTaskIsStillOpenedWhenNoEditorIsShowingIt() {
        assertEquals(
            OpenTask.Replace,
            openTaskFromList(
                backStack = listOf(list),
                destination = editor(2),
                heldByEditor = true,
                doomedByEditor = true,
            ),
        )
    }

    @Test
    fun askingForASubtaskThatIsAlreadyOpenBelowWindsBackToIt() {
        assertEquals(
            OpenTask.Resume(1),
            openSubtask(
                backStack = listOf(list, editor(2), editor(3)),
                destination = editor(2),
            ),
        )
    }

    @Test
    fun oneSubtaskWithNoRowYetIsNotMistakenForAnother() {
        val draft = TaskEditDestination(taskId = 0L, remoteId = "uuid-draft")
        val another = TaskEditDestination(taskId = 0L, remoteId = "uuid-another")

        assertEquals(
            OpenTask.Stack,
            openSubtask(backStack = listOf(list, editor(1), draft), destination = another),
        )
    }

    @Test
    fun aSubtaskWithNoRowYetIsStillOnlyOpenedOnce() {
        val draft = TaskEditDestination(taskId = 0L, remoteId = "uuid-draft")

        assertEquals(
            OpenTask.Resume(2),
            openSubtask(backStack = listOf(list, editor(1), draft), destination = draft),
        )
    }

    @Test
    fun aDraftWhoseRowNowExistsIsTheSameTaskAsTheEditorAlreadyOnIt() {
        val draft = TaskEditDestination(taskId = 0L, remoteId = "uuid-draft", isSubtaskDraft = true)
        val created = TaskEditDestination(taskId = 55L, remoteId = "uuid-draft")

        assertEquals(
            OpenTask.Resume(2),
            openSubtask(backStack = listOf(list, editor(1), draft), destination = created),
        )
    }

    @Test
    fun twoRowsWithNoRemoteIdAreNotTheSameTask() {
        val parent = TaskEditDestination(taskId = 5L, remoteId = Task.NO_UUID)
        val child = TaskEditDestination(taskId = 11L, remoteId = Task.NO_UUID)

        assertEquals(
            OpenTask.Stack,
            openSubtask(backStack = listOf(list, parent), destination = child),
        )
    }

    @Test
    fun aRowWithNoRemoteIdIsStillOnlyOpenedOnce() {
        val child = TaskEditDestination(taskId = 11L, remoteId = Task.NO_UUID)

        assertEquals(
            OpenTask.Resume(2),
            openSubtask(backStack = listOf(list, editor(5), child), destination = child),
        )
    }

    @Test
    fun aRequestThatOutlivedTheEditorItCameFromIsDropped() {
        assertEquals(
            OpenTask.Ignore,
            openSubtask(backStack = listOf(list), destination = editor(2)),
        )
    }
}
