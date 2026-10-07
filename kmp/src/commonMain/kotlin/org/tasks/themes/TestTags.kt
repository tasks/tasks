package org.tasks.themes

import androidx.compose.ui.Modifier

expect fun Modifier.exposeTestTags(): Modifier

object TestTags {
    const val CONTINUE_WITHOUT_SYNC = "continue_without_sync"
    const val NAVIGATION_DRAWER = "navigation_drawer"
    const val LIST_PICKER = "list_picker"
    const val DUE_DATE_ROW = "due_date_row"
    const val DATE_SHORTCUT_TODAY = "date_shortcut_today"
    const val DATE_PICKER_OK = "date_picker_ok"
    const val SAVE_TASK = "save_task"
}
