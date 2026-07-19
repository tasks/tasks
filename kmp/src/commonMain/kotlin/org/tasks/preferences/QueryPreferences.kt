package org.tasks.preferences

interface QueryPreferences {
    var sortMode: Int

    var groupMode: Int

    var completedMode: Int

    var subtaskMode: Int

    var isManualSort: Boolean

    var isAstridSort: Boolean

    var sortAscending: Boolean

    var groupAscending: Boolean

    var completedAscending: Boolean

    var subtaskAscending: Boolean

    var showHidden: Boolean

    var showCompleted: Boolean

    val alwaysDisplayFullDate: Boolean

    var completedTasksAtBottom: Boolean
}