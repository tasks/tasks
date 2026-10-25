package org.tasks.compose.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.InMemoryDataStore
import org.tasks.preferences.TasksPreferences
import org.tasks.viewmodel.TaskEditPreferencesViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class TaskEditPreferencesViewModelTest {

    private val preferences = TasksPreferences(InMemoryDataStore())

    @Before
    fun setUp() = Dispatchers.setMain(Dispatchers.Unconfined)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = TaskEditPreferencesViewModel(preferences)

    private suspend fun TaskEditPreferencesViewModel.loadedState() =
        withTimeout(5_000) { state.first { it.loaded } }

    @Test
    fun defaultMatchesNative() = runBlocking {
        val state = viewModel().loadedState()
        // Native default is false; the desktop one-shot seed (desktopMain main.kt)
        // is what makes the de-facto desktop default true.
        assertFalse(state.backButtonSaves)
    }

    @Test
    fun backButtonSavesTogglePersists() = runBlocking {
        val vm = viewModel()
        vm.loadedState()
        vm.updateBackButtonSaves(true)
        assertTrue(vm.state.value.backButtonSaves)
        assertTrue(preferences.get(TasksPreferences.taskEditBackButtonSaves, false))
        vm.updateBackButtonSaves(false)
        assertFalse(vm.state.value.backButtonSaves)
        assertFalse(preferences.get(TasksPreferences.taskEditBackButtonSaves, true))
    }

    @Test
    fun loadsSeededValue() = runBlocking {
        // Simulates the desktop first-launch seed having already run.
        preferences.set(TasksPreferences.taskEditBackButtonSaves, true)
        val state = viewModel().loadedState()
        assertTrue(state.backButtonSaves)
    }
}
