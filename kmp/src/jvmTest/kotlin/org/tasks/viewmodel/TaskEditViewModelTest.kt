package org.tasks.viewmodel

import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.check
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.tasks.data.TaskMover
import org.tasks.data.TaskSaver
import com.todoroo.astrid.alarms.AlarmService
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.SYNC_TAGS
import org.tasks.data.entity.TagData
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.DatePickerPreferences
import org.tasks.service.TaskCompleter
import org.tasks.service.TaskDeleter
import org.tasks.compose.pickers.DAY_BEFORE_DUE
import org.tasks.compose.pickers.DUE_DATE
import org.tasks.compose.pickers.DUE_TIME
import org.tasks.compose.pickers.NO_DAY
import org.tasks.compose.pickers.NO_TIME
import org.tasks.compose.pickers.WEEK_BEFORE_DUE
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.TagFilter
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_HOUR
import org.tasks.time.minusDays
import org.tasks.time.noon
import org.tasks.time.plusDays
import org.tasks.time.startOfDay
import org.tasks.time.withMillisOfDay

@OptIn(ExperimentalCoroutinesApi::class)
class TaskEditViewModelTest {

    private val NINE_AM_WITH_TIME = 9 * 60 * 60 * 1000 + 1000

    /** The row id the fake [TaskDao.createNew] stamps onto a newly created task. */
    private val NEW_TASK_ID = 55L

    private val mergedFields = setOf(
        "title", "priority", "dueDate", "hideUntil", "completionDate", "deletionDate", "notes",
        "estimatedSeconds", "elapsedSeconds", "timerStart", "ringFlags", "recurrence", "repeatFrom",
        "calendarURI", "isCollapsed", "parent", "order", "readOnly", "modificationDate", "reminderLast",
    )
    private val notMergedFields = setOf(
        "id", "creationDate", "remoteId", "transitoryData",
    )

    private fun Task.fieldValue(name: String): Any? =
        Task::class.java.getDeclaredField(name).apply { isAccessible = true }.get(this)

    private val testDispatcher = StandardTestDispatcher()
    private val taskDao: TaskDao = mock()
    private val taskSaver: TaskSaver = mock()
    private val caldavDao: CaldavDao = mock()
    private val taskMover: TaskMover = mock()
    private val tagDao: TagDao = mock()
    private val tagDataDao: TagDataDao = mock()
    private val alarmDao: AlarmDao = mock()
    private val alarmService: AlarmService = mock()
    private val appPreferences: AppPreferences = mock()
    private val taskCompleter: TaskCompleter = mock()
    private val taskDeleter: TaskDeleter = mock()

    private lateinit var viewModel: TaskEditViewModel
    private lateinit var pendingSaves: PendingTaskSaves

    /** Rows created through [TaskDao.createNew], so that a later fetch finds them. */
    private val createdRows = mutableMapOf<Long, Task>()

    private val testCalendar = CaldavCalendar(account = "acct-1", uuid = "cal-1", name = "Test")
    private val seedCalendar = CaldavCalendar(id = 7, account = "acct-1", uuid = "cal-7", name = "Seed")
    private val testAccount = CaldavAccount(uuid = "acct-1")

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
        whenever(appPreferences.isDefaultDueTimeEnabled()).thenReturn(false)
        whenever(appPreferences.defaultRandomHours()).thenReturn(0)
        whenever(appPreferences.defaultRingMode()).thenReturn(0)
        whenever(alarmDao.getAlarms(any<Long>())).thenReturn(emptyList())
        whenever(alarmDao.watchAlarms(any())).thenReturn(MutableSharedFlow())
        // inTransaction exists only to wrap its block, and a mock would swallow it - taking the
        // row creation the editor does inside it along with it.
        taskDao.stub {
            onBlocking { inTransaction<Any?>(any()) } doSuspendableAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                (invocation.arguments[0] as suspend () -> Any?).invoke()
            }
            // createNew stamps the row id onto the task it is handed, and the row exists once it
            // returns. The editor depends on both - isNew flips false, and every later save
            // re-reads the row - so a mock that quietly did neither let tests pass that production
            // could not: a retry after a failed save was still treated as a creation.
            onBlocking { createNew(any()) } doSuspendableAnswer { invocation ->
                val task = invocation.arguments[0] as Task
                task.id = NEW_TASK_ID
                createdRows[NEW_TASK_ID] = task.copy()
                NEW_TASK_ID
            }
            // The default for any id nothing else has stubbed. Specific stubbings registered later
            // take precedence, so `whenever(taskDao.fetch(42L))` still wins for 42.
            onBlocking { fetch(any<Long>()) } doSuspendableAnswer { invocation ->
                createdRows[invocation.arguments[0] as Long]
            }
        }
    }

    private fun buildViewModel(
        taskId: Long = 0L,
        remoteId: String = "",
        listId: Long? = null,
        tagUuid: String? = null,
    ) = TaskEditViewModel(
        taskId = taskId,
        remoteId = remoteId,
        listId = listId,
        tagUuid = tagUuid,
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
    ).also { viewModel = it }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region helpers

    private fun TestScope.initializeNew() {
        buildViewModel()
        advanceUntilIdle()
    }

    private suspend fun TestScope.initializeExisting(
        id: Long = 42,
        title: String = "Existing",
    ) {
        whenever(taskDao.fetch(id)).thenReturn(Task(id = id, title = title))
        whenever(caldavDao.getTask(id)).thenReturn(null)
        buildViewModel(taskId = id)
        advanceUntilIdle()
    }

    /**
     * Reads the count of failures still waiting to be shown. Nothing has to subscribe up front:
     * failures are held until acknowledged precisely because the editor that started the save - and
     * on Android the composition that would have reported it - is usually gone by the time it
     * fails.
     */
    private fun collectSaveFailures(): () -> Int = { pendingSaves.saveFailures.value }

    private fun TestScope.awaitClose(): () -> Boolean {
        var received = false
        val job = CoroutineScope(testDispatcher).launch {
            viewModel.closeEvents.first()
            received = true
        }
        coroutineContext.job.invokeOnCompletion { job.cancel() }
        return { received }
    }

    private suspend fun TestScope.initializeNewWithFailingSave() {
        initializeNew()
        whenever(taskDao.createNew(any())).thenThrow(RuntimeException("db error"))
        viewModel.setTitle("Will fail")
    }

    // endregion

    // region initialize

    @Test
    fun initializeNewTask() = runTest(testDispatcher) {
        initializeNew()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isNew)
        assertEquals(
            CaldavFilter(calendar = testCalendar, account = testAccount),
            state.list,
        )
    }

    @Test
    fun initializeExistingTask() = runTest(testDispatcher) {
        initializeExisting(title = "My Task")

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("My Task", state.task.title)
        assertFalse(state.isNew)
    }

    // endregion

    private val workTag = TagData(name = "Work", remoteId = "tag-work")

    private fun TestScope.initializeNewWith(listId: Long? = null, tagUuid: String? = null) {
        buildViewModel(listId = listId, tagUuid = tagUuid)
        advanceUntilIdle()
    }

    @Test
    fun newTaskFromTagFilterPreFillsTag() = runTest(testDispatcher) {
        initializeNewWith(tagUuid = workTag.remoteId)

        val state = viewModel.state.value
        assertEquals(listOf(workTag), state.tags)
        assertFalse(state.hasChanges)
    }

    @Test
    fun newTaskFromCaldavFilterSeedsListWithoutTags() = runTest(testDispatcher) {
        initializeNewWith(listId = seedCalendar.id)

        val state = viewModel.state.value
        assertEquals(CaldavFilter(calendar = seedCalendar, account = testAccount), state.list)
        assertTrue(state.tags.isEmpty())
    }

    @Test
    fun newTaskFallsBackToFirstListWhenSeedListIsGone() = runTest(testDispatcher) {
        whenever(caldavDao.getCalendarById(404)).thenReturn(null)

        initializeNewWith(listId = 404)

        assertEquals(
            CaldavFilter(calendar = testCalendar, account = testAccount),
            viewModel.state.value.list,
        )
    }

    @Test
    fun saveAppliesPreFilledTag() = runTest(testDispatcher) {
        initializeNewWith(tagUuid = workTag.remoteId)

        viewModel.setTitle("Tagged task")
        viewModel.save()
        advanceUntilIdle()

        verify(tagDao).applyTags(
            check { assertEquals("Tagged task", it.title) },
            check { assertEquals(listOf("Work"), it.map(TagData::name)) },
            any(),
        )
        verify(taskSaver).save(
            check { assertTrue(it.checkTransitory(SYNC_TAGS)) },
            anyOrNull(),
            any(),
        )
    }

    @Test
    fun emptyTaskFromTagFilterIsNotSaved() = runTest(testDispatcher) {
        initializeNewWith(tagUuid = workTag.remoteId)
        val closed = awaitClose()

        viewModel.save()
        advanceUntilIdle()

        assertTrue(closed())
        verify(taskDao, never()).createNew(any())
        verify(tagDao, never()).applyTags(any(), any<Collection<TagData>>(), any())
    }

    @Test
    fun removingTagFromExistingTaskAppliesEmptyTags() = runTest(testDispatcher) {
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "Existing"))
        whenever(caldavDao.getTask(42)).thenReturn(null)
        whenever(tagDataDao.getTagDataForTask(42)).thenReturn(listOf(workTag))
        buildViewModel(taskId = 42)
        advanceUntilIdle()
        assertEquals(listOf(workTag), viewModel.state.value.tags)

        viewModel.setTags(emptyList())
        assertTrue(viewModel.state.value.hasChanges)
        viewModel.save()
        advanceUntilIdle()

        verify(tagDao).applyTags(
            check { assertEquals(42, it.id) },
            check { assertTrue(it.isEmpty()) },
            any(),
        )
    }

    // region priority

    @Test
    fun setPriorityMarksHasChanges() = runTest(testDispatcher) {
        initializeNew()

        viewModel.setPriority(Task.Priority.HIGH)

        assertEquals(Task.Priority.HIGH, viewModel.state.value.task.priority)
        assertTrue(viewModel.state.value.hasChanges)
    }

    @Test
    fun saveNewTaskWithPriority() = runTest(testDispatcher) {
        initializeNew()

        viewModel.setTitle("Prioritized")
        viewModel.setPriority(Task.Priority.HIGH)
        viewModel.save()
        advanceUntilIdle()

        verify(taskDao).createNew(check { assertEquals(Task.Priority.HIGH, it.priority) })
    }

    @Test
    fun savePriorityChangeToExistingTask() = runTest(testDispatcher) {
        initializeExisting()

        viewModel.setPriority(Task.Priority.MEDIUM)
        viewModel.save()
        advanceUntilIdle()

        verify(taskSaver).save(
            check { assertEquals(Task.Priority.MEDIUM, it.priority) },
            any(),
            any(),
        )
    }

    // endregion

    // region save

    @Test
    fun saveCreatesNewTask() = runTest(testDispatcher) {
        initializeNew()

        viewModel.setTitle("New Task")
        viewModel.save()
        advanceUntilIdle()

        verify(taskDao).createNew(check { assertEquals("New Task", it.title) })
        verify(caldavDao).insert(task = any(), caldavTask = any(), addToTop = any())
        verify(taskSaver).save(check { assertEquals("New Task", it.title) }, anyOrNull(), any())
    }

    @Test
    fun saveUpdatesExistingTask() = runTest(testDispatcher) {
        initializeExisting()

        viewModel.setTitle("Updated")
        viewModel.save()
        advanceUntilIdle()

        verify(taskDao, never()).createNew(any())
        verify(taskSaver).save(check { assertEquals("Updated", it.title) }, any(), any())
    }

    @Test
    fun saveSkippedWithoutChanges() = runTest(testDispatcher) {
        initializeNew()
        val closed = awaitClose()

        viewModel.save()
        advanceUntilIdle()

        assertTrue(closed())
        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    @Test
    fun saveNewTaskWithDescription() = runTest(testDispatcher) {
        initializeNew()

        viewModel.setTitle("Described")
        viewModel.setDescription("Some notes")
        viewModel.save()
        advanceUntilIdle()

        verify(taskDao).createNew(check { assertEquals("Some notes", it.notes) })
    }

    @Test
    fun saveResetsHasChanges() = runTest(testDispatcher) {
        initializeNew()

        viewModel.setTitle("Something")
        assertTrue(viewModel.state.value.hasChanges)

        viewModel.save()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.hasChanges)
    }

    @Test
    fun consecutiveSaveIsNoOp() = runTest(testDispatcher) {
        initializeNew()

        viewModel.setTitle("Once")
        viewModel.save()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.hasChanges)

        viewModel.save()
        advanceUntilIdle()

        verify(taskDao).createNew(check { assertEquals("Once", it.title) })
    }

    @Test
    fun saveEmitsClose() = runTest(testDispatcher) {
        initializeNew()
        val closed = awaitClose()

        viewModel.save()
        advanceUntilIdle()

        assertTrue(closed())
    }

    @Test
    fun saveWhileLoadingEmitsCloseWithoutSaving() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        whenever(caldavDao.getTask(42)).thenReturn(null)
        taskDao.stub {
            onBlocking { fetch(42L) } doSuspendableAnswer {
                gate.await()
                Task(id = 42, title = "Loaded")
            }
        }

        buildViewModel(taskId = 42)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isLoading)
        val closed = awaitClose()

        viewModel.save()
        advanceUntilIdle()

        assertTrue(closed())
        verify(taskSaver, never()).save(any(), anyOrNull(), any())
        verify(taskDao, never()).createNew(any())

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun saveDoesNotCloseOnFailure() = runTest(testDispatcher) {
        initializeNewWithFailingSave()
        val closed = awaitClose()

        viewModel.save()
        advanceUntilIdle()

        assertFalse(closed())
    }

    @Test
    fun saveFailureIsReported() = runTest(testDispatcher) {
        initializeNewWithFailingSave()
        val failures = collectSaveFailures()

        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, failures())
    }

    @Test
    fun everySaveFailureIsReportedSeparately() = runTest(testDispatcher) {
        initializeNewWithFailingSave()
        val failures = collectSaveFailures()

        viewModel.save()
        advanceUntilIdle()
        viewModel.save()
        advanceUntilIdle()

        assertEquals(2, failures())
    }

    /**
     * What shutdown compares against. [PendingTaskSaves.saveFailures] is what is still owed to the
     * user, so it goes down as well as up - and a failure arriving while an older one was being
     * acknowledged compared equal to a snapshot taken before the flush, which let the desktop app
     * quit with the edit lost.
     */
    @Test
    fun acknowledgingAFailureDoesNotHideALaterOne() = runTest(testDispatcher) {
        initializeNewWithFailingSave()

        viewModel.save()
        advanceUntilIdle()
        val before = pendingSaves.totalSaveFailures.value
        pendingSaves.acknowledgeSaveFailure()
        assertEquals(0, pendingSaves.saveFailures.value)

        viewModel.save()
        advanceUntilIdle()

        assertTrue(pendingSaves.totalSaveFailures.value > before)
    }

    @Test
    fun saveFailureNotReportedOnSuccess() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Will succeed")
        val failures = collectSaveFailures()

        viewModel.save()
        advanceUntilIdle()

        assertEquals(0, failures())
    }

    // endregion

    // region switch task (saveCurrentTask + initialize)

    @Test
    fun switchSavesDirtyEdits() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Unsaved work")

        viewModel.saveCurrentTask()
        advanceUntilIdle()

        verify(taskDao).createNew(check { assertEquals("Unsaved work", it.title) })
        verify(taskSaver).save(check { assertEquals("Unsaved work", it.title) }, anyOrNull(), any())
    }

    @Test
    fun switchSkippedWithoutChanges() = runTest(testDispatcher) {
        initializeNew()

        viewModel.saveCurrentTask()
        advanceUntilIdle()

        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    @Test
    fun switchSavesDescription() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Described")
        viewModel.setDescription("Some notes")

        viewModel.saveCurrentTask()
        advanceUntilIdle()

        verify(taskDao).createNew(check { assertEquals("Some notes", it.notes) })
    }

    @Test
    fun switchFailureIsReported() = runTest(testDispatcher) {
        initializeNewWithFailingSave()
        val failures = collectSaveFailures()

        viewModel.saveCurrentTask()
        advanceUntilIdle()

        assertEquals(1, failures())
    }

    /**
     * The reason [PendingTaskSaves.withLock] exists: opening a task while the editor it replaced is
     * still writing that same task must not read the pre-save row back.
     */
    @Test
    fun replacementEditorWaitsForTheDepartingSave() = runTest(testDispatcher) {
        initializeExisting(id = 42, title = "Original")
        val departing = viewModel
        departing.setTitle("Modified")
        // Park the save mid-flight so the replacement's load has something to race.
        val saveGate = CompletableDeferred<Unit>()
        whenever(taskSaver.save(any(), anyOrNull(), any())).doSuspendableAnswer { invocation ->
            saveGate.await()
            val saved = invocation.arguments[0] as Task
            whenever(taskDao.fetch(42L)).thenReturn(Task(id = 42, title = saved.title))
            Unit
        }

        departing.persistCurrentTask()
        val replacement = buildViewModel(taskId = 42)
        advanceUntilIdle()

        assertTrue(
            "load must not read the row while a save for the same task is in flight",
            replacement.state.value.isLoading,
        )

        saveGate.complete(Unit)
        advanceUntilIdle()

        assertEquals("Modified", replacement.state.value.task.title)
    }

    /**
     * The [PendingTaskSaves.enqueueLocked] path that cannot claim the lock synchronously. It only
     * joins the mutex's queue once a worker thread picks it up, so ordering against a load is not
     * the lock's to decide - the load waits on the flush's own bookkeeping instead.
     */
    @Test
    fun replacementEditorWaitsForASaveThatCouldNotClaimTheLock() = runTest(testDispatcher) {
        initializeExisting(id = 42, title = "Original")
        val departing = viewModel
        val firstSaveGate = CompletableDeferred<Unit>()
        whenever(taskSaver.save(any(), anyOrNull(), any())).doSuspendableAnswer { invocation ->
            val saved = invocation.arguments[0] as Task
            if (saved.title == "First") {
                firstSaveGate.await()
            }
            whenever(taskDao.fetch(42L)).thenReturn(Task(id = 42, title = saved.title))
            Unit
        }

        departing.setTitle("First")
        departing.persistCurrentTask()
        advanceUntilIdle()
        // The parked save above is holding the lock, so this one cannot claim it on the way in.
        departing.setTitle("Second")
        departing.persistCurrentTask()

        val replacement = buildViewModel(taskId = 42)
        advanceUntilIdle()

        assertTrue(
            "load must not read the row while a save for the same task is still owed",
            replacement.state.value.isLoading,
        )

        firstSaveGate.complete(Unit)
        advanceUntilIdle()

        assertEquals("Second", replacement.state.value.task.title)
    }

    @Test
    fun savesForDifferentTasksDoNotBlockEachOther() = runTest(testDispatcher) {
        initializeExisting(id = 42, title = "Slow")
        val slow = viewModel
        slow.setTitle("Slow edit")
        val saveGate = CompletableDeferred<Unit>()
        whenever(taskSaver.save(any(), anyOrNull(), any())).doSuspendableAnswer { saveGate.await() }

        slow.persistCurrentTask()
        advanceUntilIdle()

        // A different task, so it must not be queued behind the parked save above.
        whenever(taskDao.fetch(99L)).thenReturn(Task(id = 99, title = "Other"))
        whenever(caldavDao.getTask(99L)).thenReturn(null)
        val other = buildViewModel(taskId = 99)
        advanceUntilIdle()

        assertFalse(other.state.value.isLoading)
        assertEquals("Other", other.state.value.task.title)
        saveGate.complete(Unit)
        advanceUntilIdle()
    }

    /**
     * The key shape production actually uses. [Task.uuid] returns [Task.NO_UUID] for any row whose
     * remoteId is null or empty, and the task list puts that straight into the destination - so
     * taking it at face value put every such task on one shared lock.
     */
    @Test
    fun savesForTasksWithoutRemoteIdsDoNotBlockEachOther() = runTest(testDispatcher) {
        whenever(taskDao.fetch(42L)).thenReturn(Task(id = 42, title = "Slow", remoteId = null))
        whenever(caldavDao.getTask(42L)).thenReturn(null)
        val slow = buildViewModel(taskId = 42, remoteId = Task.NO_UUID)
        advanceUntilIdle()
        slow.setTitle("Slow edit")
        val saveGate = CompletableDeferred<Unit>()
        whenever(taskSaver.save(any(), anyOrNull(), any())).doSuspendableAnswer { saveGate.await() }

        slow.persistCurrentTask()
        advanceUntilIdle()

        whenever(taskDao.fetch(99L)).thenReturn(Task(id = 99, title = "Other", remoteId = null))
        whenever(caldavDao.getTask(99L)).thenReturn(null)
        val other = buildViewModel(taskId = 99, remoteId = Task.NO_UUID)
        advanceUntilIdle()

        assertFalse(
            "two unrelated tasks without remoteIds must not share one lock",
            other.state.value.isLoading,
        )
        assertEquals("Other", other.state.value.task.title)
        saveGate.complete(Unit)
        advanceUntilIdle()
    }

    /**
     * A destination carrying neither a row id nor a uuid identifies nothing, so two of them are two
     * unrelated new tasks. Collapsing both to one key put them on a shared lock, and the second
     * one's load then flushed the first's half-typed task into the list.
     */
    @Test
    fun editorsOnAnUnidentifiedDestinationDoNotShareALock() = runTest(testDispatcher) {
        initializeNew()
        val first = viewModel
        first.setTitle("Half typed")

        buildViewModel()
        advanceUntilIdle()

        verify(taskDao, never()).createNew(any())
        assertTrue(first.state.value.hasChanges)
    }

    /**
     * The ordering the lock cannot provide on its own. The nav host builds the replacement - and
     * runs its load - during the composition *before* it disposes the editor it replaces, so by the
     * time that editor is cleared the replacement has already read the row. Waiting for the lock is
     * no help against a save that has not been asked for yet, so loading asks for it.
     */
    @Test
    fun loadCommitsAnEditStillHeldByAnotherEditorOnTheSameTask() = runTest(testDispatcher) {
        initializeExisting(id = 42, title = "Original")
        val departing = viewModel
        departing.setTitle("Modified")
        whenever(taskSaver.save(any(), anyOrNull(), any())).doSuspendableAnswer { invocation ->
            val saved = invocation.arguments[0] as Task
            whenever(taskDao.fetch(42L)).thenReturn(Task(id = 42, title = saved.title))
            Unit
        }

        // Deliberately no persistCurrentTask() and no onCleared(): the departing editor is still
        // alive and still holding the edit when the replacement loads.
        val replacement = buildViewModel(taskId = 42)
        advanceUntilIdle()

        assertEquals("Modified", replacement.state.value.task.title)
    }

    /**
     * A soft delete commits without taking the save lock, and the watch that would tell the editor
     * about it is a coroutine hop behind. A teardown save that wins that race used to write
     * deletionDate back to 0 - and, because the dirty check compares against the pre-delete row,
     * push the resurrected task to the server.
     */
    @Test
    fun teardownSaveDoesNotResurrectATaskDeletedWhileItWasQueued() = runTest(testDispatcher) {
        initializeExisting(id = 42, title = "Doomed")
        viewModel.setTitle("Edited before the delete landed")
        // The delete is committed, but nothing has merged it into state: state.deleted is still
        // false, exactly as it is between the write and the watch re-querying.
        whenever(taskDao.fetch(42L))
            .thenReturn(Task(id = 42, title = "Doomed", deletionDate = currentTimeMillis()))
        assertFalse(viewModel.state.value.deleted)

        viewModel.onCleared()
        advanceUntilIdle()

        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    /** Hard deletes leave no tombstone to find, so the missing row is the only signal. */
    @Test
    fun teardownSaveDoesNotWriteToAHardDeletedRow() = runTest(testDispatcher) {
        initializeExisting(id = 42, title = "Purged")
        viewModel.setTitle("Edited before the purge")
        whenever(taskDao.fetch(42L)).thenReturn(null)

        viewModel.onCleared()
        advanceUntilIdle()

        verify(taskSaver, never()).save(any(), anyOrNull(), any())
        assertTrue(viewModel.state.value.deleted)
    }

    @Test
    fun aSaveThatFindsTheRowHardDeletedClosesTheEditor() = runTest(testDispatcher) {
        initializeExisting(id = 42, title = "Purged")
        viewModel.setTitle("Edited before the purge")
        whenever(taskDao.fetch(42L)).thenReturn(null)
        val closed = awaitClose()

        viewModel.persistCurrentTask()
        advanceUntilIdle()

        assertTrue(closed())
        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    /** The same, for a delete that left a tombstone the re-read can see. */
    @Test
    fun aSaveThatFindsTheRowDeletedClosesTheEditor() = runTest(testDispatcher) {
        initializeExisting(id = 42, title = "Doomed")
        viewModel.setTitle("Edited before the delete landed")
        whenever(taskDao.fetch(42L))
            .thenReturn(Task(id = 42, title = "Doomed", deletionDate = currentTimeMillis()))
        val closed = awaitClose()

        viewModel.persistCurrentTask()
        advanceUntilIdle()

        assertTrue(closed())
        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    /**
     * The re-read is the only thing standing between a save and a full-row overwrite of a row it has
     * not checked, so a re-read that throws has to abort the save rather than let it run blind.
     */
    @Test
    fun aFailedReReadAbortsTheSave() = runTest(testDispatcher) {
        initializeExisting(id = 42, title = "Original")
        viewModel.setTitle("Locally edited")
        whenever(taskDao.fetch(42L)).thenThrow(RuntimeException("db error"))
        val failures = collectSaveFailures()

        viewModel.persistCurrentTask()
        advanceUntilIdle()

        verify(taskSaver, never()).save(any(), anyOrNull(), any())
        assertEquals(1, failures())
    }

    /** An external edit is merged rather than overwritten by the full-row update a save performs. */
    @Test
    fun teardownSaveMergesAnExternalEditItNeverSaw() = runTest(testDispatcher) {
        initializeExisting(id = 42, title = "Original")
        viewModel.setTitle("Locally edited")
        // Sync changed a field the user did not touch, and the watch has not delivered it yet.
        whenever(taskDao.fetch(42L))
            .thenReturn(Task(id = 42, title = "Original", notes = "Added by sync"))

        viewModel.onCleared()
        advanceUntilIdle()

        verify(taskSaver).save(
            check {
                assertEquals("Locally edited", it.title)
                assertEquals("Added by sync", it.notes)
            },
            anyOrNull(),
            any(),
        )
    }

    /** The same guarantee as [replacementEditorWaitsForTheDepartingSave], on a real remoteId. */
    @Test
    fun replacementEditorWaitsForTheDepartingSaveKeyedByRemoteId() = runTest(testDispatcher) {
        whenever(taskDao.fetch(42L))
            .thenReturn(Task(id = 42, title = "Original", remoteId = "uuid-42"))
        whenever(caldavDao.getTask(42L)).thenReturn(null)
        val departing = buildViewModel(taskId = 42, remoteId = "uuid-42")
        advanceUntilIdle()
        departing.setTitle("Modified")
        val saveGate = CompletableDeferred<Unit>()
        whenever(taskSaver.save(any(), anyOrNull(), any())).doSuspendableAnswer { invocation ->
            saveGate.await()
            val saved = invocation.arguments[0] as Task
            whenever(taskDao.fetch(42L))
                .thenReturn(Task(id = 42, title = saved.title, remoteId = "uuid-42"))
            Unit
        }

        departing.persistCurrentTask()
        val replacement = buildViewModel(taskId = 42, remoteId = "uuid-42")
        advanceUntilIdle()

        assertTrue(
            "load must not read the row while a save for the same task is in flight",
            replacement.state.value.isLoading,
        )

        saveGate.complete(Unit)
        advanceUntilIdle()

        assertEquals("Modified", replacement.state.value.task.title)
    }

    /**
     * Neither lookup filters deleted rows, and a new-task destination keeps resolving by remoteId
     * after its own teardown save created the row - so a task deleted in between used to load as if
     * it were live, and the next save wrote onto the tombstone.
     */
    @Test
    fun loadingADeletedTaskDoesNotWriteToTheTombstone() = runTest(testDispatcher) {
        whenever(taskDao.fetch("uuid-gone")).thenReturn(
            Task(
                id = 42,
                title = "Deleted elsewhere",
                remoteId = "uuid-gone",
                deletionDate = currentTimeMillis(),
            )
        )
        whenever(caldavDao.getTask(42L)).thenReturn(null)
        buildViewModel(remoteId = "uuid-gone")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.deleted)

        viewModel.setTitle("Edited anyway")
        viewModel.saveCurrentTask()
        advanceUntilIdle()

        verify(taskSaver, never()).save(any(), anyOrNull(), any())
        verify(taskDao, never()).createNew(any())
    }

    @Test
    fun newTaskRowAndCaldavRowAreCreatedInOneTransaction() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Created")

        viewModel.saveCurrentTask()
        advanceUntilIdle()

        verify(taskDao).inTransaction<Any?>(any())
    }

    /**
     * A save that fails after the row exists must not try to create it again: the unique index on
     * remoteId would reject the second insert, leaving the editor permanently unable to save.
     */
    @Test
    fun failureAfterTheRowExistsDoesNotCreateTheTaskTwice() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Created")
        whenever(taskSaver.save(any(), anyOrNull(), any()))
            .thenThrow(RuntimeException("sync error"))

        viewModel.saveCurrentTask()
        advanceUntilIdle()
        viewModel.saveCurrentTask()
        advanceUntilIdle()

        verify(taskDao, times(1)).createNew(any())
    }

    /**
     * Marking the row dirty is TaskSaver's job and it only does it for a creation, so a retry has to
     * arrive as one. Recording the state as clean the moment the row existed made every retry
     * short-circuit on "nothing has changed" and report success: the task existed locally and no
     * synchronizer could ever see it.
     */
    @Test
    fun failedNewTaskSaveIsRetriedAsACreation() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Created")
        whenever(taskSaver.save(any(), anyOrNull(), any()))
            .thenThrow(RuntimeException("sync error"))

        viewModel.saveCurrentTask()
        advanceUntilIdle()
        viewModel.saveCurrentTask()
        advanceUntilIdle()

        verify(taskSaver, times(2)).save(
            check { assertEquals("Created", it.title) },
            isNull(),
            any(),
        )
    }

    /** And it stops retrying once one of them gets through. */
    @Test
    fun successfulRetryLeavesNothingOwing() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Created")
        whenever(taskSaver.save(any(), anyOrNull(), any()))
            .thenThrow(RuntimeException("sync error"))
        viewModel.saveCurrentTask()
        advanceUntilIdle()

        reset(taskSaver)
        viewModel.saveCurrentTask()
        advanceUntilIdle()
        viewModel.saveCurrentTask()
        advanceUntilIdle()

        verify(taskSaver, times(1)).save(any(), anyOrNull(), any())
    }

    @Test
    fun switchLoadsNewTaskAfterFailure() = runTest(testDispatcher) {
        initializeNewWithFailingSave()
        val failing = viewModel

        failing.saveCurrentTask()
        val replacement = buildViewModel()
        advanceUntilIdle()

        // Asserted on the replacement explicitly. Reading it back through the shared `viewModel`
        // field made this trivially true of any fresh editor, whether or not the failed save had
        // left the lock held.
        val state = replacement.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isNew)
        assertNull(state.task.title)
        // And the editor that failed still has the edit, so nothing swapped underneath us.
        assertEquals("Will fail", failing.state.value.task.title)
    }

    @Test
    fun switchFromExistingToExistingSaves() = runTest(testDispatcher) {
        initializeExisting(id = 42, title = "Original")
        viewModel.setTitle("Modified")

        viewModel.saveCurrentTask()
        initializeExisting(id = 99, title = "Other Task")

        verify(taskSaver).save(
            check { assertEquals("Modified", it.title) },
            check { assertEquals("Original", it.title) },
            any(),
        )
        assertEquals("Other Task", viewModel.state.value.task.title)
    }

    @Test
    fun twoNewTasksEditIndependently() = runTest(testDispatcher) {
        val first = buildViewModel(remoteId = "uuid-1")
        advanceUntilIdle()
        first.setTitle("First task")
        val second = buildViewModel(remoteId = "uuid-2")
        advanceUntilIdle()
        second.setTitle("Second task")

        assertEquals("First task", first.state.value.task.title)
        assertEquals("Second task", second.state.value.task.title)
    }

    @Test
    fun newTaskKeepsDestinationUuid() = runTest(testDispatcher) {
        buildViewModel(remoteId = "uuid-1")
        advanceUntilIdle()

        assertEquals("uuid-1", viewModel.state.value.task.remoteId)
    }

    @Test
    fun newTaskDestinationReopensSavedTask() = runTest(testDispatcher) {
        whenever(taskDao.fetch("uuid-1")).thenReturn(Task(id = 7, title = "Already saved"))
        whenever(caldavDao.getTask(7)).thenReturn(null)

        buildViewModel(remoteId = "uuid-1")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Already saved", state.task.title)
        assertFalse(state.isNew)
    }

    // endregion

    // region watch / merge

    @Test
    fun mergeAdoptsUnmodifiedFieldFromDb() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        initializeExisting(id = 42, title = "Original")

        watchFlow.emit(viewModel.state.value.task.copy(title = "Updated externally"))
        advanceUntilIdle()

        assertEquals("Updated externally", viewModel.state.value.task.title)
    }

    @Test
    fun mergePreservesUserModifiedField() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        initializeExisting(id = 42, title = "Original")

        viewModel.setTitle("User edit")
        watchFlow.emit(viewModel.state.value.originalTask.copy(title = "DB edit"))
        advanceUntilIdle()

        assertEquals("User edit", viewModel.state.value.task.title)
    }

    @Test
    fun mergeUpdatesOriginalTask() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        initializeExisting(id = 42, title = "Original")

        val dbTask = viewModel.state.value.task.copy(priority = 3)
        watchFlow.emit(dbTask)
        advanceUntilIdle()

        assertEquals(dbTask, viewModel.state.value.originalTask)
    }

    @Test
    fun mergePreservesHasChangesForUserEdits() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        initializeExisting(id = 42, title = "Original")

        viewModel.setTitle("User edit")
        watchFlow.emit(viewModel.state.value.originalTask.copy(priority = 3))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.hasChanges)
        assertEquals("User edit", viewModel.state.value.task.title)
        assertEquals(3, viewModel.state.value.task.priority)
    }

    @Test
    fun mergeCoversAllTaskFields() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        initializeExisting(id = 42)

        val original = viewModel.state.value.task
        val dbTask = Task(
            id = 42,
            title = "db",
            priority = Task.Priority.HIGH,
            dueDate = 100L,
            hideUntil = 200L,
            creationDate = 300L,
            modificationDate = 400L,
            completionDate = 500L,
            deletionDate = 0L,
            notes = "db notes",
            estimatedSeconds = 600,
            elapsedSeconds = 700,
            timerStart = 800L,
            ringFlags = 1,
            reminderLast = 900L,
            recurrence = "FREQ=DAILY",
            repeatFrom = 1,
            calendarURI = "content://cal/1",
            remoteId = "db-uuid",
            isCollapsed = true,
            parent = 1000L,
            order = 1100L,
            readOnly = true,
        )

        val unexercised = (mergedFields - "deletionDate").filter {
            dbTask.fieldValue(it) == original.fieldValue(it)
        }
        assertEquals("merged field(s) not exercised by dbTask", emptyList<String>(), unexercised)

        watchFlow.emit(dbTask)
        advanceUntilIdle()

        val state = viewModel.state.value
        // id, creationDate, and remoteId are intentionally not merged.
        // A new Task field missing from mergeDbUpdate will fail here.
        val expected = dbTask.copy(
            creationDate = original.creationDate,
            remoteId = original.remoteId,
        )
        assertEquals(expected, state.task)
        assertEquals(dbTask, state.originalTask)
    }

    @Test
    fun everyTaskFieldIsClassifiedForMerge() {
        val declared = Task::class.java.declaredFields
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()
        assertEquals("unclassified Task field(s)", emptySet<String>(), declared - mergedFields - notMergedFields)
        assertEquals(
            "stale field name(s) in this test",
            emptySet<String>(),
            (mergedFields + notMergedFields) - declared,
        )
    }

    @Test
    fun sameEditableContentAsMatchesFieldClassification() {
        val base = Task(id = 1, remoteId = "rid-1", creationDate = 100L)
        assertTrue(base.copy().sameEditableContentAs(base))
        Task::class.java.declaredFields
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .forEach { field ->
                val mutated = base.withFieldMutated(field.name)
                assertEquals(
                    "sameEditableContentAs classification mismatch for ${field.name}",
                    field.name in notMergedFields,
                    mutated.sameEditableContentAs(base),
                )
            }
    }

    private fun Task.withFieldMutated(name: String): Task {
        val copy = copy()
        val field = Task::class.java.getDeclaredField(name).apply { isAccessible = true }
        val current = field.get(copy)
        field.set(copy, when (field.type) {
            Long::class.javaPrimitiveType -> (current as Long) + 1L
            Int::class.javaPrimitiveType -> (current as Int) + 1
            Boolean::class.javaPrimitiveType -> !(current as Boolean)
            Long::class.javaObjectType -> (current as Long?)?.plus(1L) ?: 1L
            String::class.java -> (current as String?)?.plus("!") ?: "changed"
            HashMap::class.java -> hashMapOf<String, Any>("changed" to true)
            else -> error("unhandled field type ${field.type} for $name")
        })
        return copy
    }

    @Test
    fun mergeIgnoresIdentityOnlyDbChange() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        initializeExisting(id = 42, title = "Original")
        assertFalse(viewModel.state.value.hasChanges)

        val before = viewModel.state.value.task
        watchFlow.emit(
            viewModel.state.value.originalTask.copy(remoteId = "new-uuid", creationDate = 999L)
        )
        advanceUntilIdle()

        assertFalse(viewModel.state.value.hasChanges)
        assertEquals(before, viewModel.state.value.task)
    }

    @Test
    fun externalDeletionClosesEditor() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        initializeExisting(id = 42)
        val closed = awaitClose()

        watchFlow.emit(viewModel.state.value.task.copy(deletionDate = 1000L))
        advanceUntilIdle()

        assertTrue(closed())
    }

    @Test
    fun externalDeletionDoesNotMerge() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        initializeExisting(id = 42)

        val before = viewModel.state.value
        watchFlow.emit(before.task.copy(deletionDate = 1000L, title = "changed"))
        advanceUntilIdle()

        assertEquals(before.task, viewModel.state.value.task)
    }

    @Test
    fun mergeAdoptsExternalStartWhenLocalStartCoincidesWithDueDay() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "t", dueDate = due))
        whenever(caldavDao.getTask(42)).thenReturn(null)
        buildViewModel(taskId = 42)
        advanceUntilIdle()

        viewModel.setStartDate(due.startOfDay(), NO_TIME)
        viewModel.save()
        advanceUntilIdle()

        val externalStart = today.minusDays(1)
        watchFlow.emit(viewModel.state.value.originalTask.copy(hideUntil = externalStart))
        advanceUntilIdle()

        assertEquals(externalStart, viewModel.state.value.task.hideUntil)
    }

    @Test
    fun mergeKeepsUnsavedLocalStartEditAgainstConcurrentExternalStartChange() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        val absoluteStart = today.plusDays(3)
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "t", dueDate = due, hideUntil = absoluteStart))
        whenever(caldavDao.getTask(42)).thenReturn(null)
        buildViewModel(taskId = 42)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.hasChanges)

        val localStart = today.plusDays(2)
        viewModel.setStartDate(localStart, NO_TIME)
        assertTrue(viewModel.state.value.hasChanges)

        val externalStart = today.plusDays(5)
        watchFlow.emit(viewModel.state.value.originalTask.copy(hideUntil = externalStart))
        advanceUntilIdle()

        assertEquals(localStart, viewModel.state.value.task.hideUntil)
        assertTrue(viewModel.state.value.hasChanges)

        viewModel.saveCurrentTask()
        advanceUntilIdle()
        verify(taskSaver).save(check { assertEquals(localStart, it.hideUntil) }, anyOrNull(), any())
    }

    @Test
    fun mergeKeepsServerStartOnExternalDueChangeForCaldav() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        val dayBefore = due.startOfDay().minusDays(1)
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "t", dueDate = due, hideUntil = dayBefore))
        whenever(caldavDao.getTask(42)).thenReturn(null)
        buildViewModel(taskId = 42)
        advanceUntilIdle()
        assertEquals(DAY_BEFORE_DUE, viewModel.state.value.startDay)

        val newDue = due.plusDays(7)
        watchFlow.emit(viewModel.state.value.originalTask.copy(dueDate = newDue))
        advanceUntilIdle()

        assertEquals(dayBefore, viewModel.state.value.task.hideUntil)
    }

    @Test
    fun mergeRelativeStartTracksExternalDueChangeForGoogleTasks() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        val googleCalendar = CaldavCalendar(account = "g-acct", uuid = "g-cal", name = "Google")
        val googleAccount = CaldavAccount(uuid = "g-acct", accountType = CaldavAccount.TYPE_GOOGLE_TASKS)
        whenever(caldavDao.getCalendars()).thenReturn(listOf(googleCalendar))
        whenever(caldavDao.getAccountByUuid("g-acct")).thenReturn(googleAccount)
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        val dayBefore = due.startOfDay().minusDays(1)
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "t", dueDate = due, hideUntil = dayBefore))
        whenever(caldavDao.getTask(42)).thenReturn(null)
        buildViewModel(taskId = 42)
        advanceUntilIdle()
        assertEquals(DAY_BEFORE_DUE, viewModel.state.value.startDay)

        val newDue = due.plusDays(7)
        watchFlow.emit(viewModel.state.value.originalTask.copy(dueDate = newDue))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(DAY_BEFORE_DUE, state.startDay)
        assertEquals(newDue.startOfDay().minusDays(1), state.task.hideUntil)
    }

    @Test
    fun mergeAdoptsExternalStartOnLocalDueEditWhenStartIsAbsolute() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        val absoluteStart = today.plusDays(3)
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "t", dueDate = due, hideUntil = absoluteStart))
        whenever(caldavDao.getTask(42)).thenReturn(null)
        buildViewModel(taskId = 42)
        advanceUntilIdle()

        val newDue = due.plusDays(2)
        viewModel.setDueDate(newDue)

        val externalStart = today.plusDays(5)
        watchFlow.emit(viewModel.state.value.originalTask.copy(hideUntil = externalStart))
        advanceUntilIdle()

        assertEquals(externalStart, viewModel.state.value.task.hideUntil)
        assertEquals(newDue, viewModel.state.value.task.dueDate)
    }

    @Test
    fun mergeKeepsRelativeStartOnLocalDueTimeEditAgainstConcurrentExternalStartForCaldav() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        val dayBefore = due.startOfDay().minusDays(1)
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "t", dueDate = due, hideUntil = dayBefore))
        whenever(caldavDao.getTask(42)).thenReturn(null)
        buildViewModel(taskId = 42)
        advanceUntilIdle()
        assertEquals(DAY_BEFORE_DUE, viewModel.state.value.startDay)

        val newDue = today.withMillisOfDay(NINE_AM_WITH_TIME)
        viewModel.setDueDate(newDue)

        val externalStart = today.plusDays(5)
        watchFlow.emit(viewModel.state.value.originalTask.copy(hideUntil = externalStart))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(DAY_BEFORE_DUE, state.startDay)
        assertEquals(dayBefore, state.task.hideUntil)
        assertEquals(newDue, state.task.dueDate)
    }

    @Test
    fun mergeAdoptsExternalStartWhenAbsoluteStartAlignsWithEditedDue() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        val absoluteStart = today.plusDays(1)
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "t", dueDate = due, hideUntil = absoluteStart))
        whenever(caldavDao.getTask(42)).thenReturn(null)
        buildViewModel(taskId = 42)
        advanceUntilIdle()

        val newDue = due.plusDays(2)
        viewModel.setDueDate(newDue)

        val externalStart = today.plusDays(5)
        watchFlow.emit(viewModel.state.value.originalTask.copy(hideUntil = externalStart))
        advanceUntilIdle()

        assertEquals(externalStart, viewModel.state.value.task.hideUntil)
        assertEquals(newDue, viewModel.state.value.task.dueDate)
    }

    @Test
    fun mergeKeepsAdoptedStartConsistentWithLocalDueOnFurtherEdit() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        val today = currentTimeMillis().startOfDay()
        val due = today.withMillisOfDay(NINE_AM_WITH_TIME)
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "t", dueDate = due))
        whenever(caldavDao.getTask(42)).thenReturn(null)
        buildViewModel(taskId = 42)
        advanceUntilIdle()

        val localDue = due.plusDays(5)
        viewModel.setDueDate(localDue)

        watchFlow.emit(viewModel.state.value.originalTask.copy(hideUntil = due))
        advanceUntilIdle()
        assertEquals(due, viewModel.state.value.task.hideUntil)

        viewModel.setDueDate(due.plusDays(10))
        advanceUntilIdle()
        assertEquals(due, viewModel.state.value.task.hideUntil)
    }

    @Test
    fun mergeKeepsAbsoluteLocalStartOnDueDayCleanAgainstExternalEcho() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "t", dueDate = due, hideUntil = due.startOfDay()))
        whenever(caldavDao.getTask(42)).thenReturn(null)
        buildViewModel(taskId = 42)
        advanceUntilIdle()
        assertEquals(DUE_DATE, viewModel.state.value.startDay)
        assertFalse(viewModel.state.value.hasChanges)

        viewModel.setStartDate(due.startOfDay(), NO_TIME)
        assertEquals(due.startOfDay(), viewModel.state.value.startDay)
        assertFalse(viewModel.state.value.hasChanges)

        watchFlow.emit(viewModel.state.value.originalTask.copy(modificationDate = 12345L))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(due.startOfDay(), state.startDay)
        assertEquals(due.startOfDay(), state.task.hideUntil)
        assertFalse(state.hasChanges)
    }

    private suspend fun TestScope.initializeGoogleTasksExisting(due: Long, hideUntil: Long): MutableSharedFlow<Task?> {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        val googleCalendar = CaldavCalendar(account = "g-acct", uuid = "g-cal", name = "Google")
        val googleAccount = CaldavAccount(uuid = "g-acct", accountType = CaldavAccount.TYPE_GOOGLE_TASKS)
        whenever(caldavDao.getCalendars()).thenReturn(listOf(googleCalendar))
        whenever(caldavDao.getAccountByUuid("g-acct")).thenReturn(googleAccount)
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "t", dueDate = due, hideUntil = hideUntil))
        whenever(caldavDao.getTask(42)).thenReturn(null)
        buildViewModel(taskId = 42)
        advanceUntilIdle()
        return watchFlow
    }

    @Test
    fun mergeExternalDueClearKeepsGoogleTasksStartAndStaysClean() = runTest(testDispatcher) {
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        val dayBefore = due.startOfDay().minusDays(1)
        val watchFlow = initializeGoogleTasksExisting(due = due, hideUntil = dayBefore)
        assertEquals(DAY_BEFORE_DUE, viewModel.state.value.startDay)
        assertFalse(viewModel.state.value.hasChanges)

        watchFlow.emit(viewModel.state.value.originalTask.copy(dueDate = 0))
        advanceUntilIdle()

        assertEquals(DAY_BEFORE_DUE, viewModel.state.value.startDay)
        assertFalse(viewModel.state.value.hasChanges)

        viewModel.saveCurrentTask()
        advanceUntilIdle()
        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    @Test
    fun mergeExternalDueMoveDoesNotDirtyGoogleTasksRelativeStart() = runTest(testDispatcher) {
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        val dayBefore = due.startOfDay().minusDays(1)
        val watchFlow = initializeGoogleTasksExisting(due = due, hideUntil = dayBefore)

        val newDue = due.plusDays(7)
        watchFlow.emit(viewModel.state.value.originalTask.copy(dueDate = newDue))
        advanceUntilIdle()

        assertEquals(DAY_BEFORE_DUE, viewModel.state.value.startDay)
        assertEquals(newDue.startOfDay().minusDays(1), viewModel.state.value.task.hideUntil)
        assertFalse(viewModel.state.value.hasChanges)

        viewModel.saveCurrentTask()
        advanceUntilIdle()
        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    @Test
    fun mergeStaysCleanAcrossRepeatedGoogleTasksDueEchoes() = runTest(testDispatcher) {
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        val dayBefore = due.startOfDay().minusDays(1)
        val watchFlow = initializeGoogleTasksExisting(due = due, hideUntil = dayBefore)

        val movedDue = due.plusDays(7)
        watchFlow.emit(viewModel.state.value.originalTask.copy(dueDate = movedDue))
        advanceUntilIdle()
        assertEquals(DAY_BEFORE_DUE, viewModel.state.value.startDay)
        assertFalse(viewModel.state.value.hasChanges)

        watchFlow.emit(viewModel.state.value.originalTask.copy(modificationDate = 12345L))
        advanceUntilIdle()

        assertEquals(DAY_BEFORE_DUE, viewModel.state.value.startDay)
        assertEquals(movedDue.startOfDay().minusDays(1), viewModel.state.value.task.hideUntil)
        assertFalse(viewModel.state.value.hasChanges)
    }

    @Test
    fun mergeKeepsDateOnlyDueTimeStartCleanAcrossGoogleTasksEcho() = runTest(testDispatcher) {
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        val watchFlow = initializeGoogleTasksExisting(due = due, hideUntil = due)
        assertEquals(DUE_TIME, viewModel.state.value.startDay)
        assertFalse(viewModel.state.value.hasChanges)

        watchFlow.emit(viewModel.state.value.originalTask.copy(modificationDate = 12345L))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(DUE_TIME, state.startDay)
        assertEquals(due, state.task.hideUntil)
        assertFalse(state.hasChanges)

        viewModel.saveCurrentTask()
        advanceUntilIdle()
        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    @Test
    fun mergeIgnoresTransitoryDataAfterTagSave() = runTest(testDispatcher) {
        val watchFlow = MutableSharedFlow<Task?>()
        whenever(taskDao.watch(42L)).thenReturn(watchFlow)
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "t"))
        whenever(caldavDao.getTask(42)).thenReturn(null)
        buildViewModel(taskId = 42)
        advanceUntilIdle()

        viewModel.setTags(listOf(workTag))
        viewModel.saveCurrentTask()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.hasChanges)

        watchFlow.emit(viewModel.state.value.originalTask.copy(transitoryData = null))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.hasChanges)
    }

    // endregion

    // region initialize edge cases

    @Test
    fun initializeMissingTaskClosesEditor() = runTest(testDispatcher) {
        whenever(taskDao.fetch(99)).thenReturn(null)
        whenever(caldavDao.getTask(99)).thenReturn(null)

        buildViewModel(taskId = 99)
        val closed = awaitClose()
        advanceUntilIdle()

        // A hard delete leaves no tombstone, so the row simply isn't there. Handing back a blank
        // task instead would look like a new one and carry a remoteId unrelated to the destination
        // this editor is locked on, creating an unrelated duplicate on the way out.
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.deleted)
        assertTrue(closed())
    }

    @Test
    fun missingTaskIsNeverWrittenBack() = runTest(testDispatcher) {
        whenever(taskDao.fetch(99)).thenReturn(null)
        whenever(caldavDao.getTask(99)).thenReturn(null)

        buildViewModel(taskId = 99)
        advanceUntilIdle()
        viewModel.setTitle("typed into a dead editor")
        viewModel.saveCurrentTask()
        advanceUntilIdle()

        verify(taskDao, never()).createNew(any())
        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    @Test
    fun initializeWithNoIdTreatedAsNew() = runTest(testDispatcher) {
        buildViewModel(taskId = Task.NO_ID)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isNew)
        verify(taskDao, never()).fetch(any<Long>())
    }

    @Test
    fun initializeExistingTaskResolvesItsCaldavList() = runTest(testDispatcher) {
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "t"))
        whenever(caldavDao.getTask(42)).thenReturn(CaldavTask(task = 42, calendar = "cal-1"))
        whenever(caldavDao.getCalendarByUuid("cal-1")).thenReturn(testCalendar)

        buildViewModel(taskId = 42)
        advanceUntilIdle()

        assertEquals(CaldavFilter(calendar = testCalendar, account = testAccount), viewModel.state.value.list)
    }

    @Test
    fun initializeWithNoWritableListHasNullList() = runTest(testDispatcher) {
        whenever(caldavDao.getCalendars()).thenReturn(
            listOf(
                CaldavCalendar(
                    account = "acct-1",
                    uuid = "cal-1",
                    name = "Read only",
                    access = CaldavCalendar.ACCESS_READ_ONLY,
                )
            )
        )

        buildViewModel()
        advanceUntilIdle()

        assertNull(viewModel.state.value.list)
    }

    @Test
    fun eachEditorWatchesOnlyItsOwnTask() = runTest(testDispatcher) {
        val watch42 = MutableSharedFlow<Task?>(extraBufferCapacity = 1)
        val watch99 = MutableSharedFlow<Task?>(extraBufferCapacity = 1)
        whenever(taskDao.watch(42L)).thenReturn(watch42)
        whenever(taskDao.watch(99L)).thenReturn(watch99)
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "Task42"))
        whenever(taskDao.fetch(99)).thenReturn(Task(id = 99, title = "Task99"))
        whenever(caldavDao.getTask(42)).thenReturn(null)
        whenever(caldavDao.getTask(99)).thenReturn(null)

        val editor42 = buildViewModel(taskId = 42)
        advanceUntilIdle()
        val editor99 = buildViewModel(taskId = 99)
        advanceUntilIdle()

        watch42.emit(Task(id = 42, title = "external edit"))
        advanceUntilIdle()

        // The emission belongs to task 42 and must not leak into the editor for task 99.
        assertEquals("external edit", editor42.state.value.task.title)
        assertEquals(99L, editor99.state.value.task.id)
        assertEquals("Task99", editor99.state.value.task.title)
    }

    @Test
    fun initializeShowsErrorOnLoadFailure() = runTest(testDispatcher) {
        whenever(taskDao.fetch(42)).thenThrow(RuntimeException("db error"))

        buildViewModel(taskId = 42)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(viewModel.loadError.value)
    }

    // endregion

    // region teardown save

    @Test
    fun clearingEditorSavesPendingChanges() = runTest(testDispatcher) {
        initializeExisting(title = "Original")

        viewModel.setTitle("Edited on the way out")
        viewModel.onCleared()
        advanceUntilIdle()

        verify(taskSaver).save(check { assertEquals("Edited on the way out", it.title) }, anyOrNull(), any())
    }

    @Test
    fun clearingEditorWithoutChangesSavesNothing() = runTest(testDispatcher) {
        initializeExisting()

        viewModel.onCleared()
        advanceUntilIdle()

        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    @Test
    fun clearingEditorDoesNotResurrectDeletedTask() = runTest(testDispatcher) {
        val watch = MutableSharedFlow<Task?>(extraBufferCapacity = 1)
        whenever(taskDao.watch(42L)).thenReturn(watch)
        initializeExisting(title = "Doomed")

        viewModel.setTitle("Edited before delete")
        watch.emit(Task(id = 42, title = "Doomed", deletionDate = currentTimeMillis()))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.deleted)

        viewModel.onCleared()
        advanceUntilIdle()

        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    @Test
    fun failedTeardownSaveIsReportedToPendingSaves() = runTest(testDispatcher) {
        initializeNewWithFailingSave()
        val failures = collectSaveFailures()

        viewModel.onCleared()
        advanceUntilIdle()

        assertEquals(1, failures())
    }

    /**
     * What desktop shutdown depends on: flushPending, then awaitIdle, and only then exit. The wait
     * has to be entered while the save is still running - collecting it afterwards asserts nothing,
     * because inFlight is a StateFlow that replays a zero it is already back to.
     */
    @Test
    fun awaitIdleWaitsForTheTeardownSave() = runTest(testDispatcher) {
        initializeExisting()
        val saveGate = CompletableDeferred<Unit>()
        whenever(taskSaver.save(any(), anyOrNull(), any())).doSuspendableAnswer {
            saveGate.await()
        }

        viewModel.setTitle("Edited on the way out")
        viewModel.onCleared()

        var idle = false
        launch { pendingSaves.awaitIdle(); idle = true }
        advanceUntilIdle()
        assertFalse("awaitIdle returned while a save was still in flight", idle)

        saveGate.complete(Unit)
        advanceUntilIdle()
        assertTrue(idle)
    }

    /** A save that fails still has to release the wait, or shutdown hangs until its timeout. */
    @Test
    fun awaitIdleReturnsAfterAFailedTeardownSave() = runTest(testDispatcher) {
        initializeNewWithFailingSave()

        viewModel.onCleared()

        var idle = false
        launch { pendingSaves.awaitIdle(); idle = true }
        advanceUntilIdle()
        assertTrue(idle)
        assertEquals(1, pendingSaves.saveFailures.value)
    }

    // endregion

    // region default hide-until seeding

    private suspend fun TestScope.initializeNewWithDefaultHideUntil(setting: Int) {
        whenever(appPreferences.datePickerPreferences())
            .thenReturn(DatePickerPreferences(defaultHideUntil = setting))
        buildViewModel()
        advanceUntilIdle()
    }

    @Test
    fun seedsDefaultHideUntilDueForNewTask() = runTest(testDispatcher) {
        initializeNewWithDefaultHideUntil(Task.HIDE_UNTIL_DUE)

        val state = viewModel.state.value
        assertEquals(DUE_DATE, state.startDay)
        assertEquals(NO_TIME, state.startTime)
        assertEquals(0L, state.task.hideUntil)
    }

    @Test
    fun seedsDefaultHideUntilDueTimeForNewTask() = runTest(testDispatcher) {
        initializeNewWithDefaultHideUntil(Task.HIDE_UNTIL_DUE_TIME)
        assertEquals(DUE_TIME, viewModel.state.value.startDay)
    }

    @Test
    fun seedsDefaultHideUntilDayBeforeForNewTask() = runTest(testDispatcher) {
        initializeNewWithDefaultHideUntil(Task.HIDE_UNTIL_DAY_BEFORE)
        assertEquals(DAY_BEFORE_DUE, viewModel.state.value.startDay)
    }

    @Test
    fun seedsDefaultHideUntilWeekBeforeForNewTask() = runTest(testDispatcher) {
        initializeNewWithDefaultHideUntil(Task.HIDE_UNTIL_WEEK_BEFORE)
        assertEquals(WEEK_BEFORE_DUE, viewModel.state.value.startDay)
    }

    @Test
    fun defaultHideUntilNoneLeavesNoStartForNewTask() = runTest(testDispatcher) {
        initializeNewWithDefaultHideUntil(Task.HIDE_UNTIL_NONE)
        assertEquals(NO_DAY, viewModel.state.value.startDay)
    }

    @Test
    fun doesNotSeedDefaultHideUntilForExistingTask() = runTest(testDispatcher) {
        whenever(appPreferences.datePickerPreferences())
            .thenReturn(DatePickerPreferences(defaultHideUntil = Task.HIDE_UNTIL_DUE))
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "t"))
        whenever(caldavDao.getTask(42)).thenReturn(null)

        buildViewModel(taskId = 42)
        advanceUntilIdle()

        assertEquals(NO_DAY, viewModel.state.value.startDay)
    }

    @Test
    fun doesNotSeedDefaultHideUntilForRequestedButMissingTask() = runTest(testDispatcher) {
        whenever(appPreferences.datePickerPreferences())
            .thenReturn(DatePickerPreferences(defaultHideUntil = Task.HIDE_UNTIL_DAY_BEFORE))
        whenever(taskDao.fetch(99)).thenReturn(null)
        whenever(caldavDao.getTask(99)).thenReturn(null)

        buildViewModel(taskId = 99)
        advanceUntilIdle()

        // Nothing to seed: a destination naming a row that is gone closes rather than turning into
        // a new task. Seeding for a genuinely new task is covered above.
        val state = viewModel.state.value
        assertTrue(state.deleted)
        assertEquals(NO_DAY, state.startDay)
    }

    @Test
    fun seededRelativeStartResolvesWhenDueSet() = runTest(testDispatcher) {
        initializeNewWithDefaultHideUntil(Task.HIDE_UNTIL_DAY_BEFORE)
        assertEquals(DAY_BEFORE_DUE, viewModel.state.value.startDay)

        val today = currentTimeMillis().startOfDay()
        viewModel.setDueDate(today.noon())

        assertEquals(DAY_BEFORE_DUE, viewModel.state.value.startDay)
        assertEquals(today.minusDays(1), viewModel.state.value.task.hideUntil)
    }

    // endregion

    // region initialize start-selection collapse

    private suspend fun TestScope.initializeExistingWith(dueDate: Long, hideUntil: Long, id: Long = 42) {
        whenever(taskDao.fetch(id)).thenReturn(Task(id = id, title = "t", dueDate = dueDate, hideUntil = hideUntil))
        whenever(caldavDao.getTask(id)).thenReturn(null)
        buildViewModel(taskId = id)
        advanceUntilIdle()
    }

    @Test
    fun initializeCollapsesDueDateStart() = runTest(testDispatcher) {
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        initializeExistingWith(dueDate = due, hideUntil = due.startOfDay())

        assertEquals(DUE_DATE, viewModel.state.value.startDay)
        assertEquals(NO_TIME, viewModel.state.value.startTime)
    }

    @Test
    fun initializeCollapsesDueTimeStart() = runTest(testDispatcher) {
        val today = currentTimeMillis().startOfDay()
        val due = today.withMillisOfDay(NINE_AM_WITH_TIME)
        initializeExistingWith(dueDate = due, hideUntil = due)

        assertEquals(DUE_TIME, viewModel.state.value.startDay)
        assertEquals(NO_TIME, viewModel.state.value.startTime)
    }

    @Test
    fun initializeIsNotDirtyWhenDateOnlyStartEqualsDueDate() = runTest(testDispatcher) {
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        initializeExistingWith(dueDate = due, hideUntil = due)

        assertEquals(DUE_TIME, viewModel.state.value.startDay)
        assertFalse(viewModel.state.value.hasChanges)

        viewModel.saveCurrentTask()
        advanceUntilIdle()
        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    @Test
    fun initializeCollapsesWeekBeforeStart() = runTest(testDispatcher) {
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        initializeExistingWith(dueDate = due, hideUntil = due.startOfDay().minusDays(7))

        assertEquals(WEEK_BEFORE_DUE, viewModel.state.value.startDay)
    }

    @Test
    fun initializeKeepsAbsoluteStart() = runTest(testDispatcher) {
        val today = currentTimeMillis().startOfDay()
        val due = today.noon()
        val absoluteStart = today.plusDays(3)
        initializeExistingWith(dueDate = due, hideUntil = absoluteStart)

        assertEquals(absoluteStart, viewModel.state.value.startDay)
        assertEquals(NO_TIME, viewModel.state.value.startTime)
    }

    // endregion

    // region setDueDate

    @Test
    fun setDueDateDateOnlyLandsAtNoon() = runTest(testDispatcher) {
        initializeNew()
        val today = currentTimeMillis().startOfDay()

        viewModel.setDueDate(today)

        val dueDate = viewModel.state.value.task.dueDate
        assertEquals(today.noon(), dueDate)
        assertFalse(Task.hasDueTime(dueDate))
    }

    @Test
    fun setDueDateTimedPreservesTime() = runTest(testDispatcher) {
        initializeNew()
        val today = currentTimeMillis().startOfDay()
        val timed = today.withMillisOfDay(NINE_AM_WITH_TIME)

        viewModel.setDueDate(timed)

        val dueDate = viewModel.state.value.task.dueDate
        assertEquals(timed, dueDate)
        assertTrue(Task.hasDueTime(dueDate))
    }

    @Test
    fun setDueDateZeroClearsDueDate() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setDueDate(currentTimeMillis().startOfDay())
        assertTrue(viewModel.state.value.task.dueDate > 0)

        viewModel.setDueDate(0)

        assertEquals(0L, viewModel.state.value.task.dueDate)
    }

    @Test
    fun setDueDateRetracksRelativeStart() = runTest(testDispatcher) {
        initializeNew()
        val today = currentTimeMillis().startOfDay()
        viewModel.setStartDate(DAY_BEFORE_DUE, NO_TIME)
        assertEquals(0L, viewModel.state.value.task.hideUntil)

        viewModel.setDueDate(today.noon())

        assertEquals(today.minusDays(1), viewModel.state.value.task.hideUntil)
    }

    @Test
    fun setDueDateLeavesAbsoluteStart() = runTest(testDispatcher) {
        initializeNew()
        val today = currentTimeMillis().startOfDay()
        val absoluteStart = today.plusDays(3)
        viewModel.setStartDate(absoluteStart, NO_TIME)
        assertEquals(absoluteStart, viewModel.state.value.task.hideUntil)

        viewModel.setDueDate(today.noon())

        assertEquals(absoluteStart, viewModel.state.value.task.hideUntil)
    }

    // endregion

    // region setStartDate

    @Test
    fun setStartDateDueDateResolvesToDueDay() = runTest(testDispatcher) {
        initializeNew()
        val today = currentTimeMillis().startOfDay()
        viewModel.setDueDate(today.noon())

        viewModel.setStartDate(DUE_DATE, NO_TIME)

        assertEquals(DUE_DATE, viewModel.state.value.startDay)
        assertEquals(today, viewModel.state.value.task.hideUntil)
    }

    @Test
    fun setStartDateNoneClearsStart() = runTest(testDispatcher) {
        initializeNew()
        val today = currentTimeMillis().startOfDay()
        viewModel.setStartDate(today.plusDays(2), NO_TIME)
        assertTrue(viewModel.state.value.task.hideUntil > 0)

        viewModel.setStartDate(NO_DAY, NO_TIME)

        assertEquals(NO_DAY, viewModel.state.value.startDay)
        assertEquals(0L, viewModel.state.value.task.hideUntil)
    }

    @Test
    fun setStartDateRelativeWithoutDueResolvesToZero() = runTest(testDispatcher) {
        initializeNew()

        viewModel.setStartDate(DUE_DATE, NO_TIME)

        assertEquals(DUE_DATE, viewModel.state.value.startDay)
        assertEquals(0L, viewModel.state.value.task.hideUntil)
    }

    @Test
    fun setStartDateMarksHasChanges() = runTest(testDispatcher) {
        initializeExisting(id = 42, title = "t")
        assertFalse(viewModel.state.value.hasChanges)

        viewModel.setStartDate(currentTimeMillis().startOfDay().plusDays(2), NO_TIME)

        assertTrue(viewModel.state.value.hasChanges)
    }

    @Test
    fun setStartDateAbsoluteWithTime() = runTest(testDispatcher) {
        initializeNew()
        val today = currentTimeMillis().startOfDay()
        val startTime = NINE_AM_WITH_TIME

        viewModel.setStartDate(today.plusDays(2), startTime)

        val state = viewModel.state.value
        assertEquals(today.plusDays(2), state.startDay)
        assertEquals(startTime, state.startTime)
        assertEquals(today.plusDays(2).withMillisOfDay(startTime), state.task.hideUntil)
    }

    @Test
    fun setStartDateKeepsAbsolutePickThatAlignsWithDueOffsetAcrossDueChange() = runTest(testDispatcher) {
        initializeNew()
        val today = currentTimeMillis().startOfDay()
        viewModel.setDueDate(today.plusDays(1).noon())
        val absoluteStart = today
        viewModel.setStartDate(absoluteStart, NO_TIME)
        assertEquals(absoluteStart, viewModel.state.value.startDay)
        assertEquals(absoluteStart, viewModel.state.value.task.hideUntil)

        viewModel.setDueDate(today.plusDays(8).noon())
        assertEquals(absoluteStart, viewModel.state.value.startDay)
        assertEquals(absoluteStart, viewModel.state.value.task.hideUntil)
    }

    // endregion

    // region picker input mode

    @Test
    fun setDatePickerInputModePersistsAndUpdatesState() = runTest(testDispatcher) {
        initializeNew()

        viewModel.setDatePickerInputMode(true)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.datePickerPreferences.datePickerInputMode)
        verify(appPreferences).setDatePickerInputMode(true)
    }

    @Test
    fun setTimePickerInputModePersistsAndUpdatesState() = runTest(testDispatcher) {
        initializeNew()

        viewModel.setTimePickerInputMode(true)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.datePickerPreferences.timePickerInputMode)
        verify(appPreferences).setTimePickerInputMode(true)
    }

    @Test
    fun setDatePickerInputModeRevertsWhenPersistFails() = runTest(testDispatcher) {
        initializeNew()
        whenever(appPreferences.setDatePickerInputMode(true)).thenThrow(RuntimeException("io error"))

        viewModel.setDatePickerInputMode(true)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.datePickerPreferences.datePickerInputMode)
    }

    // endregion

    // region list change

    private val otherFilter = CaldavFilter(
        calendar = CaldavCalendar(account = "acct-2", uuid = "cal-2", name = "Other"),
        account = CaldavAccount(uuid = "acct-2"),
    )

    @Test
    fun setListMarksHasChanges() = runTest(testDispatcher) {
        initializeExisting(id = 42, title = "t")
        assertFalse(viewModel.state.value.hasChanges)

        viewModel.setList(otherFilter)

        assertTrue(viewModel.state.value.hasChanges)
        assertEquals(otherFilter, viewModel.state.value.list)
    }

    @Test
    fun changingListOnExistingTaskMovesIt() = runTest(testDispatcher) {
        initializeExisting(id = 42, title = "t")

        viewModel.setList(otherFilter)
        viewModel.save()
        advanceUntilIdle()

        verify(taskMover).move(
            check<List<Long>> { assertEquals(listOf(42L), it) },
            check<CaldavFilter> { assertEquals(otherFilter, it) },
            any<Long>(),
        )
    }

    @Test
    fun newTaskIsNotMoved() = runTest(testDispatcher) {
        initializeNew()

        viewModel.setTitle("New")
        viewModel.save()
        advanceUntilIdle()

        verify(taskMover, never()).move(any<List<Long>>(), any<CaldavFilter>(), any<Long>())
    }

    // endregion

    // region save semantics

    @Test
    fun saveCurrentTaskDoesNotEmitClose() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Unsaved")
        val closed = awaitClose()

        viewModel.saveCurrentTask()
        advanceUntilIdle()

        assertFalse(closed())
        verify(taskDao).createNew(check { assertEquals("Unsaved", it.title) })
    }

    @Test
    fun saveWithNullListDoesNotSave() = runTest(testDispatcher) {
        whenever(caldavDao.getCalendars()).thenReturn(
            listOf(
                CaldavCalendar(
                    account = "acct-1",
                    uuid = "cal-1",
                    name = "Read only",
                    access = CaldavCalendar.ACCESS_READ_ONLY,
                )
            )
        )
        buildViewModel()
        advanceUntilIdle()
        assertNull(viewModel.state.value.list)

        viewModel.setTitle("Orphan")
        viewModel.save()
        advanceUntilIdle()

        verify(taskDao, never()).createNew(any())
        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    // endregion

    // region mark complete

    @Test
    fun markCompleteSavesEditBeforeCompleting() = runTest(testDispatcher) {
        initializeExisting(title = "Original")
        val closed = awaitClose()

        viewModel.setTitle("Edited")
        viewModel.markComplete()
        advanceUntilIdle()

        verify(taskSaver).save(check { assertEquals("Edited", it.title) }, anyOrNull(), any())
        verify(taskCompleter).setComplete(42L, true)
        assertTrue(closed())
    }

    @Test
    fun markCompleteCompletesRowCreatedForNewTask() = runTest(testDispatcher) {
        initializeNew()

        viewModel.setTitle("Created and completed")
        viewModel.markComplete()
        advanceUntilIdle()

        verify(taskDao).createNew(check { assertEquals("Created and completed", it.title) })
        verify(taskCompleter).setComplete(NEW_TASK_ID, true)
    }

    @Test
    fun markCompleteOnUntouchedNewTaskCreatesNothing() = runTest(testDispatcher) {
        initializeNew()
        val closed = awaitClose()

        viewModel.markComplete()
        advanceUntilIdle()

        verify(taskDao, never()).createNew(any())
        verify(taskCompleter, never()).setComplete(any<Long>(), any())
        assertTrue(closed())
    }

    @Test
    fun failedCompletionKeepsEditorOpen() = runTest(testDispatcher) {
        initializeExisting(title = "Original")
        whenever(taskCompleter.setComplete(42L, true)).thenThrow(RuntimeException("db error"))
        val closed = awaitClose()
        val failures = collectSaveFailures()

        viewModel.markComplete()
        advanceUntilIdle()

        assertFalse(closed())
        assertEquals(1, failures())
    }

    // endregion

    // region delete

    @Test
    fun deleteMarksTaskDeletedAndCloses() = runTest(testDispatcher) {
        initializeExisting(title = "Doomed")
        val closed = awaitClose()

        viewModel.delete()
        advanceUntilIdle()

        verify(taskDeleter).markDeleted(listOf(42L))
        assertTrue(viewModel.state.value.deleted)
        assertTrue(closed())
    }

    @Test
    fun deleteDiscardsPendingEditOnTeardown() = runTest(testDispatcher) {
        initializeExisting(title = "Doomed")

        viewModel.setTitle("Edited before delete")
        viewModel.delete()
        advanceUntilIdle()
        viewModel.onCleared()
        advanceUntilIdle()

        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    @Test
    fun deletingNewTaskNeverCreatesTheRow() = runTest(testDispatcher) {
        initializeNew()
        val closed = awaitClose()

        viewModel.setTitle("Typed then deleted")
        viewModel.delete()
        advanceUntilIdle()
        viewModel.onCleared()
        advanceUntilIdle()

        verify(taskDeleter, never()).markDeleted(any<List<Long>>())
        verify(taskDao, never()).createNew(any())
        assertTrue(closed())
    }

    @Test
    fun failedDeleteLeavesTaskEditable() = runTest(testDispatcher) {
        initializeExisting(title = "Doomed")
        whenever(taskDeleter.markDeleted(listOf(42L))).thenThrow(RuntimeException("db error"))
        val closed = awaitClose()
        val failures = collectSaveFailures()

        viewModel.setTitle("Edited")
        viewModel.delete()
        advanceUntilIdle()

        assertFalse(closed())
        assertFalse(viewModel.state.value.deleted)
        assertEquals(1, failures())

        viewModel.onCleared()
        advanceUntilIdle()

        verify(taskSaver).save(check { assertEquals("Edited", it.title) }, anyOrNull(), any())
    }

    // endregion

    // region discard changes

    @Test
    fun discardChangesRevertsEditAndCloses() = runTest(testDispatcher) {
        initializeExisting(title = "Original")
        val closed = awaitClose()

        viewModel.setTitle("Edited")
        viewModel.setPriority(Task.Priority.HIGH)
        viewModel.discardChanges()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Original", state.task.title)
        assertFalse(state.hasChanges)
        assertTrue(closed())
    }

    @Test
    fun discardedEditIsNotWrittenOnTeardown() = runTest(testDispatcher) {
        initializeExisting(title = "Original")

        viewModel.setTitle("Edited")
        viewModel.discardChanges()
        advanceUntilIdle()
        viewModel.onCleared()
        advanceUntilIdle()

        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    @Test
    fun discardChangesOnNewTaskCreatesNothing() = runTest(testDispatcher) {
        initializeNew()

        viewModel.setTitle("Typed then discarded")
        viewModel.discardChanges()
        advanceUntilIdle()
        viewModel.onCleared()
        advanceUntilIdle()

        verify(taskDao, never()).createNew(any())
    }

    // endregion

    // region alarms

    @Test
    fun loadsAlarmsForExistingTask() = runTest(testDispatcher) {
        val alarm = Alarm(id = 7, task = 42, time = 0, type = Alarm.TYPE_DATE_TIME)
        whenever(alarmDao.getAlarms(42L)).thenReturn(listOf(alarm))

        initializeExisting()

        assertEquals(persistentSetOf(alarm), viewModel.state.value.alarms)
    }

    @Test
    fun addAlarmIgnoresEquivalentAlarm() = runTest(testDispatcher) {
        initializeNew()

        viewModel.addAlarm(Alarm(time = ONE_HOUR, type = Alarm.TYPE_RANDOM))
        viewModel.addAlarm(Alarm(id = 99, time = ONE_HOUR, type = Alarm.TYPE_RANDOM))

        assertEquals(1, viewModel.state.value.alarms.size)
    }

    @Test
    fun removeAlarm() = runTest(testDispatcher) {
        val alarm = Alarm(time = ONE_HOUR, type = Alarm.TYPE_RANDOM)
        initializeNew()
        viewModel.addAlarm(alarm)

        viewModel.removeAlarm(alarm)

        assertTrue(viewModel.state.value.alarms.isEmpty())
    }

    @Test
    fun removeAlarmMatchesOnContent() = runTest(testDispatcher) {
        val stored = Alarm(id = 7, task = 42, time = ONE_HOUR, type = Alarm.TYPE_RANDOM)
        whenever(alarmDao.getAlarms(42L)).thenReturn(listOf(stored))
        initializeExisting()

        viewModel.removeAlarm(Alarm(time = ONE_HOUR, type = Alarm.TYPE_RANDOM))

        assertTrue(viewModel.state.value.alarms.isEmpty())
    }

    @Test
    fun replacingAnAlarmWithAnEqualOneIsNotAChange() = runTest(testDispatcher) {
        val stored = Alarm(id = 7, task = 42, time = ONE_HOUR, type = Alarm.TYPE_DATE_TIME)
        whenever(alarmDao.getAlarms(42L)).thenReturn(listOf(stored))
        initializeExisting()

        viewModel.removeAlarm(stored)
        viewModel.addAlarm(Alarm(time = ONE_HOUR, type = Alarm.TYPE_DATE_TIME))

        assertFalse(viewModel.state.value.hasChanges)
    }

    @Test
    fun savingSynchronizesAlarms() = runTest(testDispatcher) {
        val alarm = Alarm(time = ONE_HOUR, type = Alarm.TYPE_RANDOM)
        initializeExisting()

        viewModel.addAlarm(alarm)
        viewModel.save()
        advanceUntilIdle()

        verify(alarmService).synchronizeAlarms(eq(42L), eq(mutableSetOf(alarm)))
    }

    @Test
    fun addingAlarmAloneCountsAsAChange() = runTest(testDispatcher) {
        initializeExisting()

        viewModel.addAlarm(Alarm(time = ONE_HOUR, type = Alarm.TYPE_RANDOM))
        viewModel.save()
        advanceUntilIdle()

        verify(taskSaver).save(any(), any(), any())
    }

    @Test
    fun savingDropsRelativeAlarmsWithoutTheirDate() = runTest(testDispatcher) {
        initializeExisting()
        viewModel.addAlarm(Alarm.whenDue(0))
        viewModel.addAlarm(Alarm(time = ONE_HOUR, type = Alarm.TYPE_RANDOM))

        viewModel.save()
        advanceUntilIdle()

        verify(alarmService).synchronizeAlarms(
            eq(42L),
            eq(mutableSetOf(Alarm(time = ONE_HOUR, type = Alarm.TYPE_RANDOM))),
        )
    }

    @Test
    fun savingKeepsRelativeAlarmWhenItsDateIsSet() = runTest(testDispatcher) {
        initializeExisting()
        viewModel.setDueDate(currentTimeMillis().noon())
        viewModel.addAlarm(Alarm.whenDue(0))

        viewModel.save()
        advanceUntilIdle()

        verify(alarmService).synchronizeAlarms(eq(42L), eq(mutableSetOf(Alarm.whenDue(0))))
    }

    @Test
    fun alarmsAreNotSynchronizedWhenUnchanged() = runTest(testDispatcher) {
        val alarm = Alarm(id = 7, task = 42, time = ONE_HOUR, type = Alarm.TYPE_RANDOM)
        whenever(alarmDao.getAlarms(42L)).thenReturn(listOf(alarm))
        initializeExisting()

        viewModel.setTitle("Updated")
        viewModel.save()
        advanceUntilIdle()

        verify(alarmService, never()).synchronizeAlarms(any(), any())
    }

    @Test
    fun datelessRelativeAlarmIsNotAChangeOnItsOwn() = runTest(testDispatcher) {
        val alarm = Alarm(id = 7, task = 42, time = 0, type = Alarm.TYPE_REL_END)
        whenever(alarmDao.getAlarms(42L)).thenReturn(listOf(alarm))

        initializeExisting()

        assertFalse(viewModel.state.value.hasChanges)

        viewModel.save()
        advanceUntilIdle()

        verify(alarmService, never()).synchronizeAlarms(any(), any())
    }

    @Test
    fun savingDropsADatelessRelativeAlarmTheRowStillHas() = runTest(testDispatcher) {
        val alarm = Alarm(id = 7, task = 42, time = 0, type = Alarm.TYPE_REL_END)
        whenever(alarmDao.getAlarms(42L)).thenReturn(listOf(alarm))
        initializeExisting()

        viewModel.setTitle("Updated")
        viewModel.save()
        advanceUntilIdle()

        verify(alarmService).synchronizeAlarms(eq(42L), eq(mutableSetOf()))
    }

    @Test
    fun removingADatelessRelativeAlarmIsSaved() = runTest(testDispatcher) {
        val alarm = Alarm(id = 7, task = 42, time = 0, type = Alarm.TYPE_REL_END)
        whenever(alarmDao.getAlarms(42L)).thenReturn(listOf(alarm))
        initializeExisting()

        viewModel.removeAlarm(alarm)

        assertTrue(viewModel.state.value.hasChanges)

        viewModel.save()
        advanceUntilIdle()

        verify(alarmService).synchronizeAlarms(eq(42L), eq(mutableSetOf()))
    }

    @Test
    fun clearingTheDueDateRemovesItsAlarm() = runTest(testDispatcher) {
        val alarm = Alarm(id = 7, task = 42, time = 0, type = Alarm.TYPE_REL_END)
        val dueDate = currentTimeMillis().noon()
        whenever(taskDao.fetch(42L)).thenReturn(Task(id = 42, title = "Existing", dueDate = dueDate))
        whenever(caldavDao.getTask(42L)).thenReturn(null)
        whenever(alarmDao.getAlarms(42L)).thenReturn(listOf(alarm))
        buildViewModel(taskId = 42)
        advanceUntilIdle()

        viewModel.setDueDate(0)
        viewModel.save()
        advanceUntilIdle()

        verify(alarmService).synchronizeAlarms(eq(42L), eq(mutableSetOf()))
    }

    @Test
    fun alarmAddedBeforeItsDateDoesNotSurviveASave() = runTest(testDispatcher) {
        initializeExisting()

        viewModel.addAlarm(Alarm.whenDue(0))
        viewModel.setTitle("Updated")
        viewModel.save()
        advanceUntilIdle()

        verify(alarmService, never()).synchronizeAlarms(any(), any())
        assertTrue(viewModel.state.value.alarms.isEmpty())
        assertFalse(viewModel.state.value.hasChanges)

        viewModel.setDueDate(currentTimeMillis().noon())
        viewModel.addAlarm(Alarm.whenDue(0))
        viewModel.save()
        advanceUntilIdle()

        verify(alarmService).synchronizeAlarms(eq(42L), eq(mutableSetOf(Alarm.whenDue(0))))
    }

    @Test
    fun alarmAddedWhileSavingIsNotLost() = runTest(testDispatcher) {
        val added = Alarm(time = ONE_HOUR, type = Alarm.TYPE_RANDOM)
        initializeExisting()
        whenever(taskSaver.save(any(), anyOrNull(), any())).thenAnswer {
            viewModel.addAlarm(added)
            Unit
        }

        viewModel.setTitle("Updated")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(persistentSetOf(added), viewModel.state.value.alarms)
    }

    @Test
    fun externallyAddedAlarmShowsUpInTheEditor() = runTest(testDispatcher) {
        val existing = Alarm(id = 7, task = 42, time = ONE_HOUR, type = Alarm.TYPE_RANDOM)
        val external = Alarm(id = 8, task = 42, time = 0, type = Alarm.TYPE_DATE_TIME)
        val alarmFlow = MutableSharedFlow<List<Alarm>>(replay = 1)
        whenever(alarmDao.getAlarms(42L)).thenReturn(listOf(existing))
        whenever(alarmDao.watchAlarms(42L)).thenReturn(alarmFlow)
        initializeExisting()

        alarmFlow.emit(listOf(existing, external))
        advanceUntilIdle()

        assertEquals(persistentSetOf(existing, external), viewModel.state.value.alarms)
        assertFalse(viewModel.state.value.hasChanges)
    }

    @Test
    fun externalAlarmChangeIsMergedWithLocalEdits() = runTest(testDispatcher) {
        val existing = Alarm(id = 7, task = 42, time = ONE_HOUR, type = Alarm.TYPE_RANDOM)
        val external = Alarm(id = 8, task = 42, time = 0, type = Alarm.TYPE_DATE_TIME)
        val local = Alarm(time = 2 * ONE_HOUR, type = Alarm.TYPE_RANDOM)
        val alarmFlow = MutableSharedFlow<List<Alarm>>(replay = 1)
        whenever(alarmDao.getAlarms(42L)).thenReturn(listOf(existing))
        whenever(alarmDao.watchAlarms(42L)).thenReturn(alarmFlow)
        initializeExisting()
        viewModel.addAlarm(local)

        alarmFlow.emit(listOf(existing, external))
        advanceUntilIdle()

        assertEquals(persistentSetOf(existing, external, local), viewModel.state.value.alarms)
        assertTrue(viewModel.state.value.hasChanges)
    }

    @Test
    fun externallyAddedAlarmSurvivesTheNextSave() = runTest(testDispatcher) {
        val existing = Alarm(id = 7, task = 42, time = ONE_HOUR, type = Alarm.TYPE_RANDOM)
        val external = Alarm(id = 8, task = 42, time = 0, type = Alarm.TYPE_DATE_TIME)
        val local = Alarm(time = 2 * ONE_HOUR, type = Alarm.TYPE_RANDOM)
        val alarmFlow = MutableSharedFlow<List<Alarm>>(replay = 1)
        whenever(alarmDao.getAlarms(42L)).thenReturn(listOf(existing))
        whenever(alarmDao.watchAlarms(42L)).thenReturn(alarmFlow)
        initializeExisting()
        viewModel.addAlarm(local)

        alarmFlow.emit(listOf(existing, external))
        advanceUntilIdle()
        viewModel.save()
        advanceUntilIdle()

        verify(alarmService).synchronizeAlarms(
            eq(42L),
            eq(mutableSetOf(existing, external, local)),
        )
    }

    @Test
    fun locallyRemovedAlarmStaysRemovedThroughAMerge() = runTest(testDispatcher) {
        val existing = Alarm(id = 7, task = 42, time = ONE_HOUR, type = Alarm.TYPE_RANDOM)
        val external = Alarm(id = 8, task = 42, time = 0, type = Alarm.TYPE_DATE_TIME)
        val alarmFlow = MutableSharedFlow<List<Alarm>>(replay = 1)
        whenever(alarmDao.getAlarms(42L)).thenReturn(listOf(existing))
        whenever(alarmDao.watchAlarms(42L)).thenReturn(alarmFlow)
        initializeExisting()
        viewModel.removeAlarm(existing)

        alarmFlow.emit(listOf(existing, external))
        advanceUntilIdle()

        assertEquals(persistentSetOf(external), viewModel.state.value.alarms)
    }

    @Test
    fun mergeDoesNotDuplicateAnAlarmTheDatabaseHasCaughtUpWith() = runTest(testDispatcher) {
        val local = Alarm(time = ONE_HOUR, type = Alarm.TYPE_RANDOM)
        val alarmFlow = MutableSharedFlow<List<Alarm>>(replay = 1)
        whenever(alarmDao.watchAlarms(42L)).thenReturn(alarmFlow)
        initializeExisting()
        viewModel.addAlarm(local)

        alarmFlow.emit(listOf(local.copy(id = 9, task = 42)))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.alarms.size)
    }

    @Test
    fun settingTheDueDateAddsTheDefaultReminders() = runTest(testDispatcher) {
        whenever(appPreferences.defaultAlarms()).thenReturn(listOf(Alarm.whenDue(0)))
        whenever(appPreferences.isDefaultDueTimeEnabled()).thenReturn(true)
        initializeNew()

        assertTrue(viewModel.state.value.alarms.isEmpty())

        viewModel.setDueDate(currentTimeMillis().startOfDay())
        advanceUntilIdle()

        assertEquals(persistentSetOf(Alarm.whenDue(0)), viewModel.state.value.alarms)
    }

    @Test
    fun settingTheStartDateAddsTheDefaultReminders() = runTest(testDispatcher) {
        whenever(appPreferences.defaultAlarms()).thenReturn(listOf(Alarm.whenStarted(0)))
        whenever(appPreferences.isDefaultDueTimeEnabled()).thenReturn(true)
        initializeNew()

        viewModel.setStartDate(currentTimeMillis().startOfDay(), NO_TIME)
        advanceUntilIdle()

        assertEquals(persistentSetOf(Alarm.whenStarted(0)), viewModel.state.value.alarms)
    }

    @Test
    fun newTaskWithDefaultAlarmsIsNotAChangeOnItsOwn() = runTest(testDispatcher) {
        whenever(appPreferences.defaultRandomHours()).thenReturn(1)

        initializeNew()

        assertFalse(viewModel.state.value.hasChanges)

        viewModel.save()
        advanceUntilIdle()

        verify(taskDao, never()).createNew(any())
    }

    @Test
    fun newTaskWritesItsDefaultAlarms() = runTest(testDispatcher) {
        val default = Alarm(time = ONE_HOUR, type = Alarm.TYPE_RANDOM)
        whenever(appPreferences.defaultRandomHours()).thenReturn(1)
        initializeNew()

        viewModel.setTitle("New")
        viewModel.save()
        advanceUntilIdle()

        verify(alarmService).synchronizeAlarms(eq(NEW_TASK_ID), eq(mutableSetOf(default)))
    }

    @Test
    fun dateOnlyDueDateEarnsNoDefaultReminderWithoutADefaultDueTime() = runTest(testDispatcher) {
        whenever(appPreferences.defaultAlarms()).thenReturn(listOf(Alarm.whenDue(0)))
        whenever(appPreferences.isDefaultDueTimeEnabled()).thenReturn(false)
        initializeNew()

        viewModel.setDueDate(currentTimeMillis().startOfDay())
        advanceUntilIdle()

        assertTrue(viewModel.state.value.alarms.isEmpty())
    }

    // endregion
}
