package org.tasks.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.TaskListTest
import org.tasks.data.entity.Task

class SubtaskFoldingTest : TaskListTest() {
    private suspend fun dirtyVersion(task: Task): Long =
        db.dirtyDao().getDirtyStateByTaskIds(listOf(task.id))[task.id]?.dirtyVersion ?: 0

    private suspend fun foldedAway(
        parent: Task,
        showCompleted: Boolean = true,
        atBottom: Boolean = true,
        showCompletedSubtasks: Boolean = true,
    ): List<String?> {
        val wasCollapsed = taskDao.fetch(parent.id)!!.isCollapsed
        taskDao.setCollapsed(listOf(parent.id), false)
        val expanded = rows(showCompleted, atBottom, showCompletedSubtasks)
        taskDao.setCollapsed(listOf(parent.id), true)
        val folded = rows(showCompleted, atBottom, showCompletedSubtasks)
            .mapTo(HashSet()) { it.id }
        taskDao.setCollapsed(listOf(parent.id), wasCollapsed)
        return expanded.filterNot { folded.contains(it.id) }.map { it.title }
    }

    @Test
    fun aCompletedSubtaskSortsAfterItsSiblingsUnderTheParent() = runBlocking {
        val parent = newTask(PARENT)
        newTask(DONE, parent = parent.id, completed = true)
        newTask(TODO, parent = parent.id)

        assertEquals(listOf(PARENT, TODO, DONE), titles())
        assertEquals(1, row(DONE).indent)
    }

    @Test
    fun aCompletedSubtaskFoldsAwayWithItsParent() = runBlocking {
        val parent = newTask(PARENT)
        newTask(TODO, parent = parent.id)
        newTask(DONE, parent = parent.id, completed = true)

        assertEquals(setOf(TODO, DONE), foldedAway(parent).toSet())
    }

    @Test
    fun aFoldedParentHidesItsCompletedSubtask() = runBlocking {
        val parent = newTask(PARENT)
        newTask(TODO, parent = parent.id)
        newTask(DONE, parent = parent.id, completed = true)
        taskDao.setCollapsed(listOf(parent.id), true)

        assertEquals(listOf(PARENT), titles())
    }

    @Test
    fun aSubtaskCompletedWhileTheParentIsFoldedStaysHidden() = runBlocking {
        val parent = newTask(PARENT)
        val subtask = newTask(TODO, parent = parent.id)
        taskDao.setCollapsed(listOf(parent.id), true)
        assertEquals(listOf(PARENT), titles())

        taskDao.update(subtask.copy(completionDate = COMPLETED_AT))

        assertEquals(listOf(PARENT), titles())
    }

    @Test
    fun completedSubtasksCanBeHiddenOnTheirOwn() = runBlocking {
        val parent = newTask(PARENT)
        newTask(TODO, parent = parent.id)
        newTask(DONE, parent = parent.id, completed = true)

        assertEquals(listOf(PARENT, TODO), titles(showCompletedSubtasks = false))
    }

    @Test
    fun hidingCompletedSubtasksLeavesCompletedRootTasksAlone() = runBlocking {
        val parent = newTask(PARENT)
        newTask(DONE, parent = parent.id, completed = true)
        newTask(DONE_ROOT, completed = true)

        assertEquals(listOf(PARENT, DONE_ROOT), titles(showCompletedSubtasks = false))
    }

    @Test
    fun hidingCompletedSubtasksReachesInsideTheCompletedSection() = runBlocking {
        val parent = newTask(PARENT)
        newTask(TODO, parent = parent.id)
        val doneRoot = newTask(DONE_ROOT, completed = true)
        newTask(DONE, parent = doneRoot.id, completed = true)

        for (atBottom in listOf(false, true)) {
            val where = "atBottom=$atBottom"
            assertEquals(
                where,
                setOf(PARENT, TODO, DONE_ROOT),
                titles(atBottom = atBottom, showCompletedSubtasks = false).toSet(),
            )
            assertEquals(
                where,
                0,
                row(DONE_ROOT, atBottom = atBottom, showCompletedSubtasks = false).children,
            )
        }
    }

    @Test
    fun hidingCompletedSubtasksSurvivesTheShowCompletedRewrite() = runBlocking {
        val parent = newTask(PARENT)
        newTask(DONE, parent = parent.id, completed = true)

        assertEquals(
            listOf(PARENT),
            titles(showCompleted = true, showCompletedSubtasks = false),
        )
    }

    @Test
    fun theSubtaskToggleIsInertWhenCompletedTasksAreHidden() = runBlocking {
        val parent = newTask(PARENT)
        newTask(TODO, parent = parent.id)
        newTask(DONE, parent = parent.id, completed = true)
        newTask(DONE_ROOT, completed = true)

        assertEquals(
            titles(showCompleted = false, showCompletedSubtasks = true),
            titles(showCompleted = false, showCompletedSubtasks = false),
        )
        assertEquals(listOf(PARENT, TODO), titles(showCompleted = false))
    }

    @Test
    fun hiddenCompletedSubtasksDropOutOfTheCount() = runBlocking {
        val parent = newTask(PARENT)
        newTask(TODO, parent = parent.id)
        newTask(DONE, parent = parent.id, completed = true)

        assertEquals(2, row(PARENT).children)
        assertEquals(1, row(PARENT, showCompletedSubtasks = false).children)
    }

    @Test
    fun hiddenCompletedSubtasksDropOutOfTheCountSortedInPlace() = runBlocking {
        val parent = newTask(PARENT)
        newTask(TODO, parent = parent.id)
        newTask(DONE, parent = parent.id, completed = true)

        assertEquals(2, row(PARENT, atBottom = false).children)
        assertEquals(
            1,
            row(PARENT, atBottom = false, showCompletedSubtasks = false).children,
        )
    }

    @Test
    fun aCompletedSubtaskKeepsItsOwnSubtreeUnderneathIt() = runBlocking {
        val parent = newTask(PARENT)
        val active = newTask("active", parent = parent.id)
        newTask("active-sub", parent = active.id)
        val done = newTask(DONE, parent = parent.id, completed = true)
        newTask("done-sub", parent = done.id, completed = true)
        val later = newTask("later", parent = parent.id)
        newTask("later-sub", parent = later.id)

        assertEquals(
            listOf(PARENT, "active", "active-sub", "later", "later-sub", DONE, "done-sub"),
            titles(),
        )
        assertEquals(2, row("done-sub").indent)
        assertFalse(row(DONE).parentComplete)
        assertFalse(row("done-sub").parentComplete)
        assertEquals(
            listOf(PARENT, "active", "active-sub", DONE, "done-sub", "later", "later-sub"),
            titles(atBottom = false),
        )
        assertEquals(2, row("done-sub", atBottom = false).indent)
    }

    @Test
    fun hidingCompletedSubtasksTakesAwayTheWholeCompletedBranch() = runBlocking {
        val parent = newTask(PARENT)
        newTask(TODO, parent = parent.id)
        val done = newTask(DONE, parent = parent.id, completed = true)
        newTask("done-sub", parent = done.id, completed = true)

        assertEquals(listOf(PARENT, TODO), titles(showCompletedSubtasks = false))
        assertEquals(3, row(PARENT).children)
        assertEquals(1, row(PARENT, showCompletedSubtasks = false).children)
    }

    @Test
    fun hidingACompletedGrandchildComesOffEveryAncestorsCount() = runBlocking {
        val parent = newTask(PARENT)
        val mid = newTask(TODO, parent = parent.id)
        newTask(DONE, parent = mid.id, completed = true)

        assertEquals(listOf(PARENT, TODO), titles(showCompletedSubtasks = false))
        assertEquals(2, row(PARENT).children)
        assertEquals(1, row(PARENT, showCompletedSubtasks = false).children)
        assertEquals(0, row(TODO, showCompletedSubtasks = false).children)
    }

    @Test
    fun theReportedCountIsWhatTheFoldTakesAwayWithCompletedSubtasksHidden() = runBlocking {
        val parent = newTask(PARENT)
        val active = newTask("active", parent = parent.id)
        newTask("active-sub", parent = active.id)
        val done = newTask(DONE, parent = parent.id, completed = true)
        newTask("done-sub", parent = done.id, completed = true)

        for (atBottom in listOf(false, true)) {
            val where = "atBottom=$atBottom"
            assertEquals(
                where,
                foldedAway(parent, atBottom = atBottom, showCompletedSubtasks = false).size,
                row(PARENT, atBottom = atBottom, showCompletedSubtasks = false).children,
            )
        }
    }

    @Test
    fun foldingStillTakesAwayAWholeCompletedSubtree() = runBlocking {
        val parent = newTask(PARENT, completed = true)
        newTask(DONE, parent = parent.id, completed = true)

        assertEquals(listOf(DONE), foldedAway(parent))
    }

    @Test
    fun completedTasksInPlaceFoldWithTheirParent() = runBlocking {
        val parent = newTask(PARENT)
        newTask(TODO, parent = parent.id)
        newTask(DONE, parent = parent.id, completed = true)

        assertEquals(setOf(TODO, DONE), foldedAway(parent, atBottom = false).toSet())
    }

    @Test
    fun theReportedCountIsWhatTheFoldTakesAway() = runBlocking {
        val parent = newTask(PARENT)
        newTask("todo-1", parent = parent.id)
        newTask("todo-2", parent = parent.id)
        newTask("done-1", parent = parent.id, completed = true)
        newTask("done-2", parent = parent.id, completed = true)
        newTask("done-3", parent = parent.id, completed = true)

        for (showCompleted in listOf(false, true)) {
            for (atBottom in listOf(false, true)) {
                val where = "showCompleted=$showCompleted atBottom=$atBottom"
                assertEquals(
                    where,
                    foldedAway(parent, showCompleted, atBottom).size,
                    row(PARENT, showCompleted, atBottom).children,
                )
            }
        }
    }

    @Test
    fun expandingAllClearsAFoldWithNothingLeftToFold() = runBlocking {
        val parent = newTask(PARENT)
        newTask(DONE, parent = parent.id, completed = true)
        taskDao.setCollapsed(listOf(parent.id), true)

        taskDao.setCollapsed(preferences(showCompleted = false), filter, collapsed = false)

        assertFalse(taskDao.fetch(parent.id)!!.isCollapsed)
    }

    @Test
    fun collapsingAllLeavesAloneARowWithNothingToFold() = runBlocking {
        val parent = newTask(PARENT)
        newTask(DONE, parent = parent.id, completed = true)
        val dirtyBefore = dirtyVersion(parent)

        taskDao.setCollapsed(preferences(showCompleted = false), filter, collapsed = true)

        assertFalse(taskDao.fetch(parent.id)!!.isCollapsed)
        assertEquals(dirtyBefore, dirtyVersion(parent))
    }

    @Test
    fun collapsingAllSkipsARowThatIsAlreadyFolded() = runBlocking {
        val parent = newTask(PARENT)
        newTask(TODO, parent = parent.id)
        taskDao.setCollapsed(listOf(parent.id), true)
        val dirtyBefore = dirtyVersion(parent)

        taskDao.setCollapsed(preferences(), filter, collapsed = true)

        assertTrue(taskDao.fetch(parent.id)!!.isCollapsed)
        assertEquals(dirtyBefore, dirtyVersion(parent))
    }

    @Test
    fun collapsingAllStillFoldsARowWithSomethingToFold() = runBlocking {
        val parent = newTask(PARENT)
        newTask(TODO, parent = parent.id)

        taskDao.setCollapsed(preferences(), filter, collapsed = true)

        assertTrue(taskDao.fetch(parent.id)!!.isCollapsed)
    }

    @Test
    fun expandingAllLeavesAloneARowThatIsAlreadyUnfolded() = runBlocking {
        val parent = newTask(PARENT)
        newTask(TODO, parent = parent.id)
        val dirtyBefore = dirtyVersion(parent)

        taskDao.setCollapsed(preferences(), filter, collapsed = false)

        assertFalse(taskDao.fetch(parent.id)!!.isCollapsed)
        assertEquals(dirtyBefore, dirtyVersion(parent))
    }

    @Test
    fun aCompletedParentCountsEverySubtask() = runBlocking {
        val parent = newTask(PARENT, completed = true)
        newTask(TODO, parent = parent.id)
        newTask(DONE, parent = parent.id, completed = true)

        assertEquals(2, row(PARENT).children)
    }

    @Test
    fun theCountIsWhatTheFoldTakesAwayForACompletedParent() = runBlocking {
        val parent = newTask(PARENT, completed = true)
        newTask(TODO, parent = parent.id)
        newTask(DONE, parent = parent.id, completed = true)

        assertEquals(foldedAway(parent).size, row(PARENT).children)
    }

    @Test
    fun aCompletedSubtaskKeepsItsSiblingOrderWhenSortedInPlace() = runBlocking {
        val parent = newTask(PARENT)
        newTask(DONE, parent = parent.id, completed = true)
        newTask(TODO, parent = parent.id)

        assertEquals(listOf(PARENT, DONE, TODO), titles(atBottom = false))
        assertEquals(1, row(DONE, atBottom = false).indent)
    }
}
