package org.tasks.compose.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.InMemoryDataStore
import org.tasks.preferences.TasksPreferences
import org.tasks.viewmodel.TaskListPreferencesViewModel
import org.tasks.viewmodel.TaskListPreferencesViewModel.ChipOption
import org.tasks.viewmodel.TaskListPreferencesViewModel.SliderOption

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListPreferencesViewModelTest {

    private val preferences = TasksPreferences(InMemoryDataStore())

    @Before
    fun setUp() = Dispatchers.setMain(Dispatchers.Unconfined)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = TaskListPreferencesViewModel(preferences)

    private suspend fun TaskListPreferencesViewModel.loadedState() =
        withTimeout(5_000) { state.first { it.loaded } }

    @Test
    fun defaultsMatchTheNativeApp() = runBlocking {
        val vm = viewModel()
        val state = vm.loadedState()
        assertEquals(16, state.fontSize)
        assertEquals(16, state.rowSpacing)
        assertFalse(state.showFullTitle)
        assertTrue(state.showDescription)
        assertFalse(state.showFullDescription)
        assertFalse(state.showLinks)
        assertFalse(state.perListSort)
        assertEquals(TaskListPreferencesViewModel.CHIP_APPEARANCE_TEXT_AND_ICON, state.chipAppearance)
        assertTrue(state.subtaskChips)
        assertTrue(state.startDateChips)
        assertTrue(state.placeChips)
        assertTrue(state.listChips)
        assertTrue(state.tagChips)
    }

    @Test
    fun perListSortTogglePersists() = runBlocking {
        val vm = viewModel()
        vm.loadedState()
        vm.updatePerListSort(true)
        assertTrue(vm.state.value.perListSort)
        assertTrue(preferences.get(TasksPreferences.taskListPerListSort, false))
        vm.updatePerListSort(false)
        assertFalse(vm.state.value.perListSort)
        assertFalse(preferences.get(TasksPreferences.taskListPerListSort, false))
    }

    @Test
    fun hidingDescriptionsAlsoHidesFullDescription() = runBlocking {
        val vm = viewModel()
        vm.loadedState()
        vm.updateShowFullDescription(true)
        assertTrue(vm.state.value.showFullDescription)
        vm.updateShowDescription(false)
        assertFalse(vm.state.value.showDescription)
        assertFalse(vm.state.value.showFullDescription)
        assertFalse(preferences.get(TasksPreferences.taskListShowFullDescription, false))
    }

    @Test
    fun slidersAndChipFlagsPersist() = runBlocking {
        val vm = viewModel()
        vm.loadedState()
        vm.updateSlider(SliderOption.FONT_SIZE, 24)
        vm.updateSlider(SliderOption.ROW_SPACING, 8)
        vm.updateChipAppearance(TaskListPreferencesViewModel.CHIP_APPEARANCE_ICON_ONLY)
        vm.updateChipVisibility(ChipOption.TAGS, false)
        assertEquals(24, vm.state.value.fontSize)
        assertEquals(8, vm.state.value.rowSpacing)
        assertEquals(TaskListPreferencesViewModel.CHIP_APPEARANCE_ICON_ONLY, vm.state.value.chipAppearance)
        assertFalse(vm.state.value.tagChips)
        assertEquals(24, preferences.get(TasksPreferences.taskListFontSize, 0))
        assertEquals(8, preferences.get(TasksPreferences.taskListRowSpacing, 0))
        assertEquals(
            TaskListPreferencesViewModel.CHIP_APPEARANCE_ICON_ONLY,
            preferences.get(TasksPreferences.taskListChipAppearance, 0),
        )
        assertFalse(preferences.get(TasksPreferences.taskListTagChips, true))
    }
}
