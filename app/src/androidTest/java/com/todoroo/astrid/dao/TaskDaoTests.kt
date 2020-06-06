/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.service.TaskDeleter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import org.tasks.makers.TaskMaker.ID
import org.tasks.makers.TaskMaker.PARENT
import org.tasks.makers.TaskMaker.newTask
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class TaskDaoTests : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var taskDeleter: TaskDeleter

    /** Test basic task creation, fetch, and save  */
    @Test
    fun testTaskCreation() {
        assertEquals(0, taskDao.getAll().size)

        // create task "happy"
        var task = Task()
        task.title = "happy"
        taskDao.createNew(task)
        assertEquals(1, taskDao.getAll().size)
        val happyId = task.id
        assertNotSame(Task.NO_ID, happyId)
        task = taskDao.fetch(happyId)!!
        assertEquals("happy", task.title)

        // create task "sad"
        task = Task()
        task.title = "sad"
        taskDao.createNew(task)
        assertEquals(2, taskDao.getAll().size)

        // rename sad to melancholy
        val sadId = task.id
        assertNotSame(Task.NO_ID, sadId)
        task.title = "melancholy"
        taskDao.save(task)
        assertEquals(2, taskDao.getAll().size)

        // check state
        task = taskDao.fetch(happyId)!!
        assertEquals("happy", task.title)
        task = taskDao.fetch(sadId)!!
        assertEquals("melancholy", task.title)
    }

    /** Test various task fetch conditions  */
    @Test
    fun testTaskConditions() {
        // create normal task
        var task = Task()
        task.title = "normal"
        taskDao.createNew(task)

        // create blank task
        task = Task()
        task.title = ""
        taskDao.createNew(task)

        // create hidden task
        task = Task()
        task.title = "hidden"
        task.hideUntil = DateUtilities.now() + 10000
        taskDao.createNew(task)

        // create task with deadlines
        task = Task()
        task.title = "deadlineInFuture"
        task.dueDate = DateUtilities.now() + 10000
        taskDao.createNew(task)
        task = Task()
        task.title = "deadlineInPast"
        task.dueDate = DateUtilities.now() - 10000
        taskDao.createNew(task)

        // create completed task
        task = Task()
        task.title = "completed"
        task.completionDate = DateUtilities.now() - 10000
        taskDao.createNew(task)

        // check is active
        assertEquals(5, taskDao.getActiveTasks().size)

        // check is visible
        assertEquals(5, taskDao.getActiveTasks().size)
    }

    /** Test task deletion  */
    @Test
    fun testTDeletion() {
        assertEquals(0, taskDao.getAll().size)

        // create task "happy"
        val task = Task()
        task.title = "happy"
        taskDao.createNew(task)
        assertEquals(1, taskDao.getAll().size)

        // delete
        taskDeleter.delete(task)
        assertEquals(0, taskDao.getAll().size)
    }

    /** Test save without prior create doesn't work  */
    @Test
    fun testSaveWithoutCreate() {
        // try to save task "happy"
        val task = Task()
        task.title = "happy"
        task.id = 1L
        taskDao.save(task)
        assertEquals(0, taskDao.getAll().size)
    }

    /** Test passing invalid task indices to various things  */
    @Test
    fun testInvalidIndex() {
        assertEquals(0, taskDao.getAll().size)
        assertNull(taskDao.fetch(1))
        taskDeleter.delete(listOf(1L))

        // make sure db still works
        assertEquals(0, taskDao.getAll().size)
    }

    @Test
    fun findChildrenInList() {
        taskDao.createNew(newTask(with(ID, 1L)))
        taskDao.createNew(newTask(with(ID, 2L), with(PARENT, 1L)))
        assertEquals(listOf(2L), taskDao.getChildren(listOf(1L, 2L)))
    }

    @Test
    fun findRecursiveChildrenInList() {
        taskDao.createNew(newTask(with(ID, 1L)))
        taskDao.createNew(newTask(with(ID, 2L), with(PARENT, 1L)))
        taskDao.createNew(newTask(with(ID, 3L), with(PARENT, 2L)))
        assertEquals(listOf(2L, 3L, 3L), taskDao.getChildren(listOf(1L, 2L, 3L)))
    }

    @Test
    fun findRecursiveChildrenInListAfterSkippingParent() {
        taskDao.createNew(newTask(with(ID, 1L)))
        taskDao.createNew(newTask(with(ID, 2L), with(PARENT, 1L)))
        taskDao.createNew(newTask(with(ID, 3L), with(PARENT, 2L)))
        assertEquals(listOf(2L, 3L), taskDao.getChildren(listOf(1L, 3L)))
    }

    override fun inject(component: TestComponent) = component.inject(this)
}