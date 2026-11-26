package org.tasks.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.tasks.InMemoryDataStore
import org.tasks.data.dao.DeletionDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.TaskSaver
import org.tasks.filters.CaldavFilter
import org.tasks.preferences.DataStoreQueryPreferences
import org.tasks.preferences.TasksPreferences
import org.tasks.service.TaskCompleter
import org.tasks.service.TaskDeleter
import org.tasks.tasklist.HeaderFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelSelectionTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun withViewModel(block: (TaskListViewModel) -> Unit) = runTest(testDispatcher) {
        val taskDao: TaskDao = mock()
        whenever(taskDao.fetchTasks(any())).thenReturn(emptyList())
        val viewModel = TaskListViewModel(
            taskDao = taskDao,
            taskDeleter = mock(),
            deletionDao = mock(),
            taskSaver = mock(),
            taskCompleter = mock(),
            tasksPreferences = TasksPreferences(InMemoryDataStore()),
            headerFormatter = mock(),
            queryPreferences = DataStoreQueryPreferences(TasksPreferences(InMemoryDataStore())),
            refreshFlow = emptyFlow(),
        )
        advanceUntilIdle()
        block(viewModel)
    }

    private fun caldavFilter() = CaldavFilter(
        calendar = CaldavCalendar(uuid = "cal-1", account = "acct-1"),
        account = CaldavAccount(uuid = "acct-1"),
    )

    @Test
    fun aFreshListHasNoSelection() = withViewModel { viewModel ->
        assertNull(viewModel.state.value.selectedTaskId)
    }

    @Test
    fun selectingATaskRecordsItInTheListState() = withViewModel { viewModel ->
        viewModel.selectTask(42)
        assertEquals(42L, viewModel.state.value.selectedTaskId)
    }

    @Test
    fun selectingAnotherTaskReplacesThePreviousSelection() = withViewModel { viewModel ->
        viewModel.selectTask(1)
        viewModel.selectTask(2)
        assertEquals(2L, viewModel.state.value.selectedTaskId)
    }

    @Test
    fun switchingListsClearsTheSelection() = withViewModel { viewModel ->
        viewModel.selectTask(42)
        assertEquals(42L, viewModel.state.value.selectedTaskId)

        viewModel.setFilter(caldavFilter())

        assertNull(viewModel.state.value.selectedTaskId)
    }
}
