package org.tasks.preferences

import com.todoroo.astrid.core.SortHelper
import org.tasks.R

class FilterPreferences(
    private val preferences: Preferences,
    private val filterKey: String,
) : QueryPreferences by preferences {

    private fun getInt(suffix: String, default: Int): Int {
        val key = getKey(suffix)
        return if (preferences.isPerListSortEnabled) {
             preferences.getInt(key, default)
        } else {
            default
        }
    }

    private fun setInt(suffix: String, value: Int) {
        preferences.setInt(getKey(suffix), value)
    }

    private fun getBoolean(suffix: String, default: Boolean): Boolean {
        val key = getKey(suffix)
        return if (preferences.isPerListSortEnabled) {
            preferences.getBoolean(key, default)
        } else {
            default
        }
    }

    private fun setBoolean(suffix: String, value: Boolean) {
        preferences.setBoolean(getKey(suffix), value)
    }

    private fun getKey(suffix: String) = "${filterKey}_$suffix"

    override var sortMode: Int
        get() = getInt(KEY_SORT_MODE, preferences.sortMode)
        set(value) {
            if (preferences.isPerListSortEnabled) {
                setInt(KEY_SORT_MODE, value)
            } else {
                preferences.sortMode = value
            }
        }

    override var groupMode: Int
        get() = getInt(KEY_GROUP_MODE, preferences.groupMode)
        set(value) {
            if (preferences.isPerListSortEnabled) {
                setInt(KEY_GROUP_MODE, value)
            } else {
                preferences.groupMode = value
            }
        }

    override var completedMode: Int
        get() = getInt(KEY_COMPLETED_MODE, preferences.completedMode)
        set(value) {
            if (preferences.isPerListSortEnabled) {
                setInt(KEY_COMPLETED_MODE, value)
            } else {
                preferences.completedMode = value
            }
        }

    override var subtaskMode: Int
        get() = getInt(KEY_SUBTASK_MODE, preferences.subtaskMode)
        set(value) {
            if (preferences.isPerListSortEnabled) {
                setInt(KEY_SUBTASK_MODE, value)
            } else {
                preferences.subtaskMode = value
            }
        }

    override var isManualSort: Boolean
        get() = getBoolean(KEY_MANUAL_SORT, preferences.isManualSort)
        set(value) {
            if (preferences.isPerListSortEnabled) {
                setBoolean(KEY_MANUAL_SORT, value)
            } else {
                preferences.isManualSort = value
            }
        }

    override var isAstridSort: Boolean
        get() = getBoolean(KEY_ASTRID_SORT, preferences.isAstridSort)
        set(value) {
            if (preferences.isPerListSortEnabled) {
                setBoolean(KEY_ASTRID_SORT, value)
            } else {
                preferences.isAstridSort = value
            }
        }

    override var sortAscending: Boolean
        get() = getBoolean(KEY_SORT_ASCENDING, preferences.sortAscending)
        set(value) {
            if (preferences.isPerListSortEnabled) {
                setBoolean(KEY_SORT_ASCENDING, value)
            } else {
                preferences.sortAscending = value
            }
        }

    override var groupAscending: Boolean
        get() = getBoolean(KEY_GROUP_ASCENDING, preferences.groupAscending)
        set(value) {
            if (preferences.isPerListSortEnabled) {
                setBoolean(KEY_GROUP_ASCENDING, value)
            } else {
                preferences.groupAscending = value
            }
        }

    override var completedAscending: Boolean
        get() = getBoolean(KEY_COMPLETED_ASCENDING, preferences.completedAscending)
        set(value) {
            if (preferences.isPerListSortEnabled) {
                setBoolean(KEY_COMPLETED_ASCENDING, value)
            } else {
                preferences.completedAscending = value
            }
        }

    override var subtaskAscending: Boolean
        get() = getBoolean(KEY_SUBTASK_ASCENDING, preferences.subtaskAscending)
        set(value) {
            if (preferences.isPerListSortEnabled) {
                setBoolean(KEY_SUBTASK_ASCENDING, value)
            } else {
                preferences.subtaskAscending = value
            }
        }

    override var completedTasksAtBottom: Boolean
        get() = getBoolean(KEY_COMPLETED_AT_BOTTOM, preferences.completedTasksAtBottom)
        set(value) {
            if (preferences.isPerListSortEnabled) {
                setBoolean(KEY_COMPLETED_AT_BOTTOM, value)
            } else {
                preferences.completedTasksAtBottom = value
            }
        }
    
    companion object {
        private const val KEY_SORT_MODE = "sort_mode"
        private const val KEY_GROUP_MODE = "group_mode"
        private const val KEY_COMPLETED_MODE = "completed_mode"
        private const val KEY_SUBTASK_MODE = "subtask_mode"
        private const val KEY_MANUAL_SORT = "manual_sort"
        private const val KEY_ASTRID_SORT = "astrid_sort"
        private const val KEY_SORT_ASCENDING = "sort_ascending"
        private const val KEY_GROUP_ASCENDING = "group_ascending"
        private const val KEY_COMPLETED_ASCENDING = "completed_ascending"
        private const val KEY_SUBTASK_ASCENDING = "subtask_ascending"
        private const val KEY_COMPLETED_AT_BOTTOM = "completed_at_bottom"
    }
}
