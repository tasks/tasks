package org.tasks.compose.tasklist

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.viewmodel.TaskListPreferencesViewModel

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

    @Test
    fun theChipTogglesDefaultOnAndPassThrough() {
        val all = taskRowStyle(fontSize = 16, rowSpacingDp = 16)
        assertEquals(true, all.subtaskChips)
        assertEquals(true, all.startDateChips)
        assertEquals(true, all.placeChips)
        assertEquals(true, all.listChips)
        assertEquals(true, all.tagChips)

        val none = taskRowStyle(
            fontSize = 16,
            rowSpacingDp = 16,
            subtaskChips = false,
            startDateChips = false,
            placeChips = false,
            listChips = false,
            tagChips = false,
        )
        assertEquals(false, none.subtaskChips)
        assertEquals(false, none.startDateChips)
        assertEquals(false, none.placeChips)
        assertEquals(false, none.listChips)
        assertEquals(false, none.tagChips)
    }

    @Test
    fun theChipAppearanceDrivesTextAndIconVisibility() {
        val both = taskRowStyle(fontSize = 16, rowSpacingDp = 16)
        assertEquals(true, both.chipShowText)
        assertEquals(true, both.chipShowIcon)

        val textOnly = taskRowStyle(
            fontSize = 16,
            rowSpacingDp = 16,
            chipAppearance = TaskListPreferencesViewModel.CHIP_APPEARANCE_TEXT_ONLY,
        )
        assertEquals(true, textOnly.chipShowText)
        assertEquals(false, textOnly.chipShowIcon)

        val iconOnly = taskRowStyle(
            fontSize = 16,
            rowSpacingDp = 16,
            chipAppearance = TaskListPreferencesViewModel.CHIP_APPEARANCE_ICON_ONLY,
        )
        assertEquals(false, iconOnly.chipShowText)
        assertEquals(true, iconOnly.chipShowIcon)
    }
}
