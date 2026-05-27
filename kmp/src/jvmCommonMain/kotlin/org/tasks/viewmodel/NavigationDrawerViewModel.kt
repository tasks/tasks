package org.tasks.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.preferences.TasksPreferences

open class NavigationDrawerViewModel(
    private val tasksPreferences: TasksPreferences,
    private val refreshBroadcaster: RefreshBroadcaster,
) : ViewModel() {

    var filtersEnabled by mutableStateOf(
        runBlocking { tasksPreferences.get(TasksPreferences.filtersEnabled, true) }
    )
        private set

    var showToday by mutableStateOf(
        runBlocking { tasksPreferences.get(TasksPreferences.showTodayFilter, true) }
    )
        private set

    var showRecentlyModified by mutableStateOf(
        runBlocking { tasksPreferences.get(TasksPreferences.showRecentlyModifiedFilter, true) }
    )
        private set

    var tagsEnabled by mutableStateOf(
        runBlocking { tasksPreferences.get(TasksPreferences.tagsEnabled, true) }
    )
        private set

    var hideUnusedTags by mutableStateOf(
        runBlocking { tasksPreferences.get(TasksPreferences.tagsHideUnused, false) }
    )
        private set

    var placesEnabled by mutableStateOf(
        runBlocking { tasksPreferences.get(TasksPreferences.placesEnabled, true) }
    )
        private set

    var hideUnusedPlaces by mutableStateOf(
        runBlocking { tasksPreferences.get(TasksPreferences.placesHideUnused, false) }
    )
        private set

    fun refreshState() {
        viewModelScope.launch {
            filtersEnabled = tasksPreferences.get(TasksPreferences.filtersEnabled, true)
            showToday = tasksPreferences.get(TasksPreferences.showTodayFilter, true)
            showRecentlyModified = tasksPreferences.get(
                TasksPreferences.showRecentlyModifiedFilter, true,
            )
            tagsEnabled = tasksPreferences.get(TasksPreferences.tagsEnabled, true)
            hideUnusedTags = tasksPreferences.get(TasksPreferences.tagsHideUnused, false)
            placesEnabled = tasksPreferences.get(TasksPreferences.placesEnabled, true)
            hideUnusedPlaces = tasksPreferences.get(TasksPreferences.placesHideUnused, false)
        }
    }

    private fun refresh() {
        refreshBroadcaster.broadcastRefresh()
    }

    fun updateFiltersEnabled(enabled: Boolean) {
        filtersEnabled = enabled
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.filtersEnabled, enabled)
            refresh()
        }
    }

    fun updateShowToday(enabled: Boolean) {
        showToday = enabled
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.showTodayFilter, enabled)
            refresh()
        }
    }

    fun updateShowRecentlyModified(enabled: Boolean) {
        showRecentlyModified = enabled
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.showRecentlyModifiedFilter, enabled)
            refresh()
        }
    }

    fun updateTagsEnabled(enabled: Boolean) {
        tagsEnabled = enabled
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.tagsEnabled, enabled)
            refresh()
        }
    }

    fun updateHideUnusedTags(enabled: Boolean) {
        hideUnusedTags = enabled
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.tagsHideUnused, enabled)
            refresh()
        }
    }

    fun updatePlacesEnabled(enabled: Boolean) {
        placesEnabled = enabled
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.placesEnabled, enabled)
            refresh()
        }
    }

    fun updateHideUnusedPlaces(enabled: Boolean) {
        hideUnusedPlaces = enabled
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.placesHideUnused, enabled)
            refresh()
        }
    }
}
