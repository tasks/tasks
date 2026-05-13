package org.tasks.preferences.fragments

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.R
import org.tasks.preferences.Preferences
import javax.inject.Inject

@HiltViewModel
class NavigationDrawerViewModel @Inject constructor(
    private val preferences: Preferences,
) : ViewModel() {

    var filtersEnabled by mutableStateOf(true)
        private set
    var showToday by mutableStateOf(true)
        private set
    var showRecentlyModified by mutableStateOf(true)
        private set
    var tagsEnabled by mutableStateOf(true)
        private set
    var hideUnusedTags by mutableStateOf(false)
        private set
    var placesEnabled by mutableStateOf(true)
        private set
    var hideUnusedPlaces by mutableStateOf(false)
        private set

    init {
        refreshState()
    }

    fun refreshState() {
        filtersEnabled = preferences.getBoolean(R.string.p_filters_enabled, true)
        showToday = preferences.getBoolean(R.string.p_show_today_filter, true)
        showRecentlyModified = preferences.getBoolean(
            R.string.p_show_recently_modified_filter, true,
        )
        tagsEnabled = preferences.getBoolean(R.string.p_tags_enabled, true)
        hideUnusedTags = preferences.getBoolean(R.string.p_tags_hide_unused, false)
        placesEnabled = preferences.getBoolean(R.string.p_places_enabled, true)
        hideUnusedPlaces = preferences.getBoolean(R.string.p_places_hide_unused, false)
    }

    fun updateFiltersEnabled(enabled: Boolean) {
        preferences.setBoolean(R.string.p_filters_enabled, enabled)
        filtersEnabled = enabled
    }

    fun updateShowToday(enabled: Boolean) {
        preferences.setBoolean(R.string.p_show_today_filter, enabled)
        showToday = enabled
    }

    fun updateShowRecentlyModified(enabled: Boolean) {
        preferences.setBoolean(R.string.p_show_recently_modified_filter, enabled)
        showRecentlyModified = enabled
    }

    fun updateTagsEnabled(enabled: Boolean) {
        preferences.setBoolean(R.string.p_tags_enabled, enabled)
        tagsEnabled = enabled
    }

    fun updateHideUnusedTags(enabled: Boolean) {
        preferences.setBoolean(R.string.p_tags_hide_unused, enabled)
        hideUnusedTags = enabled
    }

    fun updatePlacesEnabled(enabled: Boolean) {
        preferences.setBoolean(R.string.p_places_enabled, enabled)
        placesEnabled = enabled
    }

    fun updateHideUnusedPlaces(enabled: Boolean) {
        preferences.setBoolean(R.string.p_places_hide_unused, enabled)
        hideUnusedPlaces = enabled
    }
}
