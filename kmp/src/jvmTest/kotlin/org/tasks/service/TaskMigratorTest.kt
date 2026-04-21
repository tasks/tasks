package org.tasks.service

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.tasks.caldav.CaldavClient
import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.dao.CaldavDao
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource

class TaskMigratorTest {
    private val db = Room.inMemoryDatabaseBuilder<Database>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val caldavDao = db.caldavDao()
    private val taskDeleter = TaskDeleter(
        deletionDao = db.deletionDao(),
        taskDao = db.taskDao(),
        refreshBroadcaster = mock(),
        vtodoCache = mock(),
        tasksPreferences = mock(),
        taskCleanup = object : TaskCleanup {},
    )
    private val caldavClient: CaldavClient = mock()
    private val clientProvider: CaldavClientProvider = mock()
    private val syncAdapters: SyncAdapters = mock()

    private val migrator by lazy {
        TaskMigrator(
            clientProvider = clientProvider,
            caldavDao = caldavDao,
            syncAdapters = syncAdapters,
            taskDeleter = taskDeleter,
        )
    }

    @Before
    fun setUp() {
        runBlocking {
            whenever(clientProvider.forAccount(any(), anyOrNull()))
                .thenReturn(caldavClient)
            whenever(caldavClient.makeCollection(any(), any(), anyOrNull()))
                .thenReturn(REMOTE_URL)
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun noOpWithoutLocalAccount() {
        runBlocking {
            val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")

            migrator.migrateLocalTasks(tasksAccount)

            verify(syncAdapters, never()).sync(any())
        }
    }

    @Test
    fun migratesCalendarToTargetAccount() = runBlocking {
        insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
        val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")
        insertCalendar(account = "local-uuid", uuid = "cal-1", name = "My List")

        migrator.migrateLocalTasks(tasksAccount)

        val calendars = caldavDao.getCalendarsByAccount(tasksAccount.uuid!!)
        assertEquals(1, calendars.size)
        assertEquals(tasksAccount.uuid!!, calendars[0].account)
    }

    @Test
    fun setsUrlFromMakeCollection() = runBlocking {
        insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
        val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")
        insertCalendar(account = "local-uuid", uuid = "cal-1", name = "Work")

        migrator.migrateLocalTasks(tasksAccount)

        val calendars = caldavDao.getCalendarsByAccount(tasksAccount.uuid!!)
        assertEquals(REMOTE_URL, calendars[0].url)
    }

    @Test
    fun migratesMultipleCalendars() = runBlocking {
        insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
        val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")
        insertCalendar(account = "local-uuid", uuid = "cal-1", name = "Work")
        insertCalendar(account = "local-uuid", uuid = "cal-2", name = "Personal")

        migrator.migrateLocalTasks(tasksAccount)

        val calendars = caldavDao.getCalendarsByAccount(tasksAccount.uuid!!)
        assertEquals(
            setOf("Work", "Personal"),
            calendars.map { it.name }.toSet(),
        )
    }

    @Test
    fun preservesCalendarColor() = runBlocking {
        insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
        val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")
        insertCalendar(account = "local-uuid", uuid = "cal-1", name = "Colored", color = 0xFF0000)

        migrator.migrateLocalTasks(tasksAccount)

        val calendars = caldavDao.getCalendarsByAccount(tasksAccount.uuid!!)
        assertEquals(0xFF0000, calendars[0].color)
    }

    @Test
    fun preservesCalendarIcon() = runBlocking {
        insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
        val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")
        insertCalendar(account = "local-uuid", uuid = "cal-1", name = "Icons", icon = "star")

        migrator.migrateLocalTasks(tasksAccount)

        val calendars = caldavDao.getCalendarsByAccount(tasksAccount.uuid!!)
        assertEquals("star", calendars[0].icon)
    }

    @Test
    fun deletesLocalAccount() = runBlocking {
        insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
        val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")
        insertCalendar(account = "local-uuid", uuid = "cal-1", name = "My List")

        migrator.migrateLocalTasks(tasksAccount)

        assertNull(caldavDao.getAccountByUuid("local-uuid"))
        assertTrue(caldavDao.getCalendarsByAccount("local-uuid").isEmpty())
    }

    @Test
    fun triggersSyncAfterMigration() {
        runBlocking {
            insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
            val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")

            migrator.migrateLocalTasks(tasksAccount)

            verify(syncAdapters).sync(SyncSource.ACCOUNT_ADDED)
        }
    }

    private suspend fun insertAccount(type: Int, uuid: String): CaldavAccount {
        val account = CaldavAccount(accountType = type, uuid = uuid)
        val id = caldavDao.insert(account)
        return account.copy(id = id)
    }

    private suspend fun insertCalendar(
        account: String,
        uuid: String,
        name: String,
        color: Int = 0,
        icon: String? = null,
    ) {
        caldavDao.insert(
            CaldavCalendar(
                account = account,
                uuid = uuid,
                name = name,
                color = color,
                icon = icon,
            )
        )
    }

    companion object {
        private const val REMOTE_URL = "https://tasks.org/calendars/abc-123/uuid-1"
    }
}
