package org.tasks.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.dao.TaskDao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.TestComponent
import org.tasks.makers.TagDataMaker.NAME
import org.tasks.makers.TagDataMaker.newTagData
import org.tasks.makers.TagMaker.TAGDATA
import org.tasks.makers.TagMaker.TAGUID
import org.tasks.makers.TagMaker.TASK
import org.tasks.makers.TagMaker.newTag
import org.tasks.makers.TaskMaker.ID
import org.tasks.makers.TaskMaker.newTask
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class TagDataDaoTest : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var tagDao: TagDao
    @Inject lateinit var tagDataDao: TagDataDao

    @Test
    fun tagDataOrderedByNameIgnoresNullNames() {
        tagDataDao.createNew(newTagData(with(NAME, null as String?)))
        assertTrue(tagDataDao.tagDataOrderedByName().isEmpty())
    }

    @Test
    fun tagDataOrderedByNameIgnoresEmptyNames() {
        tagDataDao.createNew(newTagData(with(NAME, "")))
        assertTrue(tagDataDao.tagDataOrderedByName().isEmpty())
    }

    @Test
    fun getTagWithCaseForMissingTag() {
        assertEquals("derp", tagDataDao.getTagWithCase("derp"))
    }

    @Test
    fun getTagWithCaseFixesCase() {
        tagDataDao.createNew(newTagData(with(NAME, "Derp")))
        assertEquals("Derp", tagDataDao.getTagWithCase("derp"))
    }

    @Test
    fun getTagsByName() {
        val tagData = newTagData(with(NAME, "Derp"))
        tagDataDao.createNew(tagData)
        assertEquals(listOf(tagData), tagDataDao.getTags(listOf("Derp")))
    }

    @Test
    fun getTagsByNameCaseSensitive() {
        tagDataDao.createNew(newTagData(with(NAME, "Derp")))
        assertTrue(tagDataDao.getTags(listOf("derp")).isEmpty())
    }

    @Test
    fun getTagDataForTask() {
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
    fun getEmptyTagSelections() {
        val selections = tagDataDao.getTagSelections(listOf(1L))
        assertTrue(selections.first!!.isEmpty())
        assertTrue(selections.second!!.isEmpty())
    }

    @Test
    fun getPartialTagSelections() {
        newTag(1, "tag1", "tag2")
        newTag(2, "tag2", "tag3")
        assertEquals(
                setOf("tag1", "tag3"), tagDataDao.getTagSelections(listOf(1L, 2L)).first)
    }

    @Test
    fun getEmptyPartialSelections() {
        newTag(1, "tag1")
        newTag(2, "tag1")
        assertTrue(tagDataDao.getTagSelections(listOf(1L, 2L)).first!!.isEmpty())
    }

    @Test
    fun getCommonTagSelections() {
        newTag(1, "tag1", "tag2")
        newTag(2, "tag2", "tag3")
        assertEquals(setOf("tag2"), tagDataDao.getTagSelections(listOf(1L, 2L)).second)
    }

    @Test
    fun getEmptyCommonSelections() {
        newTag(1, "tag1")
        newTag(2, "tag2")
        assertTrue(tagDataDao.getTagSelections(listOf(1L, 2L)).second!!.isEmpty())
    }

    @Test
    fun getSelectionsWithNoTags() {
        newTag(1)
        val selections = tagDataDao.getTagSelections(listOf(1L))
        assertTrue(selections.first!!.isEmpty())
        assertTrue(selections.second!!.isEmpty())
    }

    @Test
    fun noCommonSelectionsWhenOneTaskHasNoTags() {
        newTag(1, "tag1")
        newTag(2)
        val selections = tagDataDao.getTagSelections(listOf(1L, 2L))
        assertEquals(setOf("tag1"), selections.first)
        assertTrue(selections.second!!.isEmpty())
    }

    private fun newTag(taskId: Long, vararg tags: String) {
        val task = newTask(with(ID, taskId))
        taskDao.createNew(task)
        for (tag in tags) {
            tagDao.insert(newTag(with(TASK, task), with(TAGUID, tag)))
        }
    }

    override fun inject(component: TestComponent) = component.inject(this)
}