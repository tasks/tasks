package org.tasks.sync

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.DirtyDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.SUPPRESS_SYNC
import org.tasks.data.entity.SYNC_ALARMS
import org.tasks.data.entity.SYNC_LOCATION
import org.tasks.data.entity.SYNC_TAGS
import org.tasks.data.entity.Task

@OptIn(ExperimentalCoroutinesApi::class)
class SyncAdaptersTest {
    private val db = Room.inMemoryDatabaseBuilder<Database>()
        .setDriver(BundledSQLiteDriver())
        .addCallback(Database.CALLBACK)
        .build()
    private val caldavDao: CaldavDao = db.caldavDao()
    private val taskDao: TaskDao = db.taskDao()
    private val dirtyDao: DirtyDao = db.dirtyDao()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val taskSaver = TaskSaver(
        taskDao = taskDao,
        refreshBroadcaster = mock(),
        notifier = mock(),
        locationService = mock(),
        timerPlugin = mock(),
        backgroundWork = mock(),
        caldavDao = caldavDao,
    )

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun suppressSyncSkipsEverything() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_CALDAV)
        task.putTransitory(SUPPRESS_SYNC, true)

        taskSaver.save(task, null)

        assertNotDirty(task.id)
    }

    @Test
    fun localTaskDoesNotSync() = runTest(testDispatcher) {
        val task = createTask()

        taskSaver.save(task, null)

        assertNotDirty(task.id)
    }

    @Test
    fun dirtyFalseDoesNotSync() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_CALDAV)

        taskSaver.save(task, task.copy(title = "old"), dirty = false)

        assertNotDirty(task.id)
    }

    @Test
    fun googleTasksSyncsOnTitleChange() = runTest(testDispatcher) {
        val task = createTask(title = "new")
        setupAccount(task.id, TYPE_GOOGLE_TASKS)

        taskSaver.save(task, task.copy(title = "old"))

        assertDirty(task.id)
    }

    @Test
    fun googleTasksIgnoresPriorityChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_GOOGLE_TASKS)

        taskSaver.save(task, task.copy(priority = Task.Priority.HIGH))

        assertNotDirty(task.id)
    }

    @Test
    fun googleTasksIgnoresTagChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_GOOGLE_TASKS)
        task.putTransitory(SYNC_TAGS, true)

        taskSaver.save(task, task.copy())

        assertNotDirty(task.id)
    }

    @Test
    fun googleTasksIgnoresAlarmChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_GOOGLE_TASKS)
        task.putTransitory(SYNC_ALARMS, true)

        taskSaver.save(task, task.copy())

        assertNotDirty(task.id)
    }

    @Test
    fun googleTasksIgnoresLocationChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_GOOGLE_TASKS)
        task.putTransitory(SYNC_LOCATION, true)

        taskSaver.save(task, task.copy())

        assertNotDirty(task.id)
    }

    @Test
    fun microsoftSyncsOnTitleChange() = runTest(testDispatcher) {
        val task = createTask(title = "new")
        setupAccount(task.id, TYPE_MICROSOFT)

        taskSaver.save(task, task.copy(title = "old"))

        assertDirty(task.id)
    }

    @Test
    fun microsoftSyncsOnPriorityChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_MICROSOFT)

        taskSaver.save(task, task.copy(priority = Task.Priority.HIGH))

        assertDirty(task.id)
    }

    @Test
    fun microsoftSyncsOnTagChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_MICROSOFT)
        task.putTransitory(SYNC_TAGS, true)

        taskSaver.save(task, task.copy())

        assertDirty(task.id)
    }

    @Test
    fun microsoftIgnoresAlarmChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_MICROSOFT)
        task.putTransitory(SYNC_ALARMS, true)

        taskSaver.save(task, task.copy())

        assertNotDirty(task.id)
    }

    @Test
    fun microsoftIgnoresLocationChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_MICROSOFT)
        task.putTransitory(SYNC_LOCATION, true)

        taskSaver.save(task, task.copy())

        assertNotDirty(task.id)
    }

    @Test
    fun microsoftSubtaskSyncsOnTitleChange() = runTest(testDispatcher) {
        val parent = createTask()
        val subtask = createTask(title = "new", parent = parent.id)
        setupAccount(subtask.id, TYPE_MICROSOFT)

        taskSaver.save(subtask, subtask.copy(title = "old"))

        assertDirty(subtask.id)
    }

    @Test
    fun microsoftSubtaskSyncsOnCompletionChange() = runTest(testDispatcher) {
        val parent = createTask()
        val subtask = createTask(parent = parent.id)
        setupAccount(subtask.id, TYPE_MICROSOFT)

        taskSaver.save(subtask, subtask.copy(completionDate = 12345L))

        assertDirty(subtask.id)
    }

    @Test
    fun microsoftSubtaskSyncsOnReparent() = runTest(testDispatcher) {
        val parent = createTask()
        val subtask = createTask(parent = parent.id)
        setupAccount(subtask.id, TYPE_MICROSOFT)

        taskSaver.save(subtask, subtask.copy(parent = 0))

        assertDirty(subtask.id)
    }

    @Test
    fun microsoftSubtaskIgnoresPriorityChange() = runTest(testDispatcher) {
        val parent = createTask()
        val subtask = createTask(parent = parent.id)
        setupAccount(subtask.id, TYPE_MICROSOFT)

        taskSaver.save(subtask, subtask.copy(priority = Task.Priority.HIGH))

        assertNotDirty(subtask.id)
    }

    @Test
    fun microsoftSubtaskIgnoresTagChange() = runTest(testDispatcher) {
        val parent = createTask()
        val subtask = createTask(parent = parent.id)
        setupAccount(subtask.id, TYPE_MICROSOFT)
        subtask.putTransitory(SYNC_TAGS, true)

        taskSaver.save(subtask, subtask.copy())

        assertNotDirty(subtask.id)
    }

    @Test
    fun caldavSyncsOnTitleChange() = runTest(testDispatcher) {
        val task = createTask(title = "new")
        setupAccount(task.id, TYPE_CALDAV)

        taskSaver.save(task, task.copy(title = "old"))

        assertDirty(task.id)
    }

    @Test
    fun caldavSyncsOnPriorityChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_CALDAV)

        taskSaver.save(task, task.copy(priority = Task.Priority.HIGH))

        assertDirty(task.id)
    }

    @Test
    fun caldavSyncsOnCollapsedChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_CALDAV)

        taskSaver.save(task, task.copy(isCollapsed = true))

        assertDirty(task.id)
    }

    @Test
    fun caldavSyncsOnTagChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_CALDAV)
        task.putTransitory(SYNC_TAGS, true)

        taskSaver.save(task, task.copy())

        assertDirty(task.id)
    }

    @Test
    fun caldavSyncsOnAlarmChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_CALDAV)
        task.putTransitory(SYNC_ALARMS, true)

        taskSaver.save(task, task.copy())

        assertDirty(task.id)
    }

    @Test
    fun caldavSyncsOnLocationChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_CALDAV)
        task.putTransitory(SYNC_LOCATION, true)

        taskSaver.save(task, task.copy())

        assertDirty(task.id)
    }

    @Test
    fun tasksOrgSyncsOnTagChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_TASKS)
        task.putTransitory(SYNC_TAGS, true)

        taskSaver.save(task, task.copy())

        assertDirty(task.id)
    }

    @Test
    fun etebaseSyncsOnAlarmChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_ETEBASE)
        task.putTransitory(SYNC_ALARMS, true)

        taskSaver.save(task, task.copy())

        assertDirty(task.id)
    }

    @Test
    fun openTasksSyncsOnLocationChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_OPENTASKS)
        task.putTransitory(SYNC_LOCATION, true)

        taskSaver.save(task, task.copy())

        assertDirty(task.id)
    }

    @Test
    fun caldavNoSyncWhenNothingChanged() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_CALDAV)

        taskSaver.save(task, task.copy())

        assertNotDirty(task.id)
    }

    @Test
    fun googleTasksNoSyncWhenNothingChanged() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_GOOGLE_TASKS)

        taskSaver.save(task, task.copy())

        assertNotDirty(task.id)
    }

    @Test
    fun microsoftNoSyncWhenNothingChanged() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_MICROSOFT)

        taskSaver.save(task, task.copy())

        assertNotDirty(task.id)
    }

    @Test
    fun caldavIgnoresTimerChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_CALDAV)

        taskSaver.save(task, task.copy(timerStart = 12345L))

        assertNotDirty(task.id)
    }

    @Test
    fun caldavIgnoresElapsedSecondsChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_CALDAV)

        taskSaver.save(task, task.copy(elapsedSeconds = 100))

        assertNotDirty(task.id)
    }

    @Test
    fun caldavIgnoresCalendarUriChange() = runTest(testDispatcher) {
        val task = createTask()
        setupAccount(task.id, TYPE_CALDAV)

        taskSaver.save(task, task.copy(calendarURI = "old"))

        assertNotDirty(task.id)
    }

    private suspend fun createTask(title: String = "task", parent: Long = 0): Task {
        val task = Task(title = title, parent = parent)
        taskDao.createNew(task)
        return task
    }

    private suspend fun setupAccount(taskId: Long, accountType: Int) {
        val uuid = "account-$accountType"
        val calUuid = "calendar-$taskId"
        if (caldavDao.getAccountByUuid(uuid) == null) {
            caldavDao.insert(CaldavAccount(accountType = accountType, uuid = uuid))
        }
        caldavDao.insert(CaldavCalendar(account = uuid, uuid = calUuid))
        val ctId = caldavDao.insert(CaldavTask(task = taskId, calendar = calUuid))
        dirtyDao.markSynced(ctId)
    }

    private suspend fun assertDirty(taskId: Long) {
        val ct = caldavDao.getTask(taskId) ?: error("No caldav task for $taskId")
        assertEquals(true, dirtyDao.isDirty(ct.id))
    }

    private suspend fun assertNotDirty(taskId: Long) {
        val ct = caldavDao.getTask(taskId)
        val dirty = ct?.let { dirtyDao.isDirty(it.id) }
        assertEquals("Expected task $taskId to not be dirty", true, dirty != true)
    }
}
