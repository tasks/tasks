@file:Suppress("ClassName")

package com.todoroo.astrid.service

import com.natpryce.makeiteasy.MakeItEasy.with
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertTrue
import org.tasks.TestUtilities.assertEquals
import org.tasks.caldav.VtodoCache
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.DirtyDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Task
import org.tasks.injection.InjectingTestCase
import org.tasks.makers.CaldavTaskMaker.CALENDAR
import org.tasks.makers.CaldavTaskMaker.REMOTE_ID
import org.tasks.makers.CaldavTaskMaker.TASK
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker.DUE_DATE
import org.tasks.makers.TaskMaker.HIDE_TYPE
import org.tasks.makers.TaskMaker.MODIFICATION_TIME
import org.tasks.makers.TaskMaker.newTask
import org.tasks.opentasks.TestOpenTaskDao
import org.tasks.time.DateTime
import javax.inject.Inject

@HiltAndroidTest
class Upgrade_11_3_Test : InjectingTestCase() {
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var dirtyDao: DirtyDao
    @Inject lateinit var openTaskDao: TestOpenTaskDao
    @Inject lateinit var upgrader: Upgrade_11_3
    @Inject lateinit var vtodoCache: VtodoCache

    private lateinit var calendar: CaldavCalendar

    @Before
    override fun setUp() {
        super.setUp()
        calendar = CaldavCalendar(account = "account", uuid = "calendar")
        runBlocking {
            caldavDao.insert(CaldavAccount(uuid = "account", accountType = TYPE_CALDAV))
            caldavDao.insert(calendar)
        }
    }

    @Test
    fun applyRemoteiCalendarStartDate() = runBlocking {
        val taskId = taskDao.insert(newTask())
        val caldavTask = newCaldavTask(with(TASK, taskId), with(CALENDAR, calendar.uuid))
        caldavDao.insert(caldavTask)
        vtodoCache.putVtodo(calendar, caldavTask, VTODO_WITH_START_DATE)

        upgrader.applyiCalendarStartDates()

        assertEquals(DateTime(2021, 1, 21), taskDao.fetch(taskId)?.hideUntil)
    }

    @Test
    fun ignoreRemoteiCalendarStartDate() = runBlocking {
        val taskId = taskDao.insert(newTask(
                with(DUE_DATE, DateTime(2021, 1, 20)),
                with(HIDE_TYPE, Task.HIDE_UNTIL_DUE)
        ))
        val caldavTask = newCaldavTask(with(TASK, taskId), with(CALENDAR, calendar.uuid))
        caldavDao.insert(caldavTask)
        vtodoCache.putVtodo(calendar, caldavTask, VTODO_WITH_START_DATE)

        upgrader.applyiCalendarStartDates()

        assertEquals(DateTime(2021, 1, 20), taskDao.fetch(taskId)?.hideUntil)
    }

    @Test
    fun touchTaskWithLocaliCalendarStartDate() = runBlocking {
        val taskId = taskDao.insert(newTask(
                with(DUE_DATE, DateTime(2021, 1, 20)),
                with(HIDE_TYPE, Task.HIDE_UNTIL_DUE),
                with(MODIFICATION_TIME, DateTime(2021, 1, 21, 9, 50, 4, 348))
        ))
        val caldavTask = newCaldavTask(with(TASK, taskId), with(CALENDAR, calendar.uuid))
        val caldavTaskId = caldavDao.insert(caldavTask)
        dirtyDao.markSynced(caldavTaskId)
        vtodoCache.putVtodo(calendar, caldavTask, VTODO_WITH_START_DATE)

        upgrader.applyiCalendarStartDates()

        assertTrue(dirtyDao.isDirty(caldavTaskId) == true)
    }

    @Test
    fun dontTouchWhenNoiCalendarStartDate() = runBlocking {
        val taskId = taskDao.insert(newTask())
        val caldavTask = newCaldavTask(with(TASK, taskId), with(CALENDAR, calendar.uuid))
        val caldavTaskId = caldavDao.insert(caldavTask)
        dirtyDao.markSynced(caldavTaskId)
        vtodoCache.putVtodo(calendar, caldavTask, VTODO_NO_START_DATE)

        upgrader.applyiCalendarStartDates()

        assertTrue(dirtyDao.isDirty(caldavTaskId) == false)
    }

    @Test
    fun applyRemoteOpenTaskStartDate() = runBlocking {
        val (listId, list) = openTaskDao.insertList()
        openTaskDao.insertTask(listId, VTODO_WITH_START_DATE)
        val taskId = taskDao.insert(newTask())
        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(REMOTE_ID, "4586964443060640060"),
                with(TASK, taskId)
        ))

        upgrader.applyOpenTaskStartDates()

        assertEquals(DateTime(2021, 1, 21), taskDao.fetch(taskId)?.hideUntil)
    }

    @Test
    fun ignoreRemoteOpenTaskStartDate() = runBlocking {
        val (listId, list) = openTaskDao.insertList()
        openTaskDao.insertTask(listId, VTODO_WITH_START_DATE)
        val taskId = taskDao.insert(newTask(
                with(DUE_DATE, DateTime(2021, 1, 20)),
                with(HIDE_TYPE, Task.HIDE_UNTIL_DUE)
        ))
        caldavDao.insert(newCaldavTask(
                with(CALENDAR, list.uuid),
                with(REMOTE_ID, "4586964443060640060"),
                with(TASK, taskId)
        ))

        upgrader.applyOpenTaskStartDates()

        assertEquals(DateTime(2021, 1, 20), taskDao.fetch(taskId)?.hideUntil)
    }

    @Test
    fun touchWithOpenTaskStartDate() = runBlocking {
        val (listId, list) = openTaskDao.insertList()
        caldavDao.insert(CaldavAccount(uuid = list.account!!, accountType = TYPE_OPENTASKS))
        openTaskDao.insertTask(listId, VTODO_WITH_START_DATE)
        val taskId = taskDao.insert(newTask(
                with(DUE_DATE, DateTime(2021, 1, 20)),
                with(HIDE_TYPE, Task.HIDE_UNTIL_DUE),
                with(MODIFICATION_TIME, DateTime(2021, 1, 21, 9, 50, 4, 348))
        ))
        val caldavTask = newCaldavTask(
                with(CALENDAR, list.uuid),
                with(REMOTE_ID, "4586964443060640060"),
                with(TASK, taskId)
        )
        val caldavTaskId = caldavDao.insert(caldavTask)
        dirtyDao.markSynced(caldavTaskId)

        upgrader.applyOpenTaskStartDates()

        assertTrue(dirtyDao.isDirty(caldavTaskId) == true)
    }

    @Test
    fun dontTouchNoOpenTaskStartDate() = runBlocking {
        val (listId, list) = openTaskDao.insertList()
        caldavDao.insert(CaldavAccount(uuid = list.account!!, accountType = TYPE_OPENTASKS))
        openTaskDao.insertTask(listId, VTODO_NO_START_DATE)
        val taskId = taskDao.insert(newTask())
        val caldavTask = newCaldavTask(
                with(CALENDAR, list.uuid),
                with(REMOTE_ID, "4586964443060640060"),
                with(TASK, taskId)
        )
        val caldavTaskId = caldavDao.insert(caldavTask)
        dirtyDao.markSynced(caldavTaskId)

        upgrader.applyOpenTaskStartDates()

        assertTrue(dirtyDao.isDirty(caldavTaskId) == false)
    }

    companion object {
        val VTODO_WITH_START_DATE = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:+//IDN tasks.org//android-110301//EN
            BEGIN:VTODO
            DTSTAMP:20210121T153032Z
            UID:4586964443060640060
            CREATED:20210121T153000Z
            LAST-MODIFIED:20210121T153029Z
            SUMMARY:Test
            PRIORITY:9
            X-APPLE-SORT-ORDER:-27
            DTSTART;VALUE=DATE:20210121
            END:VTODO
            END:VCALENDAR
        """.trimIndent()

        val VTODO_NO_START_DATE = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:+//IDN tasks.org//android-110301//EN
            BEGIN:VTODO
            DTSTAMP:20210121T153032Z
            UID:4586964443060640060
            CREATED:20210121T153000Z
            LAST-MODIFIED:20210121T153029Z
            SUMMARY:Test
            PRIORITY:9
            X-APPLE-SORT-ORDER:-27
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
    }
}