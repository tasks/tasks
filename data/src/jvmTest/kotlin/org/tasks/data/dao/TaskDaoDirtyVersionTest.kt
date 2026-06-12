package org.tasks.data.dao

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task

class TaskDaoDirtyVersionTest {
    private lateinit var db: Database
    private lateinit var taskDao: TaskDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var dirtyDao: DirtyDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<Database>()
            .setDriver(BundledSQLiteDriver())
            .addCallback(Database.CALLBACK)
            .build()
        taskDao = db.taskDao()
        caldavDao = db.caldavDao()
        dirtyDao = db.dirtyDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun triggerCreatesRowOnInsert() = runBlocking {
        val (_, ctId) = createTaskWithCaldavTask()

        assertEquals(1L, dirtyVersion(ctId))
        assertEquals(0L, dirtyDao.getSyncedVersion(ctId))
    }

    @Test
    fun triggerRowIsDirty() = runBlocking {
        val (_, ctId) = createTaskWithCaldavTask()

        assertEquals(true, dirtyDao.isDirty(ctId))
    }

    @Test
    fun triggerSkipsLocalAccount() = runBlocking {
        val (_, ctId) = createTaskWithCaldavTask(accountType = TYPE_LOCAL)

        assertNull(dirtyDao.getDirtyState(ctId))
    }

    @Test
    fun setDirtySkipsLocalAccount() = runBlocking {
        val (taskId, ctId) = createTaskWithCaldavTask(accountType = TYPE_LOCAL)

        dirtyDao.setDirty(listOf(taskId))

        assertNull(dirtyDao.getDirtyState(ctId))
    }

    @Test
    fun hasDirtyTasksIgnoresLocalAccount() = runBlocking {
        createTaskWithCaldavTask(accountType = TYPE_LOCAL)

        assertEquals(false, dirtyDao.hasDirtyTasks().first())
    }

    @Test
    fun setDirtyIncrementsDirtyVersion() = runBlocking {
        val (taskId, ctId) = createTaskWithCaldavTask()

        dirtyDao.setDirty(listOf(taskId))

        assertEquals(2L, dirtyVersion(ctId))
        assertEquals(0L, dirtyDao.getSyncedVersion(ctId))
    }

    @Test
    fun setDirtyTwiceIncrementsTwice() = runBlocking {
        val (taskId, ctId) = createTaskWithCaldavTask()

        dirtyDao.setDirty(listOf(taskId))
        dirtyDao.setDirty(listOf(taskId))

        assertEquals(3L, dirtyVersion(ctId))
        assertEquals(0L, dirtyDao.getSyncedVersion(ctId))
    }

    @Test
    fun setDirtySkipsTombstones() = runBlocking {
        val (taskId, ctId) = createTaskWithCaldavTask(deleted = true)

        dirtyDao.setDirty(listOf(taskId))

        assertEquals(2L, dirtyVersion(ctId))
    }

    @Test
    fun setDirtyForAccountTypes() = runBlocking {
        val (taskId, ctId) = createTaskWithCaldavTask(accountType = TYPE_CALDAV)

        dirtyDao.setDirty(listOf(taskId), listOf(TYPE_GOOGLE_TASKS))

        assertEquals(1L, dirtyVersion(ctId))
    }

    @Test
    fun setDirtyForMatchingAccountType() = runBlocking {
        val (taskId, ctId) = createTaskWithCaldavTask(accountType = TYPE_CALDAV)

        dirtyDao.setDirty(listOf(taskId), listOf(TYPE_CALDAV))

        assertEquals(2L, dirtyVersion(ctId))
    }

    @Test
    fun markSyncedOnTriggerRow() = runBlocking {
        val (_, ctId) = createTaskWithCaldavTask()

        dirtyDao.markSynced(ctId)

        assertEquals(1L, dirtyVersion(ctId))
        assertEquals(1L, dirtyDao.getSyncedVersion(ctId))
        assertEquals(false, dirtyDao.isDirty(ctId))
    }

    @Test
    fun markSyncedOnCleanTask() = runBlocking {
        val (_, ctId) = createTaskWithCaldavTask()
        dirtyDao.setDirtyState(ctId, 1, 1)

        dirtyDao.markSynced(ctId)

        assertEquals(1L, dirtyVersion(ctId))
        assertEquals(1L, dirtyDao.getSyncedVersion(ctId))
    }

    @Test
    fun markSyncedSkipsEditAfterInsert() = runBlocking {
        val (taskId, ctId) = createTaskWithCaldavTask()
        dirtyDao.setDirty(listOf(taskId))

        dirtyDao.markSynced(ctId)

        assertEquals(2L, dirtyVersion(ctId))
        assertEquals(0L, dirtyDao.getSyncedVersion(ctId))
        assertEquals(true, dirtyDao.isDirty(ctId))
    }

    @Test
    fun markSyncedSkipsMultipleEditsBeforeFirstSync() = runBlocking {
        val (taskId, ctId) = createTaskWithCaldavTask()
        dirtyDao.setDirty(listOf(taskId))
        dirtyDao.setDirty(listOf(taskId))

        dirtyDao.markSynced(ctId)

        assertEquals(3L, dirtyVersion(ctId))
        assertEquals(0L, dirtyDao.getSyncedVersion(ctId))
        assertEquals(true, dirtyDao.isDirty(ctId))
    }

    @Test
    fun markSyncedSkipsDirtyAfterPreviousSync() = runBlocking {
        val (taskId, ctId) = createTaskWithCaldavTask()
        dirtyDao.setDirtyState(ctId, 2, 1)

        dirtyDao.markSynced(ctId)

        assertEquals(2L, dirtyVersion(ctId))
        assertEquals(1L, dirtyDao.getSyncedVersion(ctId))
        assertEquals(true, dirtyDao.isDirty(ctId))
    }

    @Test
    fun markSyncedSkipsTaskDirtiedAfterPush() = runBlocking {
        val (taskId, ctId) = createTaskWithCaldavTask()
        dirtyDao.setDirtyState(ctId, 3, 1)

        dirtyDao.markSynced(ctId)

        assertEquals(3L, dirtyVersion(ctId))
        assertEquals(1L, dirtyDao.getSyncedVersion(ctId))
    }

    @Test
    fun markPushedSetsSyncedVersion() = runBlocking {
        val (taskId, ctId) = createTaskWithCaldavTask()
        dirtyDao.setDirty(listOf(taskId))
        val version = dirtyVersion(ctId)!!

        dirtyDao.markPushed(ctId, version)

        assertEquals(2L, dirtyVersion(ctId))
        assertEquals(2L, dirtyDao.getSyncedVersion(ctId))
        assertEquals(false, dirtyDao.isDirty(ctId))
    }

    @Test
    fun markPushedWithConcurrentEdit() = runBlocking {
        val (taskId, ctId) = createTaskWithCaldavTask()
        dirtyDao.setDirty(listOf(taskId))
        val version = dirtyVersion(ctId)!!
        dirtyDao.setDirty(listOf(taskId))

        dirtyDao.markPushed(ctId, version)

        assertEquals(3L, dirtyVersion(ctId))
        assertEquals(2L, dirtyDao.getSyncedVersion(ctId))
        assertEquals(true, dirtyDao.isDirty(ctId))
    }

    @Test
    fun isDirtyForNonexistentRow() = runBlocking {
        assertNull(dirtyDao.isDirty(999L))
    }

    @Test
    fun isDirtyWhenClean() = runBlocking {
        val (_, ctId) = createTaskWithCaldavTask()
        dirtyDao.markSynced(ctId)

        assertEquals(false, dirtyDao.isDirty(ctId))
    }

    @Test
    fun isDirtyWhenDirty() = runBlocking {
        val (taskId, ctId) = createTaskWithCaldavTask()
        dirtyDao.setDirty(listOf(taskId))

        assertEquals(true, dirtyDao.isDirty(ctId))
    }

    @Test
    fun getDirtyStateByTaskIdSkipsTombstones() = runBlocking {
        val task = Task()
        taskDao.createNew(task)
        val ctId = insertCaldavTask(task.id, deleted = true)
        dirtyDao.setDirtyState(ctId, 5, 1)

        assertNull(dirtyDao.getDirtyStateByTaskIds(listOf(task.id))[task.id])
    }

    @Test
    fun getDirtyStateByTaskIdReturnsActiveVersion() = runBlocking {
        val (taskId, ctId) = createTaskWithCaldavTask()
        dirtyDao.setDirtyState(ctId, 3, 1)

        val state = dirtyDao.getDirtyStateByTaskIds(listOf(taskId))[taskId]
        assertEquals(3L, state?.dirtyVersion)
        assertEquals(1L, state?.syncedVersion)
    }

    @Test
    fun getCaldavTasksToPushReturnsDirty() = runBlocking {
        val calUuid = setupCalendar(TYPE_CALDAV)
        val (taskId, ctId) = createTaskWithCaldavTask(calendar = calUuid)

        val result = dirtyDao.getTasksToPush(calUuid)

        assertEquals(1, result.size)
        assertEquals(taskId, result.first().task.id)
    }

    @Test
    fun getCaldavTasksToPushExcludesClean() = runBlocking {
        val calUuid = setupCalendar(TYPE_CALDAV)
        val (_, ctId) = createTaskWithCaldavTask(calendar = calUuid)
        dirtyDao.markSynced(ctId)

        val result = dirtyDao.getTasksToPush(calUuid)

        assertEquals(0, result.size)
    }

    @Test
    fun googleTasksPushExcludesTombstones() = runBlocking {
        val accountUuid = "google-account"
        caldavDao.insert(CaldavAccount(accountType = TYPE_GOOGLE_TASKS, uuid = accountUuid))
        val calUuid = "google-calendar"
        caldavDao.insert(CaldavCalendar(account = accountUuid, uuid = calUuid))

        val task = Task()
        taskDao.createNew(task)
        insertCaldavTask(
            task.id,
            calendar = calUuid,
            deleted = true,
            remoteId = "remote-1",
        )

        val result = dirtyDao.getTasksToPush(calUuid)

        assertEquals(0, result.size)
    }

    @Test
    fun googleTasksPushExcludesClean() = runBlocking {
        val accountUuid = "google-account-2"
        caldavDao.insert(CaldavAccount(accountType = TYPE_GOOGLE_TASKS, uuid = accountUuid))
        val calUuid = "google-calendar-2"
        caldavDao.insert(CaldavCalendar(account = accountUuid, uuid = calUuid))

        val task = Task()
        taskDao.createNew(task)
        val ctId = insertCaldavTask(
            task.id,
            calendar = calUuid,
            remoteId = "remote-2",
        )
        dirtyDao.markSynced(ctId)

        val result = dirtyDao.getTasksToPush(calUuid)

        assertEquals(0, result.size)
    }

    @Test
    fun getMovedByAccountReturnsTombstones() = runBlocking {
        val accountUuid = "google-account-3"
        caldavDao.insert(CaldavAccount(accountType = TYPE_GOOGLE_TASKS, uuid = accountUuid))
        val calUuid = "google-calendar-3"
        caldavDao.insert(CaldavCalendar(account = accountUuid, uuid = calUuid))

        val task = Task()
        taskDao.createNew(task)
        insertCaldavTask(
            task.id,
            calendar = calUuid,
            deleted = true,
            remoteId = "remote-3",
        )

        val result = caldavDao.getMovedByAccount(accountUuid)

        assertEquals(1, result.size)
    }

    @Test
    fun getMovedByAccountExcludesActive() = runBlocking {
        val accountUuid = "google-account-4"
        caldavDao.insert(CaldavAccount(accountType = TYPE_GOOGLE_TASKS, uuid = accountUuid))
        val calUuid = "google-calendar-4"
        caldavDao.insert(CaldavCalendar(account = accountUuid, uuid = calUuid))

        val task = Task()
        taskDao.createNew(task)
        insertCaldavTask(task.id, calendar = calUuid, remoteId = "remote-4")

        val result = caldavDao.getMovedByAccount(accountUuid)

        assertEquals(0, result.size)
    }

    @Test
    fun deletingCaldavTaskDeletesDirtyRow() = runBlocking {
        val (taskId, ctId) = createTaskWithCaldavTask()
        assertEquals(1L, dirtyVersion(ctId))

        caldavDao.delete(caldavDao.getTask(taskId)!!)

        assertNull(dirtyVersion(ctId))
    }

    private suspend fun dirtyVersion(ctId: Long): Long? = dirtyDao.getDirtyState(ctId)?.dirtyVersion

    private suspend fun createTaskWithCaldavTask(
        accountType: Int = TYPE_CALDAV,
        calendar: String? = null,
        deleted: Boolean = false,
    ): Pair<Long, Long> {
        val calUuid = calendar ?: setupCalendar(accountType)
        val task = Task()
        taskDao.createNew(task)
        val ctId = insertCaldavTask(task.id, calUuid, deleted)
        return task.id to ctId
    }

    private suspend fun setupCalendar(accountType: Int): String {
        val accountUuid = "account-$accountType"
        if (caldavDao.getAccountByUuid(accountUuid) == null) {
            caldavDao.insert(CaldavAccount(accountType = accountType, uuid = accountUuid))
        }
        val calUuid = "calendar-${System.nanoTime()}"
        caldavDao.insert(CaldavCalendar(account = accountUuid, uuid = calUuid))
        return calUuid
    }

    private suspend fun insertCaldavTask(
        taskId: Long,
        calendar: String? = null,
        deleted: Boolean = false,
        remoteId: String? = null,
    ): Long {
        val calUuid = calendar ?: setupCalendar(TYPE_CALDAV)
        val ct = CaldavTask(
            task = taskId,
            calendar = calUuid,
            remoteId = remoteId,
        )
        val ctId = caldavDao.insert(ct)
        if (deleted) {
            caldavDao.markDeleted(listOf(taskId))
        }
        return ctId
    }
}
