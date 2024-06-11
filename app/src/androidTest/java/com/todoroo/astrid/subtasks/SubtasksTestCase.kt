package com.todoroo.astrid.subtasks

import androidx.test.InstrumentationRegistry
import com.todoroo.astrid.core.BuiltInFilterExposer
import com.todoroo.astrid.dao.TaskDao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.tasks.data.dao.TaskListMetadataDao
import org.tasks.data.entity.Task
import org.tasks.filters.AstridOrderingFilter
import org.tasks.injection.InjectingTestCase
import org.tasks.preferences.Preferences
import javax.inject.Inject

abstract class SubtasksTestCase : InjectingTestCase() {
    lateinit var updater: SubtasksFilterUpdater
    lateinit var filter: AstridOrderingFilter
    @Inject lateinit var taskListMetadataDao: TaskListMetadataDao
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var preferences: Preferences
    
    override fun setUp() {
        super.setUp()
        filter = BuiltInFilterExposer.getMyTasksFilter(InstrumentationRegistry.getTargetContext().resources)
        preferences.clear(SubtasksFilterUpdater.ACTIVE_TASKS_ORDER)
        updater = SubtasksFilterUpdater(taskListMetadataDao, taskDao)
    }

    fun expectParentAndPosition(task: Task, parent: Task?, positionInParent: Int) {
        val parentId = parent?.uuid ?: "-1"
        val n = updater.findNodeForTask(task.uuid)
        assertNotNull("No node found for task " + task.title, n)
        assertEquals("Parent mismatch", parentId, n!!.parent!!.uuid)
        assertEquals("Position mismatch", positionInParent, n.parent!!.children.indexOf(n))
    }

    companion object {
        /* Starting State:
   *
   * A
   *  B
   *  C
   *   D
   * E
   * F
   */
        val DEFAULT_SERIALIZED_TREE = "[-1, [1, 2, [3, 4]], 5, 6]".replace("\\s".toRegex(), "")
    }
}