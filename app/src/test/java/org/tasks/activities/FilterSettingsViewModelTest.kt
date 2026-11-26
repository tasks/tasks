package org.tasks.activities

import androidx.lifecycle.SavedStateHandle
import com.todoroo.astrid.api.CustomFilterCriterion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyNoInteractions
import org.tasks.MainDispatcherRule
import org.tasks.activities.FilterSettingsActivity.Companion.TOKEN_FILTER
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Filter
import org.tasks.filters.CustomFilter
import org.tasks.filters.FilterCriteriaProvider
import org.tasks.filters.key
import org.tasks.preferences.TasksPreferences

class FilterSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val criterion: CustomFilterCriterion = mock()

    private val filterCriteriaProvider = mock<FilterCriteriaProvider> {
        onBlocking { fromString(any()) } doReturn emptyList()
        on { startingUniverse } doReturn criterion
    }
    private val filterDao: FilterDao = mock()
    private val taskDao: TaskDao = mock {
        // init's setCriteria path calls count() eagerly under the Unconfined dispatcher.
        onBlocking { count(any()) } doReturn 0
    }
    private val tasksPreferences: TasksPreferences = mock()
    private val refreshBroadcaster: RefreshBroadcaster = mock()

    private val filter = Filter(
        id = 7L,
        title = "Work",
        criterion = "universe\n|||1",
    )
    private val customFilter = CustomFilter(filter)

    private fun viewModel(
        savedStateHandle: SavedStateHandle =
            SavedStateHandle(mapOf(TOKEN_FILTER to customFilter)),
    ) = FilterSettingsViewModel(
        savedStateHandle,
        filterCriteriaProvider,
        filterDao,
        taskDao,
        tasksPreferences,
        refreshBroadcaster,
    )

    @Test
    fun deleteBroadcastsRefreshAfterRemovingFilter() = runTest {
        val events = mutableListOf<String>()
        refreshBroadcaster.stub {
            on { broadcastRefresh() } doAnswer {
                events.add("broadcast")
                Unit
            }
        }

        var completed = false
        val sut = viewModel()
        sut.delete {
            completed = true
            events.add("completed")
        }

        inOrder(filterDao, tasksPreferences, refreshBroadcaster).run {
            verify(filterDao).delete(filter.id)
            verify(tasksPreferences).removeByPrefix("filter_sort_${customFilter.key()}_")
            verify(refreshBroadcaster).broadcastRefresh()
        }
        assertEquals(listOf("broadcast", "completed"), events)
        assertTrue(completed)
    }

    @Test
    fun deleteWithoutExistingFilterCompletesWithoutInteractions() = runTest {
        var completed = false
        val sut = viewModel(SavedStateHandle(emptyMap()))
        sut.delete { completed = true }

        verifyNoInteractions(filterDao, tasksPreferences, refreshBroadcaster)
        assertTrue(completed)
    }
}
