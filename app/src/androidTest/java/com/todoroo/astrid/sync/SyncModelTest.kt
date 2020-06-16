package com.todoroo.astrid.sync

import com.todoroo.astrid.data.Task
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.tasks.injection.ProductionModule

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
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