package org.tasks.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.DrawerSettings

open class NavigationDrawerViewModel(
    private val appPreferences: AppPreferences,
    private val refreshBroadcaster: RefreshBroadcaster,
    persistenceScope: CoroutineScope,
) : ViewModel() {

    var settings by mutableStateOf(DrawerSettings())
        private set

    var loaded by mutableStateOf(false)
        private set

    private val writes = PreferenceWriteQueue(
        viewModelScope = viewModelScope,
        persistenceScope = persistenceScope,
        tag = TAG,
        reload = { reloadSafely() },
    )

    init {
        viewModelScope.launch {
            reloadSafely()
        }
    }

    private suspend fun reload() {
        settings = appPreferences.drawerSettings()
        loaded = true
    }

    private suspend fun reloadSafely() {
        try {
            reload()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(e, tag = TAG) { "Failed to reload navigation drawer settings" }
        }
    }

    open fun refreshState() = writes.refresh()

    private fun persist(block: suspend () -> Unit) = writes.write {
        block()
        refreshBroadcaster.broadcastRefresh()
    }

    fun updateFiltersEnabled(enabled: Boolean) {
        settings = settings.copy(filtersEnabled = enabled)
        persist { appPreferences.setFiltersEnabled(enabled) }
    }

    fun updateShowToday(enabled: Boolean) {
        settings = settings.copy(todayFilter = enabled)
        persist { appPreferences.setTodayFilter(enabled) }
    }

    fun updateShowRecentlyModified(enabled: Boolean) {
        settings = settings.copy(recentlyModifiedFilter = enabled)
        persist { appPreferences.setRecentlyModifiedFilter(enabled) }
    }

    fun updateTagsEnabled(enabled: Boolean) {
        settings = settings.copy(tagsEnabled = enabled)
        persist { appPreferences.setTagsEnabled(enabled) }
    }

    fun updateHideUnusedTags(enabled: Boolean) {
        settings = settings.copy(hideUnusedTags = enabled)
        persist { appPreferences.setHideUnusedTags(enabled) }
    }

    fun updatePlacesEnabled(enabled: Boolean) {
        settings = settings.copy(placesEnabled = enabled)
        persist { appPreferences.setPlacesEnabled(enabled) }
    }

    fun updateHideUnusedPlaces(enabled: Boolean) {
        settings = settings.copy(hideUnusedPlaces = enabled)
        persist { appPreferences.setHideUnusedPlaces(enabled) }
    }
}

private const val TAG = "NavigationDrawerViewModel"
