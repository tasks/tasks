package org.tasks.data

import com.natpryce.makeiteasy.MakeItEasy
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.TagDataMaker
import org.tasks.makers.TagMaker
import org.tasks.makers.TaskMaker
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class UpgraderDaoTests : InjectingTestCase() {

    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var tagDao: TagDao
    @Inject lateinit var tagDataDao: TagDataDao
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var upgraderDao: UpgraderDao

    @Test
    fun getCaldavTasksWithTags() = runBlocking {
        val task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.ID, 1L))
        taskDao.createNew(task)
        val one = TagDataMaker.newTagData()
        val two = TagDataMaker.newTagData()
        tagDataDao.createNew(one)
        tagDataDao.createNew(two)
        tagDao.insert(TagMaker.newTag(MakeItEasy.with(TagMaker.TASK, task), MakeItEasy.with(TagMaker.TAGDATA, one)))
        tagDao.insert(TagMaker.newTag(MakeItEasy.with(TagMaker.TASK, task), MakeItEasy.with(TagMaker.TAGDATA, two)))
        caldavDao.insert(CaldavTask(task = task.id, calendar = "calendar"))
        assertEquals(listOf(task.id), upgraderDao.tasksWithTags())
    }

    @Test
    fun ignoreNonCaldavTaskWithTags() = runBlocking {
        val task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.ID, 1L))
        taskDao.createNew(task)
        val tag = TagDataMaker.newTagData()
        tagDataDao.createNew(tag)
        tagDao.insert(TagMaker.newTag(MakeItEasy.with(TagMaker.TASK, task), MakeItEasy.with(TagMaker.TAGDATA, tag)))
        assertTrue(upgraderDao.tasksWithTags().isEmpty())
    }

    @Test
    fun ignoreCaldavTaskWithoutTags() = runBlocking {
        val task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.ID, 1L))
        taskDao.createNew(task)
        tagDataDao.createNew(TagDataMaker.newTagData())
        caldavDao.insert(CaldavTask(task = task.id, calendar = "calendar"))
        assertTrue(upgraderDao.tasksWithTags().isEmpty())
    }
}
