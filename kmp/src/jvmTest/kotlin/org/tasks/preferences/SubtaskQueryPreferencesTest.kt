package org.tasks.preferences

import com.todoroo.astrid.core.SortHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtaskQueryPreferencesTest {
    private val caldav = SubtaskQueryPreferences(isGoogleTasks = false)
    private val google = SubtaskQueryPreferences(isGoogleTasks = true)

    @Test
    fun subtasksAreReadInTheOrderTheBackendStoresThemIn() {
        assertEquals(SortHelper.SORT_CALDAV, caldav.sortMode)
        assertEquals(SortHelper.SORT_CALDAV, caldav.subtaskMode)
        assertEquals(SortHelper.SORT_GTASKS, google.sortMode)
        assertEquals(SortHelper.SORT_GTASKS, google.subtaskMode)
        assertTrue(caldav.subtaskAscending)
        assertTrue(caldav.sortAscending)
    }

    @Test
    fun manualSortIsNotClaimedEvenThoughTheOrderIsManual() {
        assertFalse(caldav.isManualSort)
        assertFalse(caldav.isAstridSort)
    }

    @Test
    fun everySubtaskIsShown() {
        assertTrue(caldav.showHidden)
        assertTrue(caldav.showCompleted)
        assertTrue(caldav.showCompletedSubtasks)
    }

    @Test
    fun completedSubtasksStayWhereTheUserPutThem() {
        assertFalse(caldav.completedTasksAtBottom)
        assertEquals(SortHelper.SORT_COMPLETED, caldav.completedMode)
        assertFalse(caldav.completedAscending)
    }

    @Test
    fun subtasksAreNotSplitIntoGroups() {
        assertEquals(SortHelper.GROUP_NONE, caldav.groupMode)
        assertTrue(caldav.groupAscending)
    }

    @Test
    fun datesAreShownTheWayTheyAreEverywhereElse() {
        assertFalse(caldav.alwaysDisplayFullDate)
    }

    @Test
    fun oneInstanceDoesNotShareItsSettingsWithAnother() {
        assertTrue(caldav.showCompleted)
        assertTrue(google.showCompleted)

        google.showCompleted = false

        assertTrue(caldav.showCompleted)
        assertTrue(SubtaskQueryPreferences(isGoogleTasks = true).showCompleted)
    }
}
