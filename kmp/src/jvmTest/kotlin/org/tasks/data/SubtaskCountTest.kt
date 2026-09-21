package org.tasks.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.TaskListTest

class SubtaskCountTest : TaskListTest() {
    @Test
    fun theChipCountsWhatIsLeftToDo() = runBlocking {
        val parent = newTask(PARENT)
        newTask(TODO, parent = parent.id)
        newTask(DONE, parent = parent.id, completed = true)

        assertEquals(1, row(PARENT).uncompletedChildren)
        assertEquals(2, row(PARENT).children)
    }

    @Test
    fun theChipCountsDescendantsAtEveryDepth() = runBlocking {
        val parent = newTask(PARENT)
        val child = newTask(CHILD, parent = parent.id)
        newTask(TODO, parent = child.id)

        assertEquals(2, row(PARENT).uncompletedChildren)
    }

    @Test
    fun aCompletedSubtaskStillContributesItsUnfinishedChildren() = runBlocking {
        val parent = newTask(PARENT)
        val done = newTask(DONE, parent = parent.id, completed = true)
        newTask(TODO, parent = done.id)

        assertEquals(1, row(PARENT).uncompletedChildren)
        assertEquals(2, row(PARENT).children)
    }

    @Test
    fun aParentWithNothingLeftKeepsItsChipToExpandWith() = runBlocking {
        val parent = newTask(PARENT)
        newTask(DONE, parent = parent.id, completed = true)
        newTask(DONE_ROOT, parent = parent.id, completed = true)

        assertEquals(0, row(PARENT).uncompletedChildren)
        assertEquals(2, row(PARENT).children)
    }

    @Test
    fun aCompletedParentCountsWhatIsUnderItInstead() = runBlocking {
        val parent = newTask(PARENT, completed = true)
        newTask(DONE, parent = parent.id, completed = true)
        newTask(DONE_ROOT, parent = parent.id, completed = true)

        assertEquals(2, row(PARENT).chipCount)
        assertEquals(0, row(PARENT).uncompletedChildren)
    }

    @Test
    fun aCompletedParentCountsOnlyWhatIsStillOnScreen() = runBlocking {
        val parent = newTask(PARENT, completed = true)
        newTask(TODO, parent = parent.id)
        newTask(DONE, parent = parent.id, completed = true)

        assertEquals(2, row(PARENT).chipCount)
        assertEquals(1, row(PARENT, showCompletedSubtasks = false).chipCount)
    }

    @Test
    fun anUnfinishedParentStillCountsWhatIsLeft() = runBlocking {
        val parent = newTask(PARENT)
        newTask(TODO, parent = parent.id)
        newTask(DONE, parent = parent.id, completed = true)

        assertEquals(1, row(PARENT).chipCount)
    }

    @Test
    fun hidingCompletedSubtasksLeavesBothCountsAgreeing() = runBlocking {
        val parent = newTask(PARENT)
        newTask(TODO, parent = parent.id)
        newTask(DONE, parent = parent.id, completed = true)

        val row = row(PARENT, showCompletedSubtasks = false)

        assertEquals(1, row.children)
        assertEquals(1, row.uncompletedChildren)
    }
}
