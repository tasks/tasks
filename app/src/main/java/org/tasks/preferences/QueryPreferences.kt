package org.tasks.preferences

interface QueryPreferences {
    var sortMode: Int

    var isManualSort: Boolean

    var isAstridSort: Boolean

    var isReverseSort: Boolean

    val showHidden: Boolean

    val showCompleted: Boolean

    var alwaysDisplayFullDate: Boolean

    fun usePagedQueries(): Boolean
}