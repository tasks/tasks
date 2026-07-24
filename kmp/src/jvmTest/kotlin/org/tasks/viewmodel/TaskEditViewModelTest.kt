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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.tasks.data.TaskMover
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.SYNC_TAGS
import org.tasks.data.entity.TagData
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.DatePickerPreferences
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
import org.tasks.time.minusDays
import org.tasks.time.noon
import org.tasks.time.plusDays
import org.tasks.time.startOfDay
import org.tasks.time.withMillisOfDay

@OptIn(ExperimentalCoroutinesApi::class)
class TaskEditViewModelTest {

    private val NINE_AM_WITH_TIME = 9 * 60 * 60 * 1000 + 1000

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
    private val appPreferences: AppPreferences = mock()

    private lateinit var viewModel: TaskEditViewModel

    private val testCalendar = CaldavCalendar(account = "acct-1", uuid = "cal-1", name = "Test")
    private val testAccount = CaldavAccount(uuid = "acct-1")

    @Before
    fun setUp() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        whenever(caldavDao.getCalendars()).thenReturn(listOf(testCalendar))
        whenever(caldavDao.getAccountByUuid("acct-1")).thenReturn(testAccount)
        whenever(taskDao.watch(any())).thenReturn(MutableSharedFlow())
        whenever(tagDataDao.getTagDataForTask(any())).thenReturn(emptyList())
        whenever(appPreferences.datePickerPreferences()).thenReturn(DatePickerPreferences())
        viewModel = TaskEditViewModel(
            taskDao, taskSaver, caldavDao, taskMover, tagDao, tagDataDao, appPreferences,
            externalScope = CoroutineScope(testDispatcher),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region helpers

    private fun TestScope.initializeNew() {
        viewModel.initialize(null)
        advanceUntilIdle()
    }

    private suspend fun TestScope.initializeExisting(
        id: Long = 42,
        title: String = "Existing",
    ) {
        whenever(taskDao.fetch(id)).thenReturn(Task(id = id, title = title))
        whenever(caldavDao.getTask(id)).thenReturn(null)
        viewModel.initialize(id)
        advanceUntilIdle()
    }

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

    private fun TestScope.initializeNewWith(filter: Filter) {
        viewModel.initialize(null, filter)
        advanceUntilIdle()
    }

    @Test
    fun newTaskFromTagFilterPreFillsTag() = runTest(testDispatcher) {
        initializeNewWith(TagFilter(workTag))

        val state = viewModel.state.value
        assertEquals(listOf(workTag), state.tags)
        assertFalse(state.hasChanges)
    }

    @Test
    fun newTaskFromCaldavFilterHasNoTags() = runTest(testDispatcher) {
        initializeNewWith(CaldavFilter(calendar = testCalendar, account = testAccount))

        assertTrue(viewModel.state.value.tags.isEmpty())
    }

    @Test
    fun saveAppliesPreFilledTag() = runTest(testDispatcher) {
        initializeNewWith(TagFilter(workTag))

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
        initializeNewWith(TagFilter(workTag))
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
        viewModel.initialize(42)
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
    fun saveNewTaskWithPriorityOnly() = runTest(testDispatcher) {
        initializeNew()

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
    fun saveNewTaskWithDescriptionOnly() = runTest(testDispatcher) {
        initializeNew()

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
        val closed = awaitClose()
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertTrue(closed())
        verify(taskSaver, never()).save(any(), anyOrNull(), any())
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
    fun saveFailureSetsSaveError() = runTest(testDispatcher) {
        initializeNewWithFailingSave()

        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.saveError.value)
    }

    @Test
    fun clearSaveError() = runTest(testDispatcher) {
        initializeNewWithFailingSave()

        viewModel.save()
        advanceUntilIdle()
        assertTrue(viewModel.saveError.value)

        viewModel.clearSaveError()

        assertFalse(viewModel.saveError.value)
    }

    @Test
    fun saveErrorNotSetOnSuccess() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Will succeed")

        viewModel.save()
        advanceUntilIdle()

        assertFalse(viewModel.saveError.value)
    }

    // endregion

    // region switch task (saveCurrentTask + initialize)

    @Test
    fun switchSavesDirtyEdits() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Unsaved work")

        viewModel.saveCurrentTask()
        viewModel.initialize(null)
        advanceUntilIdle()

        verify(taskDao).createNew(check { assertEquals("Unsaved work", it.title) })
        verify(taskSaver).save(check { assertEquals("Unsaved work", it.title) }, anyOrNull(), any())
    }

    @Test
    fun switchSkippedWithoutChanges() = runTest(testDispatcher) {
        initializeNew()

        viewModel.saveCurrentTask()
        viewModel.initialize(null)
        advanceUntilIdle()

        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    @Test
    fun switchSavesDescriptionOnly() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setDescription("Some notes")

        viewModel.saveCurrentTask()
        viewModel.initialize(null)
        advanceUntilIdle()

        verify(taskDao).createNew(check { assertEquals("Some notes", it.notes) })
    }

    @Test
    fun switchFailureSetsSaveError() = runTest(testDispatcher) {
        initializeNewWithFailingSave()

        viewModel.saveCurrentTask()
        viewModel.initialize(null)
        advanceUntilIdle()

        assertTrue(viewModel.saveError.value)
    }

    @Test
    fun switchLoadsNewTaskAfterFailure() = runTest(testDispatcher) {
        initializeNewWithFailingSave()

        viewModel.saveCurrentTask()
        viewModel.initialize(null)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isNew)
        assertNull(state.task.title)
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
        viewModel.initialize(42)
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
        viewModel.initialize(42)
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
        viewModel.initialize(42)
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
        viewModel.initialize(42)
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
        viewModel.initialize(42)
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
        viewModel.initialize(42)
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
        viewModel.initialize(42)
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
        viewModel.initialize(42)
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
        viewModel.initialize(42)
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
        viewModel.initialize(42)
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
        viewModel.initialize(42)
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
    fun initializeMissingTaskFallsBackToBlank() = runTest(testDispatcher) {
        whenever(taskDao.fetch(99)).thenReturn(null)
        whenever(caldavDao.getTask(99)).thenReturn(null)

        viewModel.initialize(99)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isNew)
        assertNull(state.task.title)
        assertEquals(CaldavFilter(calendar = testCalendar, account = testAccount), state.list)
    }

    @Test
    fun initializeWithNoIdTreatedAsNew() = runTest(testDispatcher) {
        viewModel.initialize(Task.NO_ID)
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

        viewModel.initialize(42)
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

        viewModel.initialize(null)
        advanceUntilIdle()

        assertNull(viewModel.state.value.list)
    }

    @Test
    fun reinitializeStopsWatchingPreviousTask() = runTest(testDispatcher) {
        val watch42 = MutableSharedFlow<Task?>(extraBufferCapacity = 1)
        val watch99 = MutableSharedFlow<Task?>(extraBufferCapacity = 1)
        whenever(taskDao.watch(42L)).thenReturn(watch42)
        whenever(taskDao.watch(99L)).thenReturn(watch99)
        whenever(taskDao.fetch(42)).thenReturn(Task(id = 42, title = "Task42"))
        whenever(taskDao.fetch(99)).thenReturn(Task(id = 99, title = "Task99"))
        whenever(caldavDao.getTask(42)).thenReturn(null)
        whenever(caldavDao.getTask(99)).thenReturn(null)

        viewModel.initialize(42)
        advanceUntilIdle()
        viewModel.initialize(99)
        advanceUntilIdle()

        watch42.emit(Task(id = 42, title = "external edit"))
        advanceUntilIdle()

        assertEquals(99L, viewModel.state.value.task.id)
        assertEquals("Task99", viewModel.state.value.task.title)
    }

    @Test
    fun initializeShowsErrorOnLoadFailure() = runTest(testDispatcher) {
        whenever(taskDao.fetch(42)).thenThrow(RuntimeException("db error"))

        viewModel.initialize(42)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(viewModel.loadError.value)
    }

    // endregion

    // region default hide-until seeding

    private suspend fun TestScope.initializeNewWithDefaultHideUntil(setting: Int) {
        whenever(appPreferences.datePickerPreferences())
            .thenReturn(DatePickerPreferences(defaultHideUntil = setting))
        viewModel.initialize(null)
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

        viewModel.initialize(42)
        advanceUntilIdle()

        assertEquals(NO_DAY, viewModel.state.value.startDay)
    }

    @Test
    fun seedsDefaultHideUntilForRequestedButMissingTask() = runTest(testDispatcher) {
        whenever(appPreferences.datePickerPreferences())
            .thenReturn(DatePickerPreferences(defaultHideUntil = Task.HIDE_UNTIL_DAY_BEFORE))
        whenever(taskDao.fetch(99)).thenReturn(null)
        whenever(caldavDao.getTask(99)).thenReturn(null)

        viewModel.initialize(99)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isNew)
        assertEquals(DAY_BEFORE_DUE, state.startDay)
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
        viewModel.initialize(id)
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
        viewModel.initialize(null)
        advanceUntilIdle()
        assertNull(viewModel.state.value.list)

        viewModel.setTitle("Orphan")
        viewModel.save()
        advanceUntilIdle()

        verify(taskDao, never()).createNew(any())
        verify(taskSaver, never()).save(any(), anyOrNull(), any())
    }

    // endregion
}
