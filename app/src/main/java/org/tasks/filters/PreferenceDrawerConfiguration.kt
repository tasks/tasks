package org.tasks.filters

import org.tasks.R
import org.tasks.compose.drawer.DrawerConfiguration
import org.tasks.preferences.Preferences

class PreferenceDrawerConfiguration(
    private val preferences: Preferences
) : DrawerConfiguration {
    override val filtersEnabled: Boolean
        get() = preferences.getBoolean(R.string.p_filters_enabled, super.filtersEnabled)

    override val placesEnabled: Boolean
        get() = preferences.getBoolean(R.string.p_places_enabled, super.placesEnabled)

    override val hideUnusedPlaces: Boolean
        get() = preferences.getBoolean(R.string.p_places_hide_unused, super.hideUnusedPlaces)

    override val tagsEnabled: Boolean
        get() = preferences.getBoolean(R.string.p_tags_enabled, super.tagsEnabled)

    override val hideUnusedTags: Boolean
        get() = preferences.getBoolean(R.string.p_tags_hide_unused, super.hideUnusedTags)

    override val todayFilter: Boolean
        get() = preferences.getBoolean(R.string.p_show_today_filter, super.todayFilter)

    override val recentlyModifiedFilter: Boolean
        get() = preferences.getBoolean(R.string.p_show_recently_modified_filter, super.recentlyModifiedFilter)

    override val localListsEnabled: Boolean
        get() = preferences.getBoolean(R.string.p_lists_enabled, super.localListsEnabled)
}