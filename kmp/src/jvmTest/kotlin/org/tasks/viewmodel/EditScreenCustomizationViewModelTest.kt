package org.tasks.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.InMemoryDataStore
import org.tasks.compose.edit.EditScreenSection
import org.tasks.compose.edit.EditScreenSections
import org.tasks.preferences.TasksPreferences

/**
 * State and persistence for the "Customize edit screen" view model. The
 * prefs are an in-memory store so each test sees a fresh install unless it
 * seeds the key itself.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditScreenCustomizationViewModelTest {

    private val preferences = TasksPreferences(InMemoryDataStore())

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun storedRaw(): String = runBlocking {
        preferences.get(TasksPreferences.editScreenSections, "")
    }

    @Test
    fun `first load is canonical order with everything enabled and nothing stored`() {
        val vm = EditScreenCustomizationViewModel(preferences)
        val state = vm.state.value
        assertTrue(state.loaded)
        assertEquals(EditScreenSections.CANONICAL, state.sections.map { it.section })
        assertTrue(state.sections.all { it.enabled })
        // An untouched install never writes the key.
        assertEquals("", storedRaw())
        assertFalse(vm.isCustomized())
    }

    @Test
    fun `onMove reorders sections and persists the new order`() {
        val vm = EditScreenCustomizationViewModel(preferences)
        vm.onMove(EditScreenSection.TAGS.name, EditScreenSection.LIST.name)
        val order = vm.state.value.sections.map { it.section }
        assertEquals(EditScreenSection.TAGS, order.first())
        assertEquals(EditScreenSection.LIST, order[1])
        assertTrue(vm.isCustomized())
        val expectedPrefix = """[{"id":"TAGS"}"""
        assertTrue(storedRaw().startsWith(expectedPrefix))
    }

    @Test
    fun `onMove with an unknown id is a no-op`() {
        val vm = EditScreenCustomizationViewModel(preferences)
        val before = vm.state.value.sections
        vm.onMove("NOPE", EditScreenSection.LIST.name)
        vm.onMove(EditScreenSection.LIST.name, "NOPE")
        assertEquals(before, vm.state.value.sections)
        assertEquals("", storedRaw())
    }

    @Test
    fun `setEnabled hides the section from visibleSections but keeps the row`() {
        val vm = EditScreenCustomizationViewModel(preferences)
        vm.setEnabled(EditScreenSection.DESCRIPTION.name, enabled = false)
        val state = vm.state.value
        assertEquals(9, state.sections.size)
        assertEquals(false, state.sections.single { it.section == EditScreenSection.DESCRIPTION }.enabled)
        assertFalse(vm.visibleSections.value.contains(EditScreenSection.DESCRIPTION))
        assertTrue(vm.isCustomized())
        assertTrue(storedRaw().contains("""{"id":"DESCRIPTION","enabled":false}"""))
    }

    @Test
    fun `a new view model over the same prefs restores the stored arrangement`() {
        val vm = EditScreenCustomizationViewModel(preferences)
        vm.onMove(EditScreenSection.SUBTASKS.name, EditScreenSection.LIST.name)
        vm.setEnabled(EditScreenSection.PRIORITY.name, enabled = false)
        val storedOrder = vm.state.value.sections.map { it.section }
        val storedEnabled = vm.state.value.sections.associate { it.section to it.enabled }

        val revived = EditScreenCustomizationViewModel(preferences)
        assertEquals(storedOrder, revived.state.value.sections.map { it.section })
        assertEquals(storedEnabled, revived.state.value.sections.associate { it.section to it.enabled })
        assertFalse(revived.visibleSections.value.contains(EditScreenSection.PRIORITY))
        assertTrue(revived.isCustomized())
    }

    @Test
    fun `a stale stored value with an unknown id survives construction`() {
        runBlocking {
            preferences.set(
                TasksPreferences.editScreenSections,
                """[{"id":"REMOVED_SECTION","enabled":true},{"id":"TAGS","enabled":false}]""",
            )
        }
        val vm = EditScreenCustomizationViewModel(preferences)
        val sections = vm.state.value.sections
        assertEquals(9, sections.size)
        assertTrue(sections.none { it.section.name == "REMOVED_SECTION" })
        assertEquals(EditScreenSection.TAGS, sections.first().section)
        assertEquals(false, sections.first().enabled)
        // The newest section is appended, enabled.
        assertEquals(EditScreenSection.SUBTASKS, sections.last().section)
        assertTrue(sections.last().enabled)
    }

    @Test
    fun `reset restores the canonical default`() {
        val vm = EditScreenCustomizationViewModel(preferences)
        vm.onMove(EditScreenSection.SUBTASKS.name, EditScreenSection.LIST.name)
        vm.setEnabled(EditScreenSection.TAGS.name, enabled = false)
        vm.reset()
        val state = vm.state.value
        assertEquals(EditScreenSections.CANONICAL, state.sections.map { it.section })
        assertTrue(state.sections.all { it.enabled })
        assertFalse(vm.isCustomized())
    }
}
