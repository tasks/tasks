package com.todoroo.astrid.subtasks

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.todoroo.astrid.data.Task
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.data.TaskListMetadata
import org.tasks.injection.TestComponent

@RunWith(AndroidJUnit4::class)
class SubtasksMovingTest : SubtasksTestCase() {
    private lateinit var A: Task
    private lateinit var B: Task
    private lateinit var C: Task
    private lateinit var D: Task
    private lateinit var E: Task
    private lateinit var F: Task

    override fun setUp() {
        super.setUp()
        createTasks()
        val m = TaskListMetadata()
        m.filter = TaskListMetadata.FILTER_ID_ALL
        updater.initializeFromSerializedTree(
                m, filter, SubtasksHelper.convertTreeToRemoteIds(taskDao, DEFAULT_SERIALIZED_TREE))

        // Assert initial state is correct
        expectParentAndPosition(A, null, 0)
        expectParentAndPosition(B, A, 0)
        expectParentAndPosition(C, A, 1)
        expectParentAndPosition(D, C, 0)
        expectParentAndPosition(E, null, 1)
        expectParentAndPosition(F, null, 2)
    }

    override fun inject(component: TestComponent) = component.inject(this)

    private fun createTasks() {
        A = createTask("A")
        B = createTask("B")
        C = createTask("C")
        D = createTask("D")
        E = createTask("E")
        F = createTask("F")
    }

    private fun createTask(title: String): Task {
        val task = Task()
        task.title = title
        taskDao.createNew(task)
        return task
    }

    private fun whenTriggerMoveBefore(target: Task?, before: Task?) {
        val beforeId = if (before == null) "-1" else before.uuid
        updater.moveTo(null, filter, target!!.uuid, beforeId)
    }

    /* Starting State (see SubtasksTestCase):
   *
   * A
   *  B
   *  C
   *   D
   * E
   * F
   */
    @Test
    fun testMoveBeforeIntoSelf() { // Should have no effect
        whenTriggerMoveBefore(A, B)
        expectParentAndPosition(A, null, 0)
        expectParentAndPosition(B, A, 0)
        expectParentAndPosition(C, A, 1)
        expectParentAndPosition(D, C, 0)
        expectParentAndPosition(E, null, 1)
        expectParentAndPosition(F, null, 2)
    }

    @Test
    fun testMoveIntoDescendant() { // Should have no effect
        whenTriggerMoveBefore(A, C)
        expectParentAndPosition(A, null, 0)
        expectParentAndPosition(B, A, 0)
        expectParentAndPosition(C, A, 1)
        expectParentAndPosition(D, C, 0)
        expectParentAndPosition(E, null, 1)
        expectParentAndPosition(F, null, 2)
    }

    @Test
    fun testMoveToEndOfChildren() { // Should have no effect
        whenTriggerMoveBefore(A, E)
        expectParentAndPosition(A, null, 0)
        expectParentAndPosition(B, A, 0)
        expectParentAndPosition(C, A, 1)
        expectParentAndPosition(D, C, 0)
        expectParentAndPosition(E, null, 1)
        expectParentAndPosition(F, null, 2)
    }

    @Test
    fun testStandardMove() {
        whenTriggerMoveBefore(A, F)
        expectParentAndPosition(A, null, 1)
        expectParentAndPosition(B, A, 0)
        expectParentAndPosition(C, A, 1)
        expectParentAndPosition(D, C, 0)
        expectParentAndPosition(E, null, 0)
        expectParentAndPosition(F, null, 2)
    }

    @Test
    fun testMoveToEndOfList() {
        whenTriggerMoveBefore(A, null)
        expectParentAndPosition(A, null, 2)
        expectParentAndPosition(B, A, 0)
        expectParentAndPosition(C, A, 1)
        expectParentAndPosition(D, C, 0)
        expectParentAndPosition(E, null, 0)
        expectParentAndPosition(F, null, 1)
    }
}