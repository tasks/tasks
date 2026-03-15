package org.tasks.watch

import org.tasks.preferences.Preferences
import org.tasks.preferences.QueryPreferences

internal class WatchQueryPreferences(
    private val delegate: Preferences,
    private val overrideShowHidden: Boolean,
    private val overrideShowCompleted: Boolean,
    private val overrideSortMode: Int? = null,
    private val overrideGroupMode: Int? = null,
) : QueryPreferences by delegate {
    override val showHidden: Boolean
        get() = overrideShowHidden

    override val showCompleted: Boolean
        get() = overrideShowCompleted

    override var sortMode: Int
        get() = overrideSortMode ?: delegate.sortMode
        set(_) {}

    override var groupMode: Int
        get() = overrideGroupMode ?: delegate.groupMode
        set(_) {}
}
