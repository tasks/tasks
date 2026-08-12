package org.tasks.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.tasks.data.SubtaskNode
import org.tasks.data.SubtaskTreeWriter
import org.tasks.data.PendingTask
import org.tasks.data.SubtaskTreeRegistry
import org.tasks.data.SubtaskTrees
import org.tasks.data.TaskMover
import org.tasks.data.TaskSaver
import com.todoroo.astrid.alarms.AlarmService
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.DirtyDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.DatePickerPreferences
import org.tasks.preferences.TaskDefaultSettings
import org.tasks.service.TaskCompleter
import org.tasks.service.TaskDeleter

@OptIn(ExperimentalCoroutinesApi::class)
abstract class TaskEditViewModelFixture {
    protected val NINE_AM_WITH_TIME = 9 * 60 * 60 * 1000 + 1000

    protected val NEW_TASK_ID = 55L
    protected val NEW_TASK_ORDER = -1L
    protected val testDispatcher = StandardTestDispatcher()
    protected val taskDao: TaskDao = mock()
    protected val taskSaver: TaskSaver = mock()
    protected val caldavDao: CaldavDao = mock()
    protected val googleTaskDao: GoogleTaskDao = mock()
    protected val dirtyDao: DirtyDao = mock()
    protected val refreshBroadcaster: RefreshBroadcaster = mock()
    protected val refreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    protected val treeRegistry = SubtaskTreeRegistry()
    protected val subtaskTrees = OpenSubtaskTrees(treeRegistry)

    protected val subtaskWriter by lazy {
        SubtaskTreeWriter(
            taskDao = taskDao,
            caldavDao = caldavDao,
            googleTaskDao = googleTaskDao,
            tagDao = tagDao,
            dirtyDao = dirtyDao,
            alarmService = alarmService,
            taskCompleter = taskCompleter,
            taskDeleter = taskDeleter,
            taskSaver = taskSaver,
            refreshBroadcaster = refreshBroadcaster,
        )
    }
    protected val taskMover: TaskMover = mock()
    protected val tagDao: TagDao = mock()
    protected val tagDataDao: TagDataDao = mock()
    protected val alarmDao: AlarmDao = mock()
    protected val alarmService: AlarmService = mock()
    protected val appPreferences: AppPreferences = mock()
    protected val taskCompleter: TaskCompleter = mock()
    protected val taskDeleter: TaskDeleter = mock()

    protected lateinit var viewModel: TaskEditViewModel
    protected lateinit var pendingSaves: PendingTaskSaves

    protected val createdRows = mutableMapOf<Long, Task>()

    protected var nextTaskId = NEW_TASK_ID

    protected var inTransaction = false

    protected val testCalendar = CaldavCalendar(account = "acct-1", uuid = "cal-1", name = "Test")
    protected val seedCalendar = CaldavCalendar(id = 7, account = "acct-1", uuid = "cal-7", name = "Seed")
    protected val testAccount = CaldavAccount(uuid = "acct-1")

    protected val workTag = TagData(name = "Work", remoteId = "tag-work")

    protected val NO_TITLE = "(No title)"

    @Before
    fun setUp() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        pendingSaves = PendingTaskSaves(CoroutineScope(testDispatcher))
        whenever(caldavDao.getCalendars()).thenReturn(listOf(testCalendar))
        whenever(caldavDao.getAccountByUuid("acct-1")).thenReturn(testAccount)
        whenever(caldavDao.getCalendarById(seedCalendar.id)).thenReturn(seedCalendar)
        whenever(taskDao.watch(any())).thenReturn(MutableSharedFlow())
        whenever(tagDataDao.getTagDataForTask(any())).thenReturn(emptyList())
        whenever(appPreferences.datePickerPreferences()).thenReturn(DatePickerPreferences())
        whenever(tagDataDao.getByUuid("tag-work")).thenReturn(workTag)
        whenever(appPreferences.defaultAlarms()).thenReturn(emptyList())
        whenever(appPreferences.taskDefaults()).thenReturn(TaskDefaultSettings(defaultAlarms = emptyList()))
        whenever(appPreferences.isDefaultDueTimeEnabled()).thenReturn(false)
        whenever(appPreferences.defaultRingMode()).thenReturn(0)
        whenever(alarmDao.getAlarms(any<Long>())).thenReturn(emptyList())
        whenever(taskDao.fetchTasks(any<String>())).thenReturn(emptyList())
        whenever(refreshBroadcaster.broadcastRefresh()).thenAnswer {
            refreshes.tryEmit(Unit)
            Unit
        }
        whenever(alarmDao.watchAlarms(any())).thenReturn(MutableSharedFlow())
        caldavDao.stub {
            onBlocking { insert(task = any(), caldavTask = any(), addToTop = any()) }
                .doSuspendableAnswer { invocation ->
                    val task = invocation.arguments[0] as Task
                    if (invocation.arguments[2] as Boolean) {
                        task.order = NEW_TASK_ORDER
                    }
                    1L
                }
        }
        taskDao.stub {
            onBlocking { inTransaction<Any?>(any()) } doSuspendableAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                (invocation.arguments[0] as suspend () -> Any?).invoke()
            }
            onBlocking { createNew(any()) } doSuspendableAnswer { invocation ->
                val task = invocation.arguments[0] as Task
                val id = nextTaskId++
                task.id = id
                createdRows[id] = task.copy()
                id
            }
            onBlocking { fetch(any<Long>()) } doSuspendableAnswer { invocation ->
                createdRows[invocation.arguments[0] as Long]
            }
            onBlocking { fetch(any<List<Long>>()) } doSuspendableAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                (invocation.arguments[0] as List<Long>).mapNotNull { createdRows[it] }
            }
            onBlocking { fetch(any<String>()) } doSuspendableAnswer { invocation ->
                createdRows.values.firstOrNull { it.remoteId == invocation.arguments[0] }
            }
        }
    }

    protected fun buildViewModel(
        taskId: Long = 0L,
        remoteId: String = "",
        listId: Long? = null,
        tagUuid: String? = null,
        isSubtaskDraft: Boolean = false,
    ) = TaskEditViewModel(
        taskId = taskId,
        remoteId = remoteId,
        listId = listId,
        tagUuid = tagUuid,
        isSubtaskDraft = isSubtaskDraft,
        taskDao = taskDao,
        taskSaver = taskSaver,
        caldavDao = caldavDao,
        taskMover = taskMover,
        tagDao = tagDao,
        tagDataDao = tagDataDao,
        alarmDao = alarmDao,
        alarmService = alarmService,
        appPreferences = appPreferences,
        externalScope = CoroutineScope(testDispatcher),
        pendingSaves = pendingSaves,
        taskCompleter = taskCompleter,
        taskDeleter = taskDeleter,
        treeRegistry = treeRegistry,
        untitled = { NO_TITLE },
        subtaskWriter = subtaskWriter,
        refreshFlow = refreshes,
    ).also { viewModel = it }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    protected suspend fun stubTaskDefaults(
        defaultAlarms: List<Alarm> = emptyList(),
        defaultList: String? = null,
        defaultHideUntil: Int = Task.HIDE_UNTIL_NONE,
        addTasksToTop: Boolean = true,
    ) {
        whenever(appPreferences.taskDefaults()).thenReturn(
            TaskDefaultSettings(
                defaultAlarms = defaultAlarms,
                defaultList = defaultList,
                defaultHideUntil = defaultHideUntil,
                addTasksToTop = addTasksToTop,
            )
        )
    }

    protected fun TestScope.initializeNew() {
        buildViewModel()
        advanceUntilIdle()
    }

    protected suspend fun TestScope.initializeExisting(
        id: Long = 42,
        title: String = "Existing",
    ) {
        whenever(taskDao.fetch(id)).thenReturn(Task(id = id, title = title))
        whenever(caldavDao.getTask(id)).thenReturn(null)
        buildViewModel(taskId = id)
        advanceUntilIdle()
    }

    protected fun collectSaveFailures(): () -> Int = { pendingSaves.saveFailures.value }

    protected fun TestScope.awaitClose(): () -> Boolean {
        var received = false
        val job = CoroutineScope(testDispatcher).launch {
            viewModel.closeEvents.first()
            received = true
        }
        coroutineContext.job.invokeOnCompletion { job.cancel() }
        return { received }
    }

    protected suspend fun TestScope.initializeNewWithFailingSave() {
        initializeNew()
        whenever(taskDao.createNew(any())).thenThrow(RuntimeException("db error"))
        viewModel.setTitle("Will fail")
    }
}

class OpenSubtaskTrees(private val registry: SubtaskTreeRegistry) {
    fun get(key: String): SubtaskNode? = registry.openTrees.firstNotNullOfOrNull { it.get(key) }

    fun holds(id: Long, remoteId: String?): Boolean = registry.holds(id, remoteId)

    fun isEmpty(): Boolean = registry.openTrees.all { it.nodes.value.isEmpty() }

    fun isRearranged(rootKey: String): Boolean =
        registry.openTrees.any { it.isRearranged(rootKey) }

    private fun holding(key: String): SubtaskTrees? =
        registry.openTrees.firstOrNull { it.get(key) != null }

    fun takePending(key: String): PendingTask? = holding(key)?.takePending(key)

    fun settle(created: Map<String, Task>): Map<String, SubtaskNode> =
        created.keys.firstNotNullOfOrNull { holding(it) }?.settle(created).orEmpty()
}
