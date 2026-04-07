package org.tasks.data.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task

class CaldavDaoMicrosoftTests {
    private lateinit var db: Database
    private lateinit var taskDao: TaskDao
    private lateinit var caldavDao: CaldavDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<Database>()
            .setDriver(BundledSQLiteDriver())
            .build()
        taskDao = db.taskDao()
        caldavDao = db.caldavDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun insertCaldavTask(
        remoteId: String,
        remoteParent: String? = null,
        lastSync: Long = 1L,
        deleted: Long = 0L,
        calendar: String = CALENDAR,
    ) {
        val task = Task()
        taskDao.createNew(task)
        caldavDao.insert(
            CaldavTask(
                task = task.id,
                calendar = calendar,
                remoteId = remoteId,
                remoteParent = remoteParent,
                lastSync = lastSync,
                deleted = deleted,
            )
        )
    }

    @Test
    fun excludesSubtasks() = runBlocking {
        insertCaldavTask(remoteId = "parent-1")
        insertCaldavTask(remoteId = "child-1", remoteParent = "parent-1")

        assertEquals(listOf("parent-1"), caldavDao.getTopLevelRemoteIds(CALENDAR))
    }

    @Test
    fun includesNullRemoteParent() = runBlocking {
        insertCaldavTask(remoteId = "task-1", remoteParent = null)

        assertEquals(listOf("task-1"), caldavDao.getTopLevelRemoteIds(CALENDAR))
    }

    @Test
    fun includesEmptyRemoteParent() = runBlocking {
        insertCaldavTask(remoteId = "task-1", remoteParent = "")

        assertEquals(listOf("task-1"), caldavDao.getTopLevelRemoteIds(CALENDAR))
    }

    @Test
    fun excludesDeleted() = runBlocking {
        insertCaldavTask(remoteId = "deleted-1", deleted = 1L)

        assertTrue(caldavDao.getTopLevelRemoteIds(CALENDAR).isEmpty())
    }

    @Test
    fun excludesUnsynced() = runBlocking {
        insertCaldavTask(remoteId = "unsynced-1", lastSync = 0L)

        assertTrue(caldavDao.getTopLevelRemoteIds(CALENDAR).isEmpty())
    }

    @Test
    fun filtersPerCalendar() = runBlocking {
        insertCaldavTask(remoteId = "a-1", calendar = "calendar-a")
        insertCaldavTask(remoteId = "b-1", calendar = "calendar-b")

        assertEquals(listOf("a-1"), caldavDao.getTopLevelRemoteIds("calendar-a"))
        assertEquals(listOf("b-1"), caldavDao.getTopLevelRemoteIds("calendar-b"))
    }

    companion object {
        private const val CALENDAR = "test-calendar"
    }
}
