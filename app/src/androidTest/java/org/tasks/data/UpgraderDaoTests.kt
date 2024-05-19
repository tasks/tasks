package org.tasks.data

import com.natpryce.makeiteasy.MakeItEasy
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.UpgraderDao
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
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
        val one = TagData()
        val two = TagData()
        tagDataDao.insert(one)
        tagDataDao.insert(two)
        tagDao.insert(Tag(task = task.id, taskUid = task.uuid, tagUid = one.remoteId))
        tagDao.insert(Tag(task = task.id, taskUid = task.uuid, tagUid = two.remoteId))
        caldavDao.insert(CaldavTask(task = task.id, calendar = "calendar"))
        assertEquals(listOf(task.id), upgraderDao.tasksWithTags())
    }

    @Test
    fun ignoreNonCaldavTaskWithTags() = runBlocking {
        val task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.ID, 1L))
        taskDao.createNew(task)
        val tag = TagData()
        tagDataDao.insert(tag)
        tagDao.insert(Tag(task = task.id, taskUid = task.uuid, tagUid = tag.remoteId))
        assertTrue(upgraderDao.tasksWithTags().isEmpty())
    }

    @Test
    fun ignoreCaldavTaskWithoutTags() = runBlocking {
        val task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.ID, 1L))
        taskDao.createNew(task)
        tagDataDao.insert(TagData())
        caldavDao.insert(CaldavTask(task = task.id, calendar = "calendar"))
        assertTrue(upgraderDao.tasksWithTags().isEmpty())
    }
}
