package org.tasks.data.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task

class CaldavDaoRemoteParentTest {
    private lateinit var db: Database
    private lateinit var taskDao: TaskDao
    private lateinit var caldavDao: CaldavDao

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder<Database>()
            .setDriver(BundledSQLiteDriver())
            .addCallback(Database.CALLBACK)
            .build()
        taskDao = db.taskDao()
        caldavDao = db.caldavDao()
        caldavDao.insert(CaldavAccount(uuid = ACCOUNT, accountType = TYPE_CALDAV))
        caldavDao.insert(CaldavCalendar(uuid = CALENDAR, account = ACCOUNT))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun newTask(remoteId: String): Long {
        val task = Task(remoteId = remoteId)
        taskDao.createNew(task)
        caldavDao.insert(
            CaldavTask(task = task.id, calendar = CALENDAR, remoteId = remoteId)
        )
        return task.id
    }

    private suspend fun remoteParentOf(task: Long): String? = caldavDao.getTask(task)?.remoteParent

    private fun account(type: Int) = CaldavAccount(uuid = ACCOUNT, accountType = type)

    @Test
    fun everyTaskInTheRunIsPointedAtTheSameParent() = runBlocking {
        val parent = newTask("parent")
        val moved = (0 until 3).map { newTask("child-$it") }

        caldavDao.setRemoteParent(moved, parent, account(TYPE_CALDAV), CALENDAR)

        moved.forEach { assertEquals("parent", remoteParentOf(it)) }
    }

    @Test
    fun aTaskThatWasNotMovedIsLeftAlone() = runBlocking {
        val parent = newTask("parent")
        val moved = newTask("moved")
        val other = newTask("other")

        caldavDao.setRemoteParent(listOf(moved), parent, account(TYPE_CALDAV), CALENDAR)

        assertEquals("parent", remoteParentOf(moved))
        assertNull(remoteParentOf(other))
    }

    @Test
    fun aRunLongerThanOneChunkIsWrittenInFull() = runBlocking {
        val parent = newTask("parent")
        val moved = (0 until 1200).map { newTask("child-$it") }

        caldavDao.setRemoteParent(moved, parent, account(TYPE_CALDAV), CALENDAR)

        assertEquals(1200, moved.count { remoteParentOf(it) == "parent" })
    }

    @Test
    fun nothingIsWrittenOnABackendThatDoesNotPushIt() = runBlocking {
        val parent = newTask("parent")
        val moved = newTask("moved")

        caldavDao.setRemoteParent(listOf(moved), parent, account(TYPE_MICROSOFT), CALENDAR)

        assertNull(remoteParentOf(moved))
    }

    @Test
    fun movingToTheTopLevelClearsWhatWasThere() = runBlocking {
        val parent = newTask("parent")
        val moved = newTask("moved")
        caldavDao.setRemoteParent(listOf(moved), parent, account(TYPE_CALDAV), CALENDAR)

        caldavDao.setRemoteParent(listOf(moved), 0, account(TYPE_CALDAV), CALENDAR)

        assertNull(remoteParentOf(moved))
    }

    @Test
    fun theRowLeftBehindByAListMoveIsNotRewritten() = runBlocking {
        val parent = newTask("parent")
        val moved = newTask("moved")
        caldavDao.insert(CaldavCalendar(uuid = OTHER_CALENDAR, account = ACCOUNT))
        val tombstone = CaldavTask(
            task = moved,
            calendar = OTHER_CALENDAR,
            remoteId = "moved-elsewhere",
        ).apply { deleted = 1_700_000_000_000L }
        caldavDao.insert(tombstone)

        caldavDao.setRemoteParent(listOf(moved), parent, account(TYPE_CALDAV), CALENDAR)

        assertEquals("parent", remoteParentOf(moved))
        assertNull(caldavDao.getTasks(moved).first { it.deleted > 0 }.remoteParent)
    }

    @Test
    fun aRowOnAnotherListIsNotRewritten() = runBlocking {
        val parent = newTask("parent")
        val moved = newTask("moved")
        caldavDao.insert(CaldavCalendar(uuid = OTHER_CALENDAR, account = ACCOUNT))
        caldavDao.insert(
            CaldavTask(task = moved, calendar = OTHER_CALENDAR, remoteId = "moved-elsewhere")
        )

        caldavDao.setRemoteParent(listOf(moved), parent, account(TYPE_CALDAV), CALENDAR)

        assertEquals("parent", remoteParentOf(moved))
        assertNull(
            caldavDao.getTasks(moved).first { it.calendar == OTHER_CALENDAR }.remoteParent
        )
    }

    companion object {
        private const val ACCOUNT = "account-1"
        private const val CALENDAR = "calendar-1"
        private const val OTHER_CALENDAR = "calendar-2"
    }
}
