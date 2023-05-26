package org.tasks.preferences

interface QueryPreferences {
    var sortMode: Int

    var groupMode: Int

    var completedMode: Int

    var isManualSort: Boolean

    var isAstridSort: Boolean

    var sortAscending: Boolean

    var groupAscending: Boolean

    var completedAscending: Boolean

    val showHidden: Boolean

    val showCompleted: Boolean

    var alwaysDisplayFullDate: Boolean

    var completedTasksAtBottom: Boolean
}