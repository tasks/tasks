package org.tasks.compose.tasklist

import kotlin.math.max
import org.tasks.viewmodel.TaskListPreferencesViewModel

/**
 * Row-level styling derived from the desktop "Task list options" pane.
 *
 * Mirrors the native Android rules (`TaskViewHolder` / `ViewHolderFactory`):
 * the configured font size (default 16) is used for the title and the
 * description, the due-date detail text uses the font size minus 2 clamped
 * to a floor of 10, and the configured row spacing (default 16) is applied
 * as the vertical padding of each row.
 */
internal const val DETAIL_FONT_SIZE_FLOOR = 10

internal data class TaskRowStyle(
    val titleFontSizeSp: Int,
    val detailFontSizeSp: Int,
    val rowSpacingDp: Int,
)

internal fun taskRowStyle(fontSize: Int, rowSpacingDp: Int): TaskRowStyle = TaskRowStyle(
    titleFontSizeSp = fontSize,
    detailFontSizeSp = max(DETAIL_FONT_SIZE_FLOOR, fontSize - 2),
    rowSpacingDp = rowSpacingDp,
)

internal val DEFAULT_TASK_ROW_STYLE = taskRowStyle(
    TaskListPreferencesViewModel.DEFAULT_FONT_SIZE,
    TaskListPreferencesViewModel.DEFAULT_ROW_SPACING,
)
