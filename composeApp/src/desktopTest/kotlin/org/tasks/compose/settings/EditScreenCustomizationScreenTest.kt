package org.tasks.compose.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.tasks.InMemoryDataStore
import org.tasks.compose.edit.EditScreenSection
import org.tasks.preferences.TasksPreferences
import org.tasks.viewmodel.EditScreenCustomizationViewModel

/**
 * Smoke + behaviour tests for the "Customize edit screen" body: it composes
 * once loaded, shows one row per customisable section in canonical order,
 * and a switch click hides the section from the edit screen.
 *
 * Drag reordering is not exercised here (no pointer-drag precedent in this
 * repo's test suite); the move logic is unit-tested in
 * EditScreenCustomizationViewModelTest and verified live.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
class EditScreenCustomizationScreenTest {

    private val expectedTitles = listOf(
        "List",
        "Start date",
        "Due date",
        "Reminders",
        "Repeats",
        "Tags",
        "Priority",
        "Description",
        "Subtasks",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `renders one row per section in canonical order`() {
        val viewModel = EditScreenCustomizationViewModel(TasksPreferences(InMemoryDataStore()))
        runComposeUiTest {
            setContent {
                EditScreenCustomizationScreen(viewModel = viewModel)
            }
            // All nine section titles render (the loaded gate has opened).
            expectedTitles.forEach { onNodeWithText(it, substring = false).assertIsDisplayed() }
            // Rows are laid out top to bottom in canonical order.
            val ordered = expectedTitles
                .associate { it to onNodeWithText(it, substring = false).getBoundsInRoot().top }
                .entries
                .sortedBy { it.value }
                .map { it.key }
            assertEquals(expectedTitles, ordered)
        }
    }

    @Test
    fun `switching a section off hides it from the edit screen and keeps the row`() {
        val preferences = TasksPreferences(InMemoryDataStore())
        val viewModel = EditScreenCustomizationViewModel(preferences)
        runComposeUiTest {
            setContent {
                EditScreenCustomizationScreen(viewModel = viewModel)
            }
            // The switches appear in row order; Tags is the sixth row.
            val tagsIndex = expectedTitles.indexOf("Tags")
            onAllNodes(isToggleable())[tagsIndex].performClick()
        }
        assertFalse(viewModel.visibleSections.value.contains(EditScreenSection.TAGS))
        // The row itself stays in the list, merely switched off.
        assertEquals(9, viewModel.state.value.sections.size)
        assertEquals(
            false,
            viewModel.state.value.sections.single { it.section == EditScreenSection.TAGS }.enabled,
        )
    }
}
