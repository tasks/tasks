package org.tasks.preferences

interface QueryPreferences {
    var sortMode: Int

    var groupMode: Int

    var isManualSort: Boolean

    var isAstridSort: Boolean

    var sortAscending: Boolean

    var groupAscending: Boolean

    val showHidden: Boolean

    val showCompleted: Boolean

    var alwaysDisplayFullDate: Boolean

    val completedTasksAtBottom: Boolean

    val sortCompletedByCompletionDate: Boolean
}