package org.tasks.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.tasks.InMemoryDataStore
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.filters.CaldavFilter
import org.tasks.filters.MyTasksFilter
import org.tasks.filters.SearchFilter
import org.tasks.preferences.DataStoreQueryPreferences
import org.tasks.preferences.TasksPreferences

/**
 * Pins the search/filter contract in [TaskListViewModel.queryFilterFor] that the task
 * search bar (driven from the floating action search button in `App.kt`) relies on. The
 * navigation drawer has its own, separate search that only filters the drawer's
 * lists/tags and must not be conflated with this one:
 *
 *  - `searchQuery == null`   -> the active filter is used
 *  - `searchQuery` non-blank -> a SearchFilter over all tasks is used
 *  - `searchQuery` blank, non-null -> "My Tasks"
 *
 * The restore behaviour search depends on: running a search temporarily replaces the
 * active filter with a search; clearing the search (the UI maps a blank query to
 * `null`) must put the originally selected filter back.
 *
 * The search bar's text is a non-null String, `""`-initialised, so the UI (App.kt) must
 * map it onto this contract with `ifBlank { null }`. Passing the raw `""` through instead
 * leaves `searchQuery` blank-not-null, which permanently masks the active filter with My
 * Tasks and makes every list/tag/filter selection a no-op (the regression this contract
 * exists to prevent).
 */
class TaskListViewModelSearchRestoreTest {
    companion object {
        const val TEST_SEARCH_QUERY = "blue whale"
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun caldavFilter() = CaldavFilter(
        calendar = CaldavCalendar(uuid = "cal-1", account = "acct-1"),
        account = CaldavAccount(uuid = "acct-1"),
    )

    private fun viewModel(): TaskListViewModel {
        val taskDao: TaskDao = mock {
            on { fetchTasks(any()) } doAnswer { emptyList() }
        }
        return TaskListViewModel(
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
    }

    @Test
    fun clearingASearchRestoresTheActiveFilter() {
        val vm = viewModel()
        val caldav = caldavFilter()
        vm.setFilter(caldav)

        // 1. No search: the selected filter is what the list query uses.
        assertEquals(caldav, vm.queryFilterFor(vm.state.value))

        // 2. A search runs: the query switches to a search over all tasks,
        // scoped by the text, not by the selected calendar.
        vm.setSearchQuery(TEST_SEARCH_QUERY)
        val duringSearch = vm.queryFilterFor(vm.state.value)
        assertTrue("expected a SearchFilter during search, got $duringSearch",
            duringSearch is SearchFilter)
        assertEquals(TEST_SEARCH_QUERY, (duringSearch as SearchFilter).query)

        // 3. The search is cleared - the drawer wiring maps a blank query to null -
        // and the originally selected filter is back.
        vm.setSearchQuery(null)
        assertEquals(caldav, vm.queryFilterFor(vm.state.value))
    }

    @Test
    fun aRawBlankQueryIsMyTasksNotTheActiveFilter() {
        val vm = viewModel()
        val caldav = caldavFilter()
        vm.setFilter(caldav)

        // The raw "" that the drawer's String query emits when cleared. Blank-not-null
        // is deliberately "My Tasks" in the view model contract - which is exactly why
        // the UI must normalise a blank query to null (ifBlank { null }) rather than
        // passing the raw "" through.
        vm.setSearchQuery("")
        val effective = vm.queryFilterFor(vm.state.value)
        assertTrue("expected MyTasksFilter for a raw blank query, got $effective",
            effective is MyTasksFilter)
    }

    @Test
    fun aBlankQueryIsTrimmedBeforeUse() {
        val vm = viewModel()
        vm.setSearchQuery("   ")
        // trim() keeps it blank-not-null -> My Tasks, so the UI still must not send it.
        assertTrue(vm.queryFilterFor(vm.state.value) is MyTasksFilter)

        vm.setSearchQuery("  ${TEST_SEARCH_QUERY}  ")
        val search = vm.queryFilterFor(vm.state.value) as SearchFilter
        assertEquals(TEST_SEARCH_QUERY, search.query)
    }
}
