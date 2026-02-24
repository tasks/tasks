package org.tasks.wear

import org.tasks.preferences.Preferences
import org.tasks.preferences.QueryPreferences

class WearPreferences(
    private val delegate: Preferences,
    private val wearShowHidden: Boolean,
    private val wearShowCompleted: Boolean,
    private val wearSortMode: Int? = null,
    private val wearGroupMode: Int? = null,
): QueryPreferences by delegate {
    override val showHidden: Boolean
        get() = wearShowHidden

    override val showCompleted: Boolean
        get() = wearShowCompleted

    override var sortMode: Int
        get() = wearSortMode ?: delegate.sortMode
        set(_) {}

    override var groupMode: Int
        get() = wearGroupMode ?: delegate.groupMode
        set(_) {}
}
