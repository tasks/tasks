package org.tasks.sync.microsoft

import com.natpryce.makeiteasy.MakeItEasy.with
import com.natpryce.makeiteasy.PropertyValue
import com.todoroo.astrid.adapter.TaskAdapter
import com.todoroo.astrid.adapter.TaskAdapterDataSource
import com.todoroo.astrid.service.TaskMover
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.tasks.LocalBroadcastManager
import org.tasks.data.TaskContainer
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavTask
import org.tasks.injection.InjectingTestCase
import org.tasks.makers.TaskContainerMaker.PARENT
import org.tasks.makers.TaskContainerMaker.newTaskContainer
import javax.inject.Inject

@HiltAndroidTest
class MicrosoftSyncTests : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    @Inject lateinit var taskSaver: TaskSaver
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var taskMover: TaskMover

    private lateinit var adapter: TaskAdapter
    private val tasks = ArrayList<TaskContainer>()

    @Before
    override fun setUp() {
        super.setUp()
        tasks.clear()
        adapter = TaskAdapter(
            false, googleTaskDao, caldavDao, taskDao, taskSaver,
            localBroadcastManager, taskMover,
        )
        adapter.setDataSource(object : TaskAdapterDataSource {
            override fun getItem(position: Int) = tasks[position]
            override fun getTaskCount() = tasks.size
        })
    }

    @Test
    fun indentMicrosoftTaskPreservesRemoteParent() = runBlocking {
        addTask()
        addTask()

        adapter.moved(1, 1, 1)

        // task.parent should be set
        assertEquals(tasks[0].id, taskDao.fetch(tasks[1].id)!!.parent)
        // remoteParent should NOT be updated for Microsoft tasks
        assertNull(caldavDao.getTask(tasks[1].id)!!.remoteParent)
    }

    @Test
    fun unindentMicrosoftTaskPreservesRemoteParent() = runBlocking {
        addTask()
        addTask(with(PARENT, tasks[0]))
        // Set a remoteParent to simulate a previously synced subtask
        val child = caldavDao.getTask(tasks[1].id)!!
        caldavDao.update(child.copy(remoteParent = "remote-parent-id"))

        adapter.moved(1, 1, 0)

        // task.parent should be cleared
        assertEquals(0, taskDao.fetch(tasks[1].id)!!.parent)
        // remoteParent should still be the old value
        assertEquals("remote-parent-id", caldavDao.getTask(tasks[1].id)!!.remoteParent)
    }

    @Test
    fun reparentMicrosoftTaskPreservesRemoteParent() = runBlocking {
        addTask()
        addTask()
        addTask(with(PARENT, tasks[0]))
        // Set remoteParent to simulate synced state
        val child = caldavDao.getTask(tasks[2].id)!!
        caldavDao.update(child.copy(remoteParent = "old-parent-remote-id"))

        // Move child from under tasks[0] to under tasks[1]
        adapter.moved(2, 2, 1)

        assertEquals(tasks[1].id, taskDao.fetch(tasks[2].id)!!.parent)
        // remoteParent should still reflect the old synced state
        assertEquals("old-parent-remote-id", caldavDao.getTask(tasks[2].id)!!.remoteParent)
    }

    private fun addTask(vararg properties: PropertyValue<in TaskContainer?, *>) = runBlocking {
        val t = newTaskContainer(*properties)
        val task = t.task
        taskDao.createNew(task)
        val caldavTask = CaldavTask(task = t.id, calendar = "calendar")
        if (task.parent > 0) {
            caldavTask.remoteParent = caldavDao.getRemoteIdForTask(task.parent)
        }
        tasks.add(
            t.copy(
                accountType = TYPE_MICROSOFT,
                caldavTask = caldavTask.copy(
                    id = caldavDao.insert(caldavTask)
                )
            )
        )
    }
}
