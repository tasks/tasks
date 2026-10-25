package org.tasks.compose.tasklist

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskRowStyleTest {
    @Test
    fun theConfiguredFontSizeIsUsedForTheTitleAndDescription() {
        assertEquals(20, taskRowStyle(fontSize = 20, rowSpacingDp = 16).titleFontSizeSp)
        assertEquals(16, taskRowStyle(fontSize = 16, rowSpacingDp = 16).titleFontSizeSp)
    }

    @Test
    fun theDueDateFontIsTwoSmallerThanTheTitle() {
        assertEquals(14, taskRowStyle(fontSize = 16, rowSpacingDp = 16).detailFontSizeSp)
        assertEquals(38, taskRowStyle(fontSize = 40, rowSpacingDp = 16).detailFontSizeSp)
    }

    @Test
    fun theDueDateFontNeverDropsBelowTheFloor() {
        assertEquals(10, taskRowStyle(fontSize = 8, rowSpacingDp = 16).detailFontSizeSp)
        assertEquals(10, taskRowStyle(fontSize = 10, rowSpacingDp = 16).detailFontSizeSp)
        assertEquals(10, taskRowStyle(fontSize = 2, rowSpacingDp = 16).detailFontSizeSp)
    }

    @Test
    fun theRowSpacingPassesThrough() {
        assertEquals(24, taskRowStyle(fontSize = 16, rowSpacingDp = 24).rowSpacingDp)
        assertEquals(0, taskRowStyle(fontSize = 16, rowSpacingDp = 0).rowSpacingDp)
    }

    @Test
    fun theDefaultStyleMatchesTheNativeDefaults() {
        assertEquals(16, DEFAULT_TASK_ROW_STYLE.titleFontSizeSp)
        assertEquals(14, DEFAULT_TASK_ROW_STYLE.detailFontSizeSp)
        assertEquals(16, DEFAULT_TASK_ROW_STYLE.rowSpacingDp)
        assertEquals(false, DEFAULT_TASK_ROW_STYLE.showFullTitle)
    }

    @Test
    fun theFullTitleTogglePassesThroughAndDefaultsOff() {
        assertEquals(true, taskRowStyle(fontSize = 16, rowSpacingDp = 16, showFullTitle = true).showFullTitle)
        assertEquals(false, taskRowStyle(fontSize = 16, rowSpacingDp = 16).showFullTitle)
    }

    @Test
    fun aClampedTitleCollapsesNewlinesIntoSpaces() {
        assertEquals("line one line two", titleContent("line one\nline two", showFullTitle = false))
        assertEquals("", titleContent(null, showFullTitle = false))
    }

    @Test
    fun aFullTitleKeepsItsNewlines() {
        assertEquals("line one\nline two", titleContent("line one\nline two", showFullTitle = true))
        assertEquals("", titleContent(null, showFullTitle = true))
    }
}
