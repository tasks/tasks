package org.tasks.viewmodel

import com.todoroo.astrid.core.SortHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.InMemoryDataStore
import org.tasks.analytics.Reporting
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.preferences.DataStoreQueryPreferences
import org.tasks.preferences.TasksPreferences

class SortSettingsViewModelTest {

    private val preferences = DataStoreQueryPreferences(TasksPreferences(InMemoryDataStore()))

    private val viewModel = SortSettingsViewModel(
        preferences = preferences,
        reporting = mock<Reporting>(),
        refreshBroadcaster = mock<RefreshBroadcaster>(),
    )

    @Test
    fun enablingManualSortTurnsItOnAndResetsGrouping() {
        viewModel.setGroupMode(SortHelper.SORT_DUE)
        assertEquals(SortHelper.SORT_DUE, preferences.groupMode)

        viewModel.setManual(true)

        assertTrue(preferences.isManualSort)
        assertTrue(viewModel.state.value.manualSort)
        assertEquals(SortHelper.GROUP_NONE, preferences.groupMode)
        assertEquals(SortHelper.GROUP_NONE, viewModel.state.value.groupMode)
    }

    @Test
    fun disablingManualSortTurnsItOff() {
        viewModel.setManual(true)
        assertTrue(preferences.isManualSort)

        viewModel.setManual(false)

        assertFalse(preferences.isManualSort)
        assertFalse(viewModel.state.value.manualSort)
    }

    @Test
    fun selectingASortModeClearsManualSort() {
        viewModel.setManual(true)

        viewModel.setSortMode(SortHelper.SORT_DUE)

        assertFalse(preferences.isManualSort)
        assertFalse(viewModel.state.value.manualSort)
        assertEquals(SortHelper.SORT_DUE, viewModel.state.value.sortMode)
    }
}
