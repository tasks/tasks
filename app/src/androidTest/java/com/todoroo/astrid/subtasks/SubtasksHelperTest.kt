package com.todoroo.astrid.subtasks

import org.tasks.data.entity.Task
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.tasks.data.entity.TaskListMetadata
import org.tasks.injection.ProductionModule

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class SubtasksHelperTest : SubtasksTestCase() {
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
    }

    private fun createTask(title: String, uuid: String) = runBlocking {
        val t = Task()
        t.title = title
        t.uuid = uuid
        taskDao.createNew(t)
    }

    private fun createTasks() {
        createTask("A", "6") // Local id 1
        createTask("B", "4") // Local id 2
        createTask("C", "3") // Local id 3
        createTask("D", "1") // Local id 4
        createTask("E", "2") // Local id 5
        createTask("F", "5") // Local id 6
    }

    // Default order: "[-1, [1, 2, [3, 4]], 5, 6]"
    @Test
    fun testOrderedIdArray() {
        val ids = SubtasksHelper.getStringIdArray(DEFAULT_SERIALIZED_TREE)
        assertEquals(EXPECTED_ORDER.size, ids.size)
        for (i in EXPECTED_ORDER.indices) {
            assertEquals(EXPECTED_ORDER[i], ids[i])
        }
    }

    @Test
    fun testLocalToRemoteIdMapping() = runBlocking {
        val mapped = SubtasksHelper.convertTreeToRemoteIds(taskDao, DEFAULT_SERIALIZED_TREE)
                .replace("\\s".toRegex(), "")
        assertEquals(EXPECTED_REMOTE, mapped)
    }

    companion object {
        private val EXPECTED_ORDER = arrayOf("-1", "1", "2", "3", "4", "5", "6")
        private val EXPECTED_REMOTE = """["-1", ["6", "4", ["3", "1"]], "2", "5"]""".replace("\\s".toRegex(), "")
    }
}