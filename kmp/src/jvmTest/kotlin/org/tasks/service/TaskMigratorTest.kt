package org.tasks.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.tasks.DatabaseTest
import org.tasks.caldav.CaldavClient
import org.tasks.caldav.CaldavClientProvider
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import org.tasks.wear.WearSyncNotifier

class TaskMigratorTest : DatabaseTest() {
    private val caldavDao = db.caldavDao()
    private val taskDao = db.taskDao()
    private val dirtyDao = db.dirtyDao()
    private val taskDeleter = TaskDeleter(
        deletionDao = db.deletionDao(),
        taskDao = db.taskDao(),
        caldavDao = caldavDao,
        refreshBroadcaster = mock(),
        vtodoCache = mock(),
        tasksPreferences = mock(),
        taskCleanup = object : TaskCleanup {},
        wearSyncNotifier = mock(),
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

    @Test
    fun migratesOnlySpecifiedLocalAccount() = runBlocking {
        val localAccount = insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
        insertAccount(type = TYPE_LOCAL, uuid = "other-local")
        val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")
        insertCalendar(account = "local-uuid", uuid = "cal-1", name = "Mine")
        insertCalendar(account = "other-local", uuid = "cal-2", name = "Other")

        migrator.migrateLocalTasks(localAccount, tasksAccount)

        assertEquals(
            listOf("Mine"),
            caldavDao.getCalendarsByAccount(tasksAccount.uuid!!).map { it.name },
        )
        // the other local account is left untouched
        assertEquals("other-local", caldavDao.getAccountByUuid("other-local")?.uuid)
        assertEquals(
            listOf("Other"),
            caldavDao.getCalendarsByAccount("other-local").map { it.name },
        )
    }

    @Test
    fun migratesCalendarToTargetAccount() = runBlocking {
        val localAccount = insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
        val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")
        insertCalendar(account = "local-uuid", uuid = "cal-1", name = "My List")

        migrator.migrateLocalTasks(localAccount, tasksAccount)

        val calendars = caldavDao.getCalendarsByAccount(tasksAccount.uuid!!)
        assertEquals(1, calendars.size)
        assertEquals(tasksAccount.uuid!!, calendars[0].account)
    }

    @Test
    fun setsUrlFromMakeCollection() = runBlocking {
        val localAccount = insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
        val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")
        insertCalendar(account = "local-uuid", uuid = "cal-1", name = "Work")

        migrator.migrateLocalTasks(localAccount, tasksAccount)

        val calendars = caldavDao.getCalendarsByAccount(tasksAccount.uuid!!)
        assertEquals(REMOTE_URL, calendars[0].url)
    }

    @Test
    fun migratesMultipleCalendars() = runBlocking {
        val localAccount = insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
        val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")
        insertCalendar(account = "local-uuid", uuid = "cal-1", name = "Work")
        insertCalendar(account = "local-uuid", uuid = "cal-2", name = "Personal")

        migrator.migrateLocalTasks(localAccount, tasksAccount)

        val calendars = caldavDao.getCalendarsByAccount(tasksAccount.uuid!!)
        assertEquals(
            setOf("Work", "Personal"),
            calendars.map { it.name }.toSet(),
        )
    }

    @Test
    fun preservesCalendarColor() = runBlocking {
        val localAccount = insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
        val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")
        insertCalendar(account = "local-uuid", uuid = "cal-1", name = "Colored", color = 0xFF0000)

        migrator.migrateLocalTasks(localAccount, tasksAccount)

        val calendars = caldavDao.getCalendarsByAccount(tasksAccount.uuid!!)
        assertEquals(0xFF0000, calendars[0].color)
    }

    @Test
    fun preservesCalendarIcon() = runBlocking {
        val localAccount = insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
        val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")
        insertCalendar(account = "local-uuid", uuid = "cal-1", name = "Icons", icon = "star")

        migrator.migrateLocalTasks(localAccount, tasksAccount)

        val calendars = caldavDao.getCalendarsByAccount(tasksAccount.uuid!!)
        assertEquals("star", calendars[0].icon)
    }

    @Test
    fun deletesLocalAccount() = runBlocking {
        val localAccount = insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
        val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")
        insertCalendar(account = "local-uuid", uuid = "cal-1", name = "My List")

        migrator.migrateLocalTasks(localAccount, tasksAccount)

        assertNull(caldavDao.getAccountByUuid("local-uuid"))
        assertTrue(caldavDao.getCalendarsByAccount("local-uuid").isEmpty())
    }

    @Test
    fun marksLocalTasksDirtyForUpload() = runBlocking {
        val localAccount = insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
        val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")
        insertCalendar(account = "local-uuid", uuid = "cal-1", name = "My List")
        val task = Task(title = "Local task")
        taskDao.createNew(task)
        val caldavTaskId = caldavDao.insert(CaldavTask(task = task.id, calendar = "cal-1"))

        assertTrue(dirtyDao.getTasksToPush("cal-1").isEmpty())

        migrator.migrateLocalTasks(localAccount, tasksAccount)

        assertEquals(true, dirtyDao.isDirty(caldavTaskId))
        assertEquals(
            listOf(task.id),
            dirtyDao.getTasksToPush("cal-1").map { it.task.id },
        )
    }

    @Test
    fun triggersSyncAfterMigration() {
        runBlocking {
            val localAccount = insertAccount(type = TYPE_LOCAL, uuid = "local-uuid")
            val tasksAccount = insertAccount(type = TYPE_TASKS, uuid = "tasks-uuid")

            migrator.migrateLocalTasks(localAccount, tasksAccount)

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
