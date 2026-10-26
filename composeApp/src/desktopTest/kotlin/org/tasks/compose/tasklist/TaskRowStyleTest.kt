package org.tasks.compose.tasklist

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.viewmodel.TaskListPreferencesViewModel

private const val TWO_LINES = "line one\nline two"
private const val TWO_LINES_CLAMPED = "line one line two"

class TaskRowStyleTest {
    @Test
    fun theConfiguredFontSizeIsUsedForTheTitle() {
        assertEquals(20, TaskRowStyle(titleFontSizeSp = 20).titleFontSizeSp)
        assertEquals(16, TaskRowStyle(titleFontSizeSp = 16).titleFontSizeSp)
    }

    @Test
    fun theDueDateFontIsTwoSmallerThanTheTitle() {
        assertEquals(14, TaskRowStyle(titleFontSizeSp = 16).detailFontSizeSp)
        assertEquals(38, TaskRowStyle(titleFontSizeSp = 40).detailFontSizeSp)
    }

    @Test
    fun theDueDateFontNeverDropsBelowTheFloor() {
        assertEquals(10, TaskRowStyle(titleFontSizeSp = 8).detailFontSizeSp)
        assertEquals(10, TaskRowStyle(titleFontSizeSp = 10).detailFontSizeSp)
        assertEquals(10, TaskRowStyle(titleFontSizeSp = 2).detailFontSizeSp)
    }

    @Test
    fun theRowSpacingPassesThrough() {
        assertEquals(24, TaskRowStyle(rowSpacingDp = 24).rowSpacingDp)
        assertEquals(0, TaskRowStyle(rowSpacingDp = 0).rowSpacingDp)
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
        assertEquals(true, TaskRowStyle(showFullTitle = true).showFullTitle)
        assertEquals(false, TaskRowStyle().showFullTitle)
    }

    @Test
    fun theFullTitleToggleDecidesWhetherNewlinesSurvive() {
        assertEquals(TWO_LINES_CLAMPED, titleContent(TWO_LINES, showFullTitle = false))
        assertEquals("", titleContent(null, showFullTitle = false))

        assertEquals(TWO_LINES, titleContent(TWO_LINES, showFullTitle = true))
        assertEquals("", titleContent(null, showFullTitle = true))
    }

    @Test
    fun theDescriptionTogglesDefaultSensibly() {
        val all = TaskRowStyle()
        assertEquals(true, all.showDescription)
        assertEquals(false, all.showFullDescription)

        val none = TaskRowStyle(showDescription = false, showFullDescription = true)
        assertEquals(false, none.showDescription)
        assertEquals(true, none.showFullDescription)
    }

    @Test
    fun theChipTogglesDefaultOnAndPassThrough() {
        val all = TaskRowStyle()
        assertEquals(true, all.subtaskChips)
        assertEquals(true, all.startDateChips)
        assertEquals(true, all.placeChips)
        assertEquals(true, all.listChips)
        assertEquals(true, all.tagChips)

        val none = TaskRowStyle(
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
        val both = TaskRowStyle()
        assertEquals(true, both.chipShowText)
        assertEquals(true, both.chipShowIcon)

        val textOnly = TaskRowStyle(chipAppearance = TaskListPreferencesViewModel.CHIP_APPEARANCE_TEXT_ONLY)
        assertEquals(true, textOnly.chipShowText)
        assertEquals(false, textOnly.chipShowIcon)

        val iconOnly = TaskRowStyle(chipAppearance = TaskListPreferencesViewModel.CHIP_APPEARANCE_ICON_ONLY)
        assertEquals(false, iconOnly.chipShowText)
        assertEquals(true, iconOnly.chipShowIcon)
    }
}
