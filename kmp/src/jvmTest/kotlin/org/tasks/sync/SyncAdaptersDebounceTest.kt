package org.tasks.sync

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.DirtyDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.jobs.BackgroundWork

class SyncAdaptersDebounceTest {
    private val db = Room.inMemoryDatabaseBuilder<Database>()
        .setDriver(BundledSQLiteDriver())
        .addCallback(Database.CALLBACK)
        .build()
    private val caldavDao: CaldavDao = db.caldavDao()
    private val taskDao: TaskDao = db.taskDao()
    private val dirtyDao: DirtyDao = db.dirtyDao()

    private val synced = mutableListOf<SyncSource>()

    private val backgroundWork = object : BackgroundWork {
        override fun updateCalendar(task: Task) {}
        override suspend fun scheduleRefresh(timestamp: Long) {}
        override suspend fun scheduleBlogFeedCheck() {}
        override suspend fun sync(source: SyncSource) {
            synced += source
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun syncAdapters() = SyncAdapters(
        backgroundWork = backgroundWork,
        caldavDao = caldavDao,
        dirtyDao = dirtyDao,
        openTaskListsActive = { false },
        tasksPreferences = mock(),
        refreshBroadcaster = mock(),
        coroutineContext = Dispatchers.Default,
        debounceMs = DEBOUNCE,
    )

    @Test
    fun burstOfDirtyTasksSyncsOnce() = runBlocking {
        setupAccount()
        syncAdapters()
        quiet()

        repeat(4) {
            dirtyTask()
            delay(DEBOUNCE * 3 / 4)
        }
        awaitSync()

        assertEquals(listOf(SyncSource.TASK_CHANGE), synced)
    }

    @Test
    fun quietPeriodBetweenChangesSyncsTwice() = runBlocking {
        setupAccount()
        syncAdapters()
        quiet()

        dirtyTask()
        awaitSync()
        dirtyTask()
        awaitSync()

        assertEquals(listOf(SyncSource.TASK_CHANGE, SyncSource.TASK_CHANGE), synced)
    }

    @Test
    fun stuckDirtyTaskDoesNotResync() = runBlocking {
        setupAccount()
        syncAdapters()
        quiet()

        val ctId = dirtyTask()
        awaitSync()
        assertEquals(listOf(SyncSource.TASK_CHANGE), synced)

        dirtyDao.markPushed(ctId, 0)
        quiet()

        assertEquals(listOf(SyncSource.TASK_CHANGE), synced)
    }

    @Test
    fun newWorkAfterAStuckTaskStillSyncs() = runBlocking {
        setupAccount()
        syncAdapters()
        quiet()

        dirtyTask()
        awaitSync()
        synced.clear()

        dirtyTask()
        awaitSync()

        assertEquals(listOf(SyncSource.TASK_CHANGE), synced)
    }

    @Test
    fun directRequestSyncsEvenWhenDirtySetUnchanged() = runBlocking {
        setupAccount()
        val adapters = syncAdapters()
        quiet()
        dirtyTask()
        awaitSync()
        synced.clear()

        adapters.sync(SyncSource.USER_INITIATED)
        awaitSync()

        assertEquals(listOf(SyncSource.USER_INITIATED), synced)
    }

    @Test
    fun directRequestDuringBurstKeepsItsPriority() = runBlocking {
        setupAccount()
        val adapters = syncAdapters()
        quiet()

        dirtyTask()
        delay(DEBOUNCE / 2)
        adapters.sync(SyncSource.USER_INITIATED)
        awaitSync()

        assertEquals(listOf(SyncSource.USER_INITIATED), synced)
    }

    @Test
    fun localTaskDoesNotSync() = runBlocking {
        syncAdapters()
        quiet()

        taskDao.createNew(Task(title = "local"))
        quiet()

        assertEquals(emptyList<SyncSource>(), synced)
    }

    private suspend fun awaitSync() {
        val before = synced.size
        withTimeoutOrNull(TIMEOUT) {
            while (synced.size == before) {
                delay(5)
            }
        }
        quiet()
    }

    private suspend fun quiet() {
        delay(DEBOUNCE * 5)
    }

    private suspend fun setupAccount() {
        caldavDao.insert(CaldavAccount(accountType = TYPE_CALDAV, uuid = ACCOUNT))
        caldavDao.insert(CaldavCalendar(account = ACCOUNT, uuid = CALENDAR))
    }

    private suspend fun dirtyTask(): Long {
        val task = Task(title = "task")
        taskDao.createNew(task)
        return caldavDao.insert(CaldavTask(task = task.id, calendar = CALENDAR))
    }

    companion object {
        private const val ACCOUNT = "account"
        private const val CALENDAR = "calendar"
        private const val DEBOUNCE = 100L
        private const val TIMEOUT = 5_000L
    }
}
