package org.tasks.preferences

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.InMemoryDataStore
import org.tasks.sync.SyncSource

class UpgradeOngoingSyncSourceTest {
    private val preferences = TasksPreferences(InMemoryDataStore())

    private suspend fun syncSource() =
        SyncSource.fromString(preferences.get(TasksPreferences.syncSource, SyncSource.NONE.name))

    @Test
    fun aManualSyncShowsTheIndicatorForAHiddenSyncThatIsRunning() = runBlocking {
        preferences.set(TasksPreferences.syncOngoing, true)
        preferences.set(TasksPreferences.syncSource, SyncSource.APP_RESUME.name)

        assertTrue(preferences.upgradeOngoingSyncSource(SyncSource.USER_INITIATED))
        assertEquals(SyncSource.USER_INITIATED, syncSource())
    }

    @Test
    fun aSyncThatAlreadyFinishedIsNotShownAsRunning() = runBlocking {
        preferences.set(TasksPreferences.syncOngoing, false)
        preferences.set(TasksPreferences.syncSource, SyncSource.NONE.name)

        assertFalse(preferences.upgradeOngoingSyncSource(SyncSource.USER_INITIATED))
        assertEquals(SyncSource.NONE, syncSource())
    }

    @Test
    fun nothingIsShownBeforeTheFirstSync() = runBlocking {
        assertFalse(preferences.upgradeOngoingSyncSource(SyncSource.USER_INITIATED))
        assertEquals(SyncSource.NONE, syncSource())
    }

    @Test
    fun aHiddenRequestDoesNotHideAVisibleSync() = runBlocking {
        preferences.set(TasksPreferences.syncOngoing, true)
        preferences.set(TasksPreferences.syncSource, SyncSource.USER_INITIATED.name)

        preferences.upgradeOngoingSyncSource(SyncSource.APP_RESUME)

        assertEquals(SyncSource.USER_INITIATED, syncSource())
    }
}
