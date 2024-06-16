package org.tasks.data

import com.natpryce.makeiteasy.MakeItEasy.with
import org.tasks.filters.GtasksFilter
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.tasks.R
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.CaldavTaskMaker.CALENDAR
import org.tasks.makers.CaldavTaskMaker.TASK
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker
import org.tasks.makers.TaskMaker.ID
import org.tasks.makers.TaskMaker.ORDER
import org.tasks.makers.TaskMaker.PARENT
import org.tasks.preferences.Preferences
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class ManualGoogleTaskQueryTest : InjectingTestCase() {
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var preferences: Preferences
    private lateinit var filter: GtasksFilter

    @Before
    override fun setUp() {
        super.setUp()
        preferences.clear()
        preferences.setBoolean(R.string.p_manual_sort, true)
        val calendar = CaldavCalendar(uuid = "1234")
        runBlocking {
            caldavDao.insert(CaldavAccount())
            caldavDao.insert(calendar)
        }
        filter = GtasksFilter(calendar)
    }

    @Test
    fun setIndentOnSubtask() = runBlocking {
        newTask(1, 0, 0)
        newTask(2, 0, 1)

        val subtask = query()[1]

        assertEquals(1, subtask.indent)
    }

    @Test
    fun setParentOnSubtask() = runBlocking {
        newTask(2, 0, 0)
        newTask(1, 0, 2)

        val subtask = query()[1]

        assertEquals(2, subtask.parent)
    }

    @Test
    fun querySetsPrimarySort() = runBlocking {
        newTask(1, 0, 0)
        newTask(2, 1, 0)
        newTask(3, 0, 2)

        val subtasks = query()

        assertEquals(0, subtasks[0].primarySort)
        assertEquals(1, subtasks[1].primarySort)
        assertEquals(1, subtasks[2].primarySort)
    }

    @Test
    fun querySetsSecondarySortOnSubtasks() = runBlocking {
        newTask(1, 0, 0)
        newTask(2, 0, 1)
        newTask(3, 1, 1)

        val subtasks = query()

        assertEquals(0, subtasks[0].secondarySort)
        assertEquals(0, subtasks[1].secondarySort)
        assertEquals(1, subtasks[2].secondarySort)
    }

    private suspend fun newTask(id: Long, order: Long, parent: Long = 0) {
        taskDao.insert(TaskMaker.newTask(
            with(ID, id),
            with(TaskMaker.UUID, UUIDHelper.newUUID()),
            with(ORDER, order),
            with(PARENT, parent),
        ))
        googleTaskDao.insert(newCaldavTask(with(CALENDAR, filter.list.uuid), with(TASK, id)))
    }

    private suspend fun query(): List<TaskContainer> = taskDao.fetchTasks {
        TaskListQuery.getQuery(preferences, filter)
    }
}