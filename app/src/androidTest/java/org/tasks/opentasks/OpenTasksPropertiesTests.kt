package org.tasks.opentasks

import com.natpryce.makeiteasy.MakeItEasy.with
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.SuspendFreeze.Companion.freezeAt
import org.tasks.TestUtilities.withTZ
import org.tasks.caldav.iCalendar.Companion.collapsed
import org.tasks.caldav.iCalendar.Companion.order
import org.tasks.caldav.iCalendar.Companion.parent
import org.tasks.caldav.iCalendar.Companion.snooze
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.injection.ProductionModule
import org.tasks.makers.CaldavTaskMaker
import org.tasks.makers.CaldavTaskMaker.CALENDAR
import org.tasks.makers.CaldavTaskMaker.REMOTE_ID
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker
import org.tasks.makers.TaskMaker.COLLAPSED
import org.tasks.makers.TaskMaker.ORDER
import org.tasks.makers.TaskMaker.newTask
import org.tasks.time.DateTime
import java.util.TimeZone
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class OpenTasksPropertiesTests : OpenTasksTest() {

    @Inject lateinit var tagDataDao: TagDataDao
    @Inject lateinit var tagDao: TagDao
    @Inject lateinit var alarmDao: AlarmDao

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

        assertEquals("1234", openTaskDao.getTask(listId, "abcd")?.task?.parent)
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
        val tag = TagData(name = "Tag1").let { it.copy(id = tagDataDao.insert(it)) }

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

        val task = caldavDao.getTaskByRemoteId(list.uuid!!, "3076145036806467726")!!.task
        assertEquals(633734058L, taskDao.fetch(task)?.order)
    }

    @Test
    fun pushOrder() = runBlocking {
        val (listId, list) = openTaskDao.insertList()
        val task = newTask(with(ORDER, 5678L))
        taskDao.createNew(task)
        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(REMOTE_ID, "1234"),
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

    @Test
    fun readSnoozeTime() = runBlocking {
        val (_, list) = withVtodo(SNOOZED)

        withTZ(CHICAGO) {
            synchronizer.sync()
        }

        val task = caldavDao
                .getTaskByRemoteId(list.uuid!!, "4CBBC669-70E3-474D-A0A3-0FC42A14A5A5")
                ?.let { taskDao.fetch(it.task) }

        assertEquals(
            listOf(
                Alarm(
                    id = 1,
                    task = task!!.id,
                    time = 1612972355000,
                    type = TYPE_SNOOZE
                )
            ),
            alarmDao.getAlarms(task.id)
        )
    }

    @Test
    fun pushSnoozeTime() = withTZ(CHICAGO) {
        val (listId, list) = openTaskDao.insertList()
        val taskId = taskDao.createNew(newTask())
        alarmDao.insert(
            Alarm(
                task = taskId,
                time = DateTime(2021, 2, 4, 13, 30).millis,
                type = TYPE_SNOOZE
            )
        )

        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(CaldavTaskMaker.TASK, taskId),
                with(REMOTE_ID, "abcd")
        ))

        freezeAt(DateTime(2021, 2, 4, 12, 30, 45, 125)) {
            synchronizer.sync()
        }

        assertEquals(1612467000000, openTaskDao.getTask(listId, "abcd")?.task!!.snooze)
    }

    @Test
    fun dontPushLapsedSnoozeTime() = withTZ(CHICAGO) {
        val (listId, list) = openTaskDao.insertList()
        val taskId = taskDao.createNew(newTask())
        alarmDao.insert(
            Alarm(
                task = taskId,
                time = DateTime(2021, 2, 4, 13, 30).millis,
                type = TYPE_SNOOZE
            )
        )

        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(CaldavTaskMaker.TASK, taskId),
                with(REMOTE_ID, "abcd")
        ))

        freezeAt(DateTime(2021, 2, 4, 13, 30, 45, 125)) {
            synchronizer.sync()
        }

        assertNull(openTaskDao.getTask(listId, "abcd")?.task!!.snooze)
    }

    @Test
    fun removeSnoozeTime() = withTZ(CHICAGO) {
        val (listId, list) = withVtodo(SNOOZED)

        synchronizer.sync()

        val task = caldavDao.getTaskByRemoteId(list.uuid!!, "4CBBC669-70E3-474D-A0A3-0FC42A14A5A5")
            ?: throw IllegalStateException("Missing task")
        assertEquals(
            listOf(Alarm(1, task.id, DateTime(2021, 2, 10, 9, 52, 35).millis, TYPE_SNOOZE)),
            alarmDao.getAlarms(1)
        )
        alarmDao.deleteSnoozed(listOf(1))
        taskDao.touch(task.task)

        synchronizer.sync()

        assertNull(
                openTaskDao
                        .getTask(listId, "4CBBC669-70E3-474D-A0A3-0FC42A14A5A5")
                        ?.task
                !!.snooze
        )
    }

    private suspend fun insertTag(task: Task, name: String) =
        TagData(name = name)
            .apply { tagDataDao.insert(this) }
            .let { tagDao.insert(Tag(task = task.id, taskUid = task.uuid, tagUid = it.remoteId)) }

    companion object {
        private val CHICAGO = TimeZone.getTimeZone("America/Chicago")

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

        private val SNOOZED = """
            BEGIN:VCALENDAR
            PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN
            VERSION:2.0
            BEGIN:VTODO
            CREATED:20210210T151826Z
            LAST-MODIFIED:20210210T152235Z
            DTSTAMP:20210210T152235Z
            UID:4CBBC669-70E3-474D-A0A3-0FC42A14A5A5
            SUMMARY:Test snooze
            STATUS:NEEDS-ACTION
            X-MOZ-LASTACK:20210210T152235Z
            DTSTART;TZID=America/Chicago:20210210T091900
            DUE;TZID=America/Chicago:20210210T091900
            X-MOZ-SNOOZE-TIME:20210210T155235Z
            X-MOZ-GENERATION:1
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
    }
}