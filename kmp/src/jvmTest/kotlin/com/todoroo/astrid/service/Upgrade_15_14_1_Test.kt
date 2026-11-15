package com.todoroo.astrid.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.DatabaseTest
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task

class Upgrade_15_14_1_Test : DatabaseTest() {
    private val caldavDao = db.caldavDao()
    private val upgrade = Upgrade_15_14_1(caldavDao)

    private suspend fun calendarWithTask(type: Int, ctag: String? = "ctag", name: String = "$type"): Pair<String, Long> {
        val account = CaldavAccount(uuid = "account-$name", accountType = type)
        caldavDao.insert(account)
        val calendar = CaldavCalendar(account = account.uuid, uuid = "calendar-$name", ctag = ctag)
        caldavDao.insert(calendar)
        val task = Task()
        db.taskDao().createNew(task)
        caldavDao.insert(CaldavTask(task = task.id, calendar = calendar.uuid, remoteId = "r$name", etag = "etag"))
        return calendar.uuid!! to task.id
    }

    private suspend fun ctag(calendar: String) = caldavDao.getCalendarByUuid(calendar)!!.ctag

    private suspend fun etag(task: Long) = caldavDao.getTask(task)!!.etag

    @Test
    fun clearsETagsAndCTagsOfCaldavAccounts() = runBlocking {
        val (caldav, caldavTask) = calendarWithTask(TYPE_CALDAV)
        val (tasksOrg, tasksOrgTask) = calendarWithTask(TYPE_TASKS)

        upgrade.downloadAgain()

        assertEquals(Upgrade_15_14_1.CTAG, ctag(caldav))
        assertNull(etag(caldavTask))
        assertEquals(Upgrade_15_14_1.CTAG, ctag(tasksOrg))
        assertNull(etag(tasksOrgTask))
    }

    @Test
    fun leavesMissingAndBlankCTagsAlone() = runBlocking {
        val (caldavNull, caldavNullTask) = calendarWithTask(TYPE_CALDAV, ctag = null, name = "caldav-null")
        val (caldavBlank, caldavBlankTask) = calendarWithTask(TYPE_CALDAV, ctag = "", name = "caldav-blank")
        val (tasksNull, tasksNullTask) = calendarWithTask(TYPE_TASKS, ctag = null, name = "tasks-null")
        val (tasksBlank, tasksBlankTask) = calendarWithTask(TYPE_TASKS, ctag = "", name = "tasks-blank")

        upgrade.downloadAgain()

        assertNull(ctag(caldavNull))
        assertEquals("", ctag(caldavBlank))
        assertNull(ctag(tasksNull))
        assertEquals("", ctag(tasksBlank))
        for (task in listOf(caldavNullTask, caldavBlankTask, tasksNullTask, tasksBlankTask)) {
            assertNull(etag(task))
        }
    }

    @Test
    fun leavesOtherAccountTypesAlone() = runBlocking {
        val (openTasks, openTasksTask) = calendarWithTask(TYPE_OPENTASKS)
        val (etebase, etebaseTask) = calendarWithTask(TYPE_ETEBASE)
        val (google, googleTask) = calendarWithTask(TYPE_GOOGLE_TASKS)
        val (microsoft, microsoftTask) = calendarWithTask(TYPE_MICROSOFT)
        val (local, localTask) = calendarWithTask(TYPE_LOCAL)

        upgrade.downloadAgain()

        for ((calendar, task) in listOf(
            openTasks to openTasksTask,
            etebase to etebaseTask,
            google to googleTask,
            microsoft to microsoftTask,
            local to localTask,
        )) {
            assertEquals("ctag", ctag(calendar))
            assertEquals("etag", etag(task))
        }
    }

    @Test
    fun changesNothingButETagsAndCTags() = runBlocking {
        val (calendar, taskId) = calendarWithTask(TYPE_CALDAV)
        val caldavTask = caldavDao.getTask(taskId)!!
        db.dirtyDao().setDirtyState(caldavTask.id, dirtyVersion = 3, syncedVersion = 2)
        val deleted = Task()
        db.taskDao().createNew(deleted)
        caldavDao.insert(CaldavTask(task = deleted.id, calendar = calendar, remoteId = "deleted", etag = "etag", deleted = 1))
        val tasks = db.taskDao().getAll()
        val calendarRow = caldavDao.getCalendarByUuid(calendar)!!
        val dirty = db.dirtyDao().isDirty(caldavTask.id)
        assertEquals(true, dirty)

        upgrade.downloadAgain()

        assertEquals(tasks, db.taskDao().getAll())
        assertEquals(calendarRow.copy(ctag = Upgrade_15_14_1.CTAG), caldavDao.getCalendarByUuid(calendar))
        assertEquals(caldavTask.copy(etag = null), caldavDao.getTask(taskId))
        assertEquals(dirty, db.dirtyDao().isDirty(caldavTask.id))
        assertEquals(listOf(caldavTask.copy(etag = null)), caldavDao.getTasks(taskId))
    }
}
