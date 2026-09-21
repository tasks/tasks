package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.tasks.viewmodel.NavigationDrawerViewModel

@Composable
fun NavigationDrawerContent(
    viewModel: NavigationDrawerViewModel,
    onCustomizeDrawer: (() -> Unit)? = null,
) {
    if (!viewModel.loaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }
    val settings = viewModel.settings
    NavigationDrawerScreen(
        filtersEnabled = settings.filtersEnabled,
        showToday = settings.todayFilter,
        showRecentlyModified = settings.recentlyModifiedFilter,
        tagsEnabled = settings.tagsEnabled,
        hideUnusedTags = settings.hideUnusedTags,
        placesEnabled = settings.placesEnabled,
        hideUnusedPlaces = settings.hideUnusedPlaces,
        onCustomizeDrawer = onCustomizeDrawer,
        onFiltersEnabled = { viewModel.updateFiltersEnabled(it) },
        onShowToday = { viewModel.updateShowToday(it) },
        onShowRecentlyModified = { viewModel.updateShowRecentlyModified(it) },
        onTagsEnabled = { viewModel.updateTagsEnabled(it) },
        onHideUnusedTags = { viewModel.updateHideUnusedTags(it) },
        onPlacesEnabled = { viewModel.updatePlacesEnabled(it) },
        onHideUnusedPlaces = { viewModel.updateHideUnusedPlaces(it) },
    )
}
