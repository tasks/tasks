package com.todoroo.astrid.sync

import org.tasks.data.entity.Task
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Test

@HiltAndroidTest
class SyncModelTest : NewSyncTestCase() {

    @Test
    fun testCreateTaskMakesUuid() = runBlocking{
        val task = createTask()
        assertNotEquals(Task.NO_UUID, task.uuid)
    }

    @Test
    fun testCreateTagMakesUuid() = runBlocking{
        val tag = createTagData()
        assertNotEquals(Task.NO_UUID, tag.remoteId)
    }
}