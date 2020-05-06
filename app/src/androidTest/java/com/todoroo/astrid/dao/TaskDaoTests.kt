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
        assertEquals(0, taskDao.all.size)

        // create task "happy"
        var task = Task()
        task.setTitle("happy")
        taskDao.createNew(task)
        assertEquals(1, taskDao.all.size)
        val happyId = task.getId()
        assertNotSame(Task.NO_ID, happyId)
        task = taskDao.fetch(happyId)
        assertEquals("happy", task.getTitle())

        // create task "sad"
        task = Task()
        task.setTitle("sad")
        taskDao.createNew(task)
        assertEquals(2, taskDao.all.size)

        // rename sad to melancholy
        val sadId = task.getId()
        assertNotSame(Task.NO_ID, sadId)
        task.setTitle("melancholy")
        taskDao.save(task)
        assertEquals(2, taskDao.all.size)

        // check state
        task = taskDao.fetch(happyId)
        assertEquals("happy", task.getTitle())
        task = taskDao.fetch(sadId)
        assertEquals("melancholy", task.getTitle())
    }

    /** Test various task fetch conditions  */
    @Test
    fun testTaskConditions() {
        // create normal task
        var task = Task()
        task.setTitle("normal")
        taskDao.createNew(task)

        // create blank task
        task = Task()
        task.setTitle("")
        taskDao.createNew(task)

        // create hidden task
        task = Task()
        task.setTitle("hidden")
        task.setHideUntil(DateUtilities.now() + 10000)
        taskDao.createNew(task)

        // create task with deadlines
        task = Task()
        task.setTitle("deadlineInFuture")
        task.setDueDate(DateUtilities.now() + 10000)
        taskDao.createNew(task)
        task = Task()
        task.setTitle("deadlineInPast")
        task.setDueDate(DateUtilities.now() - 10000)
        taskDao.createNew(task)

        // create completed task
        task = Task()
        task.setTitle("completed")
        task.completionDate = DateUtilities.now() - 10000
        taskDao.createNew(task)

        // check is active
        assertEquals(5, taskDao.activeTasks.size)

        // check is visible
        assertEquals(5, taskDao.visibleTasks.size)
    }

    /** Test task deletion  */
    @Test
    fun testTDeletion() {
        assertEquals(0, taskDao.all.size)

        // create task "happy"
        val task = Task()
        task.setTitle("happy")
        taskDao.createNew(task)
        assertEquals(1, taskDao.all.size)

        // delete
        taskDeleter.delete(task)
        assertEquals(0, taskDao.all.size)
    }

    /** Test save without prior create doesn't work  */
    @Test
    fun testSaveWithoutCreate() {
        // try to save task "happy"
        val task = Task()
        task.setTitle("happy")
        task.setId(1L)
        taskDao.save(task)
        assertEquals(0, taskDao.all.size)
    }

    /** Test passing invalid task indices to various things  */
    @Test
    fun testInvalidIndex() {
        assertEquals(0, taskDao.all.size)
        assertNull(taskDao.fetch(1))
        taskDeleter.delete(listOf(1L))

        // make sure db still works
        assertEquals(0, taskDao.all.size)
    }

    @Test
    fun findChildrenInList() {
        taskDao.createNew(newTask(with(ID, 1L)))
        taskDao.createNew(newTask(with(ID, 2L), with(PARENT, 1L)))
        assertEquals(listOf(2L), taskDao.findChildrenInList(listOf(1, 2)))
    }

    @Test
    fun findRecursiveChildrenInList() {
        taskDao.createNew(newTask(with(ID, 1L)))
        taskDao.createNew(newTask(with(ID, 2L), with(PARENT, 1L)))
        taskDao.createNew(newTask(with(ID, 3L), with(PARENT, 2L)))
        assertEquals(listOf(2L, 3L), taskDao.findChildrenInList(listOf(1, 2, 3)))
    }

    @Test
    fun findRecursiveChildrenInListAfterSkippingParent() {
        taskDao.createNew(newTask(with(ID, 1L)))
        taskDao.createNew(newTask(with(ID, 2L), with(PARENT, 1L)))
        taskDao.createNew(newTask(with(ID, 3L), with(PARENT, 2L)))
        assertEquals(listOf(3L), taskDao.findChildrenInList(listOf(1, 3)))
    }

    override fun inject(component: TestComponent) = component.inject(this)
}