package org.tasks.preferences

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.InMemoryDataStore

class InstallIdTest {
    private val store = InMemoryDataStore()
    private val preferences = TasksPreferences(store)

    @Test
    fun theSameIdComesBackEveryTime() = runBlocking {
        val first = InstallId(preferences).get()

        assertTrue(first.isNotBlank())
        assertEquals(first, InstallId(preferences).get())
    }

    @Test
    fun aDeviceThatAlreadyRegisteredAnSseTokenKeepsIt() = runBlocking {
        preferences.set(TasksPreferences.pushFallbackToken, "already-registered")

        assertEquals("already-registered", InstallId(preferences).get())
    }

    @Test
    fun theIdIsShortEnoughForTheServerToKeep() = runBlocking {
        assertTrue(InstallId(preferences).get().length <= 64)
    }
}
