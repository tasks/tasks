package org.tasks.preferences

interface QueryPreferences {
    var sortMode: Int

    var isManualSort: Boolean

    var isAstridSort: Boolean

    var isReverseSort: Boolean

    val showHidden: Boolean

    val showCompleted: Boolean

    val showCompletedTemporarily: Boolean

    fun usePagedQueries(): Boolean
}