package org.tasks.preferences

import com.todoroo.astrid.core.SortHelper

class DefaultQueryPreferences : QueryPreferences {
    override var sortMode = SortHelper.SORT_DUE
    override var groupMode = SortHelper.SORT_DUE
    override var completedMode = SortHelper.SORT_AUTO
    override var subtaskMode = SortHelper.SORT_MANUAL
    override var isManualSort = false
    override var isAstridSort = false
    override var sortAscending = false
    override var groupAscending = true
    override var completedAscending = false
    override var subtaskAscending = false
    override val showHidden = false
    override val showCompleted = false
    override val alwaysDisplayFullDate = false
    override var completedTasksAtBottom = true
}
