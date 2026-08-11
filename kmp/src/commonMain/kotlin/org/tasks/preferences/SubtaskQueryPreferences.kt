package org.tasks.preferences

import com.todoroo.astrid.core.SortHelper

class SubtaskQueryPreferences(
    isGoogleTasks: Boolean,
) : QueryPreferences by DefaultQueryPreferences() {
    private val manualSort =
        if (isGoogleTasks) SortHelper.SORT_GTASKS else SortHelper.SORT_CALDAV

    override var sortMode = manualSort
    override var subtaskMode = manualSort
    override var groupMode = SortHelper.GROUP_NONE
    override var completedMode = SortHelper.SORT_COMPLETED
    override var subtaskAscending = true
    override var showHidden = true

    override var completedTasksAtBottom = false
}
