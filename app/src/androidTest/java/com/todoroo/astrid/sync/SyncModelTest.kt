package com.todoroo.astrid.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.todoroo.astrid.data.Task
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncModelTest : NewSyncTestCase() {
    @Test
    fun testCreateTaskMakesUuid() {
        val task = createTask()
        assertNotEquals(Task.NO_UUID, task.uuid)
    }

    @Test
    fun testCreateTagMakesUuid() {
        val tag = createTagData()
        assertNotEquals(Task.NO_UUID, tag.remoteId)
    }
}