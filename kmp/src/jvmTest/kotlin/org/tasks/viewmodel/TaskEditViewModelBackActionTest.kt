package org.tasks.viewmodel

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.preferences.TasksPreferences

/**
 * resolveBackAction() matrix: dirty x "back button saves task" pref.
 * Pref default is false (native parity); the desktop seeds true on first
 * launch, so the "pref on" rows model the de-facto desktop default.
 */
class TaskEditViewModelBackActionTest : TaskEditViewModelFixture() {

    @Test
    fun cleanTaskBackDiscardsWhenPrefOff() = runTest(testDispatcher) {
        initializeExisting()
        assertEquals(TaskEditViewModel.BackAction.Discard, viewModel.resolveBackAction())
    }

    @Test
    fun cleanTaskBackSavesWhenPrefOn() = runTest(testDispatcher) {
        tasksPreferences.set(TasksPreferences.taskEditBackButtonSaves, true)
        initializeExisting()
        assertEquals(TaskEditViewModel.BackAction.Save, viewModel.resolveBackAction())
    }

    @Test
    fun dirtyTaskBackPromptsWhenPrefOff() = runTest(testDispatcher) {
        initializeExisting()
        viewModel.setTitle("Changed")
        advanceUntilIdle()
        assertEquals(TaskEditViewModel.BackAction.PromptDiscard, viewModel.resolveBackAction())
    }

    @Test
    fun dirtyTaskBackSavesWhenPrefOn() = runTest(testDispatcher) {
        tasksPreferences.set(TasksPreferences.taskEditBackButtonSaves, true)
        initializeExisting()
        viewModel.setTitle("Changed")
        advanceUntilIdle()
        assertEquals(TaskEditViewModel.BackAction.Save, viewModel.resolveBackAction())
    }

    @Test
    fun prefFlippedLiveChangesTheBackAction() = runTest(testDispatcher) {
        // Pref starts off; the editor is open when the settings pane flips it.
        initializeExisting()
        viewModel.setTitle("Changed")
        advanceUntilIdle()
        assertEquals(TaskEditViewModel.BackAction.PromptDiscard, viewModel.resolveBackAction())

        tasksPreferences.set(TasksPreferences.taskEditBackButtonSaves, true)
        advanceUntilIdle()
        assertEquals(TaskEditViewModel.BackAction.Save, viewModel.resolveBackAction())
    }
}
