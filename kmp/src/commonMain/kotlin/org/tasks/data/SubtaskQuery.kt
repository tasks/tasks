package org.tasks.data

import org.tasks.filters.SubtaskFilter
import org.tasks.preferences.SubtaskQueryPreferences

fun subtaskQuery(parentId: Long, isGoogleTasks: Boolean): String =
    TaskListQuery.getQuery(
        preferences = SubtaskQueryPreferences(isGoogleTasks),
        filter = SubtaskFilter(parentId),
        showCollapsed = true,
    )
