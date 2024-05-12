package org.tasks.data

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.dao.TaskDao
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import org.tasks.makers.TagDataMaker.NAME
import org.tasks.makers.TagDataMaker.newTagData
import org.tasks.makers.TagMaker.TAGDATA
import org.tasks.makers.TagMaker.TAGUID
import org.tasks.makers.TagMaker.TASK
import org.tasks.makers.TagMaker.newTag
import org.tasks.makers.TaskMaker.ID
import org.tasks.makers.TaskMaker.newTask
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class TagDataDaoTest : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var tagDao: TagDao
    @Inject lateinit var tagDataDao: TagDataDao

    @Test
    fun tagDataOrderedByNameIgnoresNullNames() = runBlocking {
        tagDataDao.createNew(newTagData(with(NAME, null as String?)))
        assertTrue(tagDataDao.tagDataOrderedByName().isEmpty())
    }

    @Test
    fun tagDataOrderedByNameIgnoresEmptyNames() = runBlocking {
        tagDataDao.createNew(newTagData(with(NAME, "")))
        assertTrue(tagDataDao.tagDataOrderedByName().isEmpty())
    }

    @Test
    fun getTagWithCaseForMissingTag() = runBlocking {
        assertEquals("derp", tagDataDao.getTagWithCase("derp"))
    }

    @Test
    fun getTagWithCaseFixesCase() = runBlocking {
        tagDataDao.createNew(newTagData(with(NAME, "Derp")))
        assertEquals("Derp", tagDataDao.getTagWithCase("derp"))
    }

    @Test
    fun getTagsByName() = runBlocking {
        val tagData = newTagData(with(NAME, "Derp"))
        tagDataDao.createNew(tagData)
        assertEquals(listOf(tagData), tagDataDao.getTags(listOf("Derp")))
    }

    @Test
    fun getTagsByNameCaseSensitive() = runBlocking {
        tagDataDao.createNew(newTagData(with(NAME, "Derp")))
        assertTrue(tagDataDao.getTags(listOf("derp")).isEmpty())
    }

    @Test
    fun getTagDataForTask() = runBlocking {
        val taskOne = newTask()
        val taskTwo = newTask()
        taskDao.createNew(taskOne)
        taskDao.createNew(taskTwo)
        val tagOne = newTagData(with(NAME, "one"))
        val tagTwo = newTagData(with(NAME, "two"))
        tagDataDao.createNew(tagOne)
        tagDataDao.createNew(tagTwo)
        tagDao.insert(newTag(with(TAGDATA, tagOne), with(TASK, taskOne)))
        tagDao.insert(newTag(with(TAGDATA, tagTwo), with(TASK, taskTwo)))
        assertEquals(listOf(tagOne), tagDataDao.getTagDataForTask(taskOne.id))
    }

    @Test
    fun getEmptyTagSelections() = runBlocking {
        val selections = tagDataDao.getTagSelections(listOf(1L))
        assertTrue(selections.first!!.isEmpty())
        assertTrue(selections.second!!.isEmpty())
    }

    @Test
    fun getPartialTagSelections() = runBlocking {
        newTag(1, "tag1", "tag2")
        newTag(2, "tag2", "tag3")
        assertEquals(
                setOf("tag1", "tag3"), tagDataDao.getTagSelections(listOf(1L, 2L)).first)
    }

    @Test
    fun getEmptyPartialSelections() = runBlocking {
        newTag(1, "tag1")
        newTag(2, "tag1")
        assertTrue(tagDataDao.getTagSelections(listOf(1L, 2L)).first!!.isEmpty())
    }

    @Test
    fun getCommonTagSelections() = runBlocking {
        newTag(1, "tag1", "tag2")
        newTag(2, "tag2", "tag3")
        assertEquals(setOf("tag2"), tagDataDao.getTagSelections(listOf(1L, 2L)).second)
    }

    @Test
    fun getEmptyCommonSelections() = runBlocking {
        newTag(1, "tag1")
        newTag(2, "tag2")
        assertTrue(tagDataDao.getTagSelections(listOf(1L, 2L)).second!!.isEmpty())
    }

    @Test
    fun getSelectionsWithNoTags() = runBlocking {
        newTag(1)
        val selections = tagDataDao.getTagSelections(listOf(1L))
        assertTrue(selections.first!!.isEmpty())
        assertTrue(selections.second!!.isEmpty())
    }

    @Test
    fun noCommonSelectionsWhenOneTaskHasNoTags() = runBlocking {
        newTag(1, "tag1")
        newTag(2)
        val selections = tagDataDao.getTagSelections(listOf(1L, 2L))
        assertEquals(setOf("tag1"), selections.first)
        assertTrue(selections.second!!.isEmpty())
    }

    private suspend fun newTag(taskId: Long, vararg tags: String) {
        val task = newTask(with(ID, taskId))
        taskDao.createNew(task)
        for (tag in tags) {
            tagDao.insert(newTag(with(TASK, task), with(TAGUID, tag)))
        }
    }
}