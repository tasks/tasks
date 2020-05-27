/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.data.GoogleTask
import org.tasks.data.GoogleTaskDao
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class GtasksMetadataServiceTest : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    
    private var task: Task? = null
    private var metadata: GoogleTask? = null
    
    override fun inject(component: TestComponent) = component.inject(this)

    @Test
    fun testMetadataFound() {
        givenTask(taskWithMetadata(null))
        whenSearchForMetadata()
        thenExpectMetadataFound()
    }

    @Test
    fun testMetadataDoesntExist() {
        givenTask(taskWithoutMetadata())
        whenSearchForMetadata()
        thenExpectNoMetadataFound()
    }

    private fun thenExpectNoMetadataFound() {
        assertNull(metadata)
    }

    private fun thenExpectMetadataFound() {
        assertNotNull(metadata)
    }

    // --- helpers
    private fun whenSearchForMetadata() {
        metadata = googleTaskDao.getByTaskId(task!!.id)
    }

    private fun taskWithMetadata(id: String?): Task {
        val task = Task()
        task.title = "cats"
        taskDao.createNew(task)
        val metadata = GoogleTask(task.id, "")
        if (id != null) {
            metadata.remoteId = id
        }
        metadata.task = task.id
        googleTaskDao.insert(metadata)
        return task
    }

    private fun givenTask(taskToTest: Task) {
        task = taskToTest
    }

    private fun taskWithoutMetadata(): Task {
        val task = Task()
        task.title = "dogs"
        taskDao.createNew(task)
        return task
    }
}