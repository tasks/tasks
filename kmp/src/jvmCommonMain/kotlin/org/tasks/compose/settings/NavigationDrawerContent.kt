package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import org.tasks.viewmodel.NavigationDrawerViewModel

@Composable
fun NavigationDrawerContent(
    viewModel: NavigationDrawerViewModel,
    onCustomizeDrawer: (() -> Unit)? = null,
) {
    NavigationDrawerScreen(
        filtersEnabled = viewModel.filtersEnabled,
        showToday = viewModel.showToday,
        showRecentlyModified = viewModel.showRecentlyModified,
        tagsEnabled = viewModel.tagsEnabled,
        hideUnusedTags = viewModel.hideUnusedTags,
        placesEnabled = viewModel.placesEnabled,
        hideUnusedPlaces = viewModel.hideUnusedPlaces,
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
