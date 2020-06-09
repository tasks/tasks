package org.tasks.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.helper.UUIDHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import org.tasks.makers.TagDataMaker.newTagData
import org.tasks.makers.TagMaker.TAGDATA
import org.tasks.makers.TagMaker.TASK
import org.tasks.makers.TagMaker.newTag
import org.tasks.makers.TaskMaker.CREATION_TIME
import org.tasks.makers.TaskMaker.ID
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class CaldavDaoTests : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var tagDao: TagDao
    @Inject lateinit var tagDataDao: TagDataDao
    @Inject lateinit var caldavDao: CaldavDao

    @Test
    fun insertNewTaskAtTopOfEmptyList() {
        val task = newTask()
        taskDao.createNew(task)
        val caldavTask = CaldavTask(task.id, "calendar")
        caldavDao.insert(task, caldavTask, true)

        checkOrder(null, task.id)
    }

    @Test
    fun insertNewTaskAboveExistingTask() {
        val created = DateTime(2020, 5, 21, 15, 29, 16, 452)
        val first = newTask(with(CREATION_TIME, created))
        val second = newTask(with(CREATION_TIME, created.plusSeconds(1)))
        taskDao.createNew(first)
        taskDao.createNew(second)
        caldavDao.insert(first, CaldavTask(first.id, "calendar"), true)

        caldavDao.insert(second, CaldavTask(second.id, "calendar"), true)

        checkOrder(null, first.id)
        checkOrder(created.minusSeconds(1), second.id)
    }

    @Test
    fun insertNewTaskBelowExistingTask() {
        val created = DateTime(2020, 5, 21, 15, 29, 16, 452)
        val first = newTask(with(CREATION_TIME, created))
        val second = newTask(with(CREATION_TIME, created.plusSeconds(1)))
        taskDao.createNew(first)
        taskDao.createNew(second)
        caldavDao.insert(first, CaldavTask(first.id, "calendar"), false)

        caldavDao.insert(second, CaldavTask(second.id, "calendar"), false)

        checkOrder(null, first.id)
        checkOrder(null, second.id)
    }

    @Test
    fun insertNewTaskBelowExistingTaskWithSameCreationDate() {
        val created = DateTime(2020, 5, 21, 15, 29, 16, 452)
        val first = newTask(with(CREATION_TIME, created))
        val second = newTask(with(CREATION_TIME, created))
        taskDao.createNew(first)
        taskDao.createNew(second)
        caldavDao.insert(first, CaldavTask(first.id, "calendar"), false)

        caldavDao.insert(second, CaldavTask(second.id, "calendar"), false)

        checkOrder(null, first.id)
        checkOrder(created.plusSeconds(1), second.id)
    }

    @Test
    fun insertNewTaskAtBottomOfEmptyList() {
        val task = newTask()
        taskDao.createNew(task)
        val caldavTask = CaldavTask(task.id, "calendar")
        caldavDao.insert(task, caldavTask, false)

        checkOrder(null, task.id)
    }

    @Test
    fun getCaldavTasksWithTags() {
            val task = newTask(with(ID, 1L))
            taskDao.createNew(task)
            val one = newTagData()
            val two = newTagData()
            tagDataDao.createNew(one)
            tagDataDao.createNew(two)
            tagDao.insert(newTag(with(TASK, task), with(TAGDATA, one)))
            tagDao.insert(newTag(with(TASK, task), with(TAGDATA, two)))
            caldavDao.insert(CaldavTask(task.id, "calendar"))
            assertEquals(listOf(task.id), caldavDao.getTasksWithTags())
        }

    @Test
    fun ignoreNonCaldavTaskWithTags() {
        val task = newTask(with(ID, 1L))
        taskDao.createNew(task)
        val tag = newTagData()
        tagDataDao.createNew(tag)
        tagDao.insert(newTag(with(TASK, task), with(TAGDATA, tag)))
        assertTrue(caldavDao.getTasksWithTags().isEmpty())
    }

    @Test
    fun ignoreCaldavTaskWithoutTags() {
        val task = newTask(with(ID, 1L))
        taskDao.createNew(task)
        tagDataDao.createNew(newTagData())
        caldavDao.insert(CaldavTask(task.id, "calendar"))
        assertTrue(caldavDao.getTasksWithTags().isEmpty())
    }

    @Test
    fun noResultsForEmptyAccounts() {
        val caldavAccount = CaldavAccount()
        caldavAccount.uuid = UUIDHelper.newUUID()
        caldavDao.insert(caldavAccount)
        assertTrue(caldavDao.getCaldavFilters(caldavAccount.uuid!!).isEmpty())
    }

    private fun checkOrder(dateTime: DateTime, task: Long) = checkOrder(dateTime.toAppleEpoch(), task)

    private fun checkOrder(order: Long?, task: Long) {
        val sortOrder = caldavDao.getTask(task)!!.order
        if (order == null) {
            assertNull(sortOrder)
        } else {
            assertEquals(order, sortOrder)
        }
    }

    override fun inject(component: TestComponent) = component.inject(this)
}