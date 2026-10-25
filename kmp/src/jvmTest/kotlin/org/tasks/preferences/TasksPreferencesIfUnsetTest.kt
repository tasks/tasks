package org.tasks.preferences

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.InMemoryDataStore

class TasksPreferencesIfUnsetTest {

    @Test
    fun ifUnsetSeedsWhenMissing() = runTest {
        val prefs = TasksPreferences(InMemoryDataStore())
        assertTrue(prefs.ifUnset(TasksPreferences.taskEditBackButtonSaves, true))
        assertEquals(true, prefs.get(TasksPreferences.taskEditBackButtonSaves, false))
    }

    @Test
    fun ifUnsetNeverOverridesAStoredValue() = runTest {
        val prefs = TasksPreferences(InMemoryDataStore())
        // First seed wins; a later seed with a different value is a no-op.
        assertTrue(prefs.ifUnset(TasksPreferences.taskEditBackButtonSaves, true))
        assertFalse(prefs.ifUnset(TasksPreferences.taskEditBackButtonSaves, false))
        assertEquals(true, prefs.get(TasksPreferences.taskEditBackButtonSaves, false))
    }

    @Test
    fun ifUnsetDoesNotTouchAUserSetValue() = runTest {
        val prefs = TasksPreferences(InMemoryDataStore())
        // The user turned the preference off before the seed ran.
        prefs.set(TasksPreferences.taskEditBackButtonSaves, false)
        assertFalse(prefs.ifUnset(TasksPreferences.taskEditBackButtonSaves, true))
        assertEquals(false, prefs.get(TasksPreferences.taskEditBackButtonSaves, true))
    }
}
