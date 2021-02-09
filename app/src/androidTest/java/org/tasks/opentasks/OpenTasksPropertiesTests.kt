package org.tasks.opentasks

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.data.Task
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.tasks.caldav.iCalendar.Companion.collapsed
import org.tasks.caldav.iCalendar.Companion.getParent
import org.tasks.caldav.iCalendar.Companion.order
import org.tasks.data.TagDao
import org.tasks.data.TagDataDao
import org.tasks.injection.ProductionModule
import org.tasks.makers.CaldavTaskMaker
import org.tasks.makers.CaldavTaskMaker.CALENDAR
import org.tasks.makers.CaldavTaskMaker.REMOTE_ID
import org.tasks.makers.CaldavTaskMaker.REMOTE_ORDER
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TagDataMaker.NAME
import org.tasks.makers.TagDataMaker.newTagData
import org.tasks.makers.TagMaker.TAGDATA
import org.tasks.makers.TagMaker.TASK
import org.tasks.makers.TagMaker.newTag
import org.tasks.makers.TaskMaker
import org.tasks.makers.TaskMaker.COLLAPSED
import org.tasks.makers.TaskMaker.newTask
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class OpenTasksPropertiesTests : OpenTasksTest() {

    @Inject lateinit var tagDataDao: TagDataDao
    @Inject lateinit var tagDao: TagDao

    @Test
    fun loadRemoteParentInfo() = runBlocking {
        val (_, list) = withVtodo(SUBTASK)

        synchronizer.sync()

        val task = caldavDao.getTaskByRemoteId(list.uuid!!, "dfede1b0-435b-4bba-9708-2422e781747c")
        assertEquals("7daa4a5c-cc76-4ddf-b4f8-b9d3a9cb00e7", task?.remoteParent)
    }

    @Test
    fun pushParentInfo() = runBlocking {
        val (listId, list) = openTaskDao.insertList()
        val taskId = taskDao.createNew(newTask(with(TaskMaker.PARENT, 594)))

        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(CaldavTaskMaker.TASK, taskId),
                with(REMOTE_ID, "abcd"),
                with(CaldavTaskMaker.REMOTE_PARENT, "1234")
        ))

        synchronizer.sync()

        assertEquals("1234", openTaskDao.getTask(listId, "abcd")?.task?.getParent())
    }

    @Test
    fun createNewTags() = runBlocking {
        val (_, list) = withVtodo(TWO_TAGS)

        synchronizer.sync()

        assertEquals(
                setOf("Tag1", "Tag2"),
                caldavDao.getTaskByRemoteId(list.uuid!!, "3076145036806467726")
                        ?.task
                        ?.let { tagDao.getTagsForTask(it) }
                        ?.map { it.name }
                        ?.toSet()
        )
    }

    @Test
    fun matchExistingTag() = runBlocking {
        val (_, list) = withVtodo(ONE_TAG)
        val tag = newTagData(with(NAME, "Tag1"))
        tagDataDao.createNew(tag)

        synchronizer.sync()

        assertEquals(
                listOf(tag),
                caldavDao.getTaskByRemoteId(list.uuid!!, "3076145036806467726")
                        ?.task
                        ?.let { tagDataDao.getTagDataForTask(it)}
        )
    }

    @Test
    fun uploadTags() = runBlocking {
        val (listId, list) = openTaskDao.insertList()
        val task = newTask().apply { taskDao.createNew(this) }
        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(REMOTE_ID, "1234"),
                with(CaldavTaskMaker.TASK, task.id)
        ))
        insertTag(task, "Tag1")
        insertTag(task, "Tag2")

        synchronizer.sync()

        assertEquals(
                setOf("Tag1", "Tag2"),
                openTaskDao.getTask(listId, "1234")?.task?.categories?.toSet()
        )
    }

    @Test
    fun loadOrder() = runBlocking {
        val (_, list) = withVtodo(ONE_TAG)

        synchronizer.sync()

        assertEquals(
                633734058L,
                caldavDao.getTaskByRemoteId(list.uuid!!, "3076145036806467726")?.order
        )
    }

    @Test
    fun pushOrder() = runBlocking {
        val (listId, list) = openTaskDao.insertList()
        val task = newTask().apply { taskDao.createNew(this) }
        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(REMOTE_ID, "1234"),
                with(REMOTE_ORDER, 5678L),
                with(CaldavTaskMaker.TASK, task.id)
        ))

        synchronizer.sync()

        assertEquals(
                5678L,
                openTaskDao.getTask(listId, "1234")?.task?.order
        )
    }

    @Test
    fun readCollapsedState() = runBlocking {
        val (_, list) = withVtodo(HIDE_SUBTASKS)

        synchronizer.sync()

        val task = caldavDao
                .getTaskByRemoteId(list.uuid!!, "2822976a-b71e-4962-92e4-db7297789c20")
                ?.let { taskDao.fetch(it.task) }
        assertTrue(task!!.isCollapsed)
    }

    @Test
    fun pushCollapsedState() = runBlocking {
        val (listId, list) = openTaskDao.insertList()
        val taskId = taskDao.createNew(newTask(with(COLLAPSED, true)))

        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(CaldavTaskMaker.TASK, taskId),
                with(REMOTE_ID, "abcd")
        ))

        synchronizer.sync()

        assertTrue(openTaskDao.getTask(listId, "abcd")?.task!!.collapsed)
    }

    @Test
    fun removeCollapsedState() = runBlocking {
        val (listId, list) = withVtodo(HIDE_SUBTASKS)

        synchronizer.sync()

        val task = caldavDao.getTaskByRemoteId(list.uuid!!, "2822976a-b71e-4962-92e4-db7297789c20")

        taskDao.setCollapsed(task!!.task, false)

        synchronizer.sync()

        assertFalse(
                openTaskDao
                        .getTask(listId, "2822976a-b71e-4962-92e4-db7297789c20")
                        ?.task
                        !!.collapsed
        )
    }

    private suspend fun insertTag(task: Task, name: String) =
            newTagData(with(NAME, name))
                    .apply { tagDataDao.createNew(this) }
                    .let { tagDao.insert(newTag(with(TASK, task), with(TAGDATA, it))) }

    companion object {
        private val SUBTASK = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Nextcloud Tasks v0.13.6
            BEGIN:VTODO
            UID:dfede1b0-435b-4bba-9708-2422e781747c
            CREATED:20210128T150333
            LAST-MODIFIED:20210128T150338
            DTSTAMP:20210128T150338
            SUMMARY:Child
            RELATED-TO:7daa4a5c-cc76-4ddf-b4f8-b9d3a9cb00e7
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        private val ONE_TAG = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:+//IDN tasks.org//android-110304//EN
            BEGIN:VTODO
            DTSTAMP:20210201T204211Z
            UID:3076145036806467726
            CREATED:20210201T204143Z
            LAST-MODIFIED:20210201T204209Z
            SUMMARY:Tags
            CATEGORIES:Tag1
            X-APPLE-SORT-ORDER:633734058
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        private val TWO_TAGS = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:+//IDN tasks.org//android-110304//EN
            BEGIN:VTODO
            DTSTAMP:20210201T204211Z
            UID:3076145036806467726
            CREATED:20210201T204143Z
            LAST-MODIFIED:20210201T204209Z
            SUMMARY:Tags
            CATEGORIES:Tag1,Tag2
            X-APPLE-SORT-ORDER:633734058
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        private val HIDE_SUBTASKS = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Nextcloud Tasks v0.13.6
            BEGIN:VTODO
            UID:2822976a-b71e-4962-92e4-db7297789c20
            CREATED:20210209T104536
            LAST-MODIFIED:20210209T104548
            DTSTAMP:20210209T104548
            SUMMARY:Parent
            X-OC-HIDESUBTASKS:1
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
    }
}