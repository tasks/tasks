package org.tasks.wear

import org.tasks.GrpcProto.Settings
import org.tasks.preferences.Preferences
import org.tasks.preferences.QueryPreferences

class WearPreferences(
    preferences: Preferences,
    private val settings: Settings,
): QueryPreferences by preferences {
    override val showHidden: Boolean
        get() = settings.showHidden
}