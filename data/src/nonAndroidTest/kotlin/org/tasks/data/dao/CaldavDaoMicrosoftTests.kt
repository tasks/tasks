package org.tasks.data.dao

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.runBlocking
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CaldavDaoMicrosoftTests {
    private lateinit var db: Database
    private lateinit var taskDao: TaskDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var dirtyDao: DirtyDao

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<Database>()
            .setDriver(BundledSQLiteDriver())
            .addCallback(Database.CALLBACK)
            .build()
        taskDao = db.taskDao()
        caldavDao = db.caldavDao()
        dirtyDao = db.dirtyDao()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private suspend fun insertCaldavTask(
        remoteId: String,
        remoteParent: String? = null,
        synced: Boolean = true,
        deleted: Long = 0L,
        calendar: String = CALENDAR,
    ) {
        val task = Task()
        taskDao.createNew(task)
        val ctId = caldavDao.insert(
            CaldavTask(
                task = task.id,
                calendar = calendar,
                remoteId = remoteId,
                remoteParent = remoteParent,
                deleted = deleted,
            )
        )
        if (synced) {
            dirtyDao.markSynced(ctId)
        }
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
        insertCaldavTask(remoteId = "unsynced-1", synced = false)

        assertTrue(caldavDao.getTopLevelRemoteIds(CALENDAR).isEmpty())
    }

    @Test
    fun filtersPerCalendar() = runBlocking {
        insertCaldavTask(remoteId = "a-1", calendar = "calendar-a")
        insertCaldavTask(remoteId = "b-1", calendar = "calendar-b")

        assertEquals(listOf("a-1"), caldavDao.getTopLevelRemoteIds("calendar-a"))
        assertEquals(listOf("b-1"), caldavDao.getTopLevelRemoteIds("calendar-b"))
    }

    private suspend fun account(username: String, type: Int = TYPE_MICROSOFT) =
        caldavDao.insert(CaldavAccount(accountType = type, username = username))

    @Test
    fun theSubjectWinsOverTheSignInName() = runBlocking {
        account("alice@example.com")
        val subject = account("1234")

        assertEquals(
            subject,
            caldavDao.findMicrosoftAccount("1234", "alice@example.com", "alice@example.com")?.id,
        )
    }

    @Test
    fun thePreferredUsernameWinsOverTheEmail() = runBlocking {
        account("alice@outlook.com")
        val preferred = account("alice@live.com")

        assertEquals(
            preferred,
            caldavDao.findMicrosoftAccount("1234", "alice@live.com", "alice@outlook.com")?.id,
        )
    }

    @Test
    fun theEmailIsUsedWithoutAPreferredUsername() = runBlocking {
        val stored = account("alice@outlook.com")

        assertEquals(stored, caldavDao.findMicrosoftAccount("1234", null, "alice@outlook.com")?.id)
    }

    @Test
    fun aSignInNameMatchesRegardlessOfCase() = runBlocking {
        val stored = account("Alice@Outlook.com")

        assertEquals(stored, caldavDao.findMicrosoftAccount("1234", "alice@outlook.com", null)?.id)
    }

    @Test
    fun theSubjectIsMatchedExactly() = runBlocking {
        account("abcd")

        assertNull(caldavDao.findMicrosoftAccount("ABCD", null, null))
    }

    @Test
    fun otherAccountTypesAreIgnored() = runBlocking {
        account("alice@example.com", type = TYPE_GOOGLE_TASKS)

        assertNull(caldavDao.findMicrosoftAccount("1234", "alice@example.com", "alice@example.com"))
    }

    companion object {
        private const val CALENDAR = "test-calendar"
    }
}
