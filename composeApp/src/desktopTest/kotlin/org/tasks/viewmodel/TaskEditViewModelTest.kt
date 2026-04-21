package org.tasks.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
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
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter

@OptIn(ExperimentalCoroutinesApi::class)
class TaskEditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val taskDao: TaskDao = mock()
    private val taskSaver: TaskSaver = mock()
    private val caldavDao: CaldavDao = mock()

    private lateinit var viewModel: TaskEditViewModel

    private val testCalendar = CaldavCalendar(account = "acct-1", uuid = "cal-1", name = "Test")
    private val testAccount = CaldavAccount(uuid = "acct-1")

    @Before
    fun setUp() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        whenever(caldavDao.getCalendars()).thenReturn(listOf(testCalendar))
        whenever(caldavDao.getAccountByUuid("acct-1")).thenReturn(testAccount)
        viewModel = TaskEditViewModel(taskDao, taskSaver, caldavDao)
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

    // region save

    @Test
    fun saveCreatesNewTask() = runTest(testDispatcher) {
        initializeNew()

        viewModel.setTitle("New Task")
        viewModel.save()
        advanceUntilIdle()

        verify(taskDao).createNew(check { assertEquals("New Task", it.title) })
        verify(caldavDao).insert(task = any(), caldavTask = any(), addToTop = any())
        verify(taskSaver).save(check { assertEquals("New Task", it.title) }, anyOrNull())
    }

    @Test
    fun saveUpdatesExistingTask() = runTest(testDispatcher) {
        initializeExisting()

        viewModel.setTitle("Updated")
        viewModel.save()
        advanceUntilIdle()

        verify(taskDao, never()).createNew(any())
        verify(taskSaver).save(check { assertEquals("Updated", it.title) }, any())
    }

    @Test
    fun saveSkippedWithoutChanges() = runTest(testDispatcher) {
        initializeNew()
        val closed = awaitClose()

        viewModel.save()
        advanceUntilIdle()

        assertTrue(closed())
        verify(taskSaver, never()).save(any(), anyOrNull())
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
        verify(taskSaver, never()).save(any(), anyOrNull())
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

    // region switch task (auto-save on initialize)

    @Test
    fun switchSavesDirtyEdits() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setTitle("Unsaved work")

        viewModel.initialize(null)
        advanceUntilIdle()

        verify(taskDao).createNew(check { assertEquals("Unsaved work", it.title) })
        verify(taskSaver).save(check { assertEquals("Unsaved work", it.title) }, anyOrNull())
    }

    @Test
    fun switchSkippedWithoutChanges() = runTest(testDispatcher) {
        initializeNew()

        viewModel.initialize(null)
        advanceUntilIdle()

        verify(taskSaver, never()).save(any(), anyOrNull())
    }

    @Test
    fun switchSavesDescriptionOnly() = runTest(testDispatcher) {
        initializeNew()
        viewModel.setDescription("Some notes")

        viewModel.initialize(null)
        advanceUntilIdle()

        verify(taskDao).createNew(check { assertEquals("Some notes", it.notes) })
    }

    @Test
    fun switchFailureSetsSaveError() = runTest(testDispatcher) {
        initializeNewWithFailingSave()

        viewModel.initialize(null)
        advanceUntilIdle()

        assertTrue(viewModel.saveError.value)
    }

    @Test
    fun switchLoadsNewTaskAfterFailure() = runTest(testDispatcher) {
        initializeNewWithFailingSave()

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

        initializeExisting(id = 99, title = "Other Task")

        verify(taskSaver).save(
            check { assertEquals("Modified", it.title) },
            check { assertEquals("Original", it.title) },
        )
        assertEquals("Other Task", viewModel.state.value.task.title)
    }

    // endregion
}
