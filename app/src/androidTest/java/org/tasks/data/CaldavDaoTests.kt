package org.tasks.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.helper.UUIDHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import org.tasks.makers.TagDataMaker.newTagData
import org.tasks.makers.TagMaker.TAGDATA
import org.tasks.makers.TagMaker.TASK
import org.tasks.makers.TagMaker.newTag
import org.tasks.makers.TaskMaker.ID
import org.tasks.makers.TaskMaker.newTask
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class CaldavDaoTests : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var tagDao: TagDao
    @Inject lateinit var tagDataDao: TagDataDao
    @Inject lateinit var caldavDao: CaldavDao

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
        assertTrue(caldavDao.getCaldavFilters(caldavAccount.uuid!!, DateUtilities.now()).isEmpty())
    }

    override fun inject(component: TestComponent) = component.inject(this)
}