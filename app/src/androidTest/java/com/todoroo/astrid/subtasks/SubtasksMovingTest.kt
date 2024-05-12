package com.todoroo.astrid.subtasks

import org.tasks.data.entity.Task
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.tasks.data.entity.TaskListMetadata
import org.tasks.injection.ProductionModule

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class SubtasksMovingTest : SubtasksTestCase() {
    private lateinit var A: Task
    private lateinit var B: Task
    private lateinit var C: Task
    private lateinit var D: Task
    private lateinit var E: Task
    private lateinit var F: Task

    @Before
    override fun setUp() {
        super.setUp()
        createTasks()
        val m = TaskListMetadata()
        m.filter = TaskListMetadata.FILTER_ID_ALL
        runBlocking {
            updater.initializeFromSerializedTree(
                    m, filter, SubtasksHelper.convertTreeToRemoteIds(taskDao, DEFAULT_SERIALIZED_TREE))
        }

        // Assert initial state is correct
        expectParentAndPosition(A, null, 0)
        expectParentAndPosition(B, A, 0)
        expectParentAndPosition(C, A, 1)
        expectParentAndPosition(D, C, 0)
        expectParentAndPosition(E, null, 1)
        expectParentAndPosition(F, null, 2)
    }

    private fun createTasks() {
        A = createTask("A")
        B = createTask("B")
        C = createTask("C")
        D = createTask("D")
        E = createTask("E")
        F = createTask("F")
    }

    private fun createTask(title: String): Task = runBlocking {
        val task = Task()
        task.title = title
        taskDao.createNew(task)
        task
    }

    private fun whenTriggerMoveBefore(target: Task?, before: Task?) = runBlocking {
        val beforeId = before?.uuid ?: "-1"
        updater.moveTo(TaskListMetadata(), filter, target!!.uuid, beforeId)
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