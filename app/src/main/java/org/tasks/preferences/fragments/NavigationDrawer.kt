package org.tasks.preferences.fragments

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.activities.NavigationDrawerCustomization
import org.tasks.compose.settings.NavigationDrawerScreen
import org.tasks.preferences.BasePreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class NavigationDrawer : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: NavigationDrawerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            NavigationDrawerScreen(
                filtersEnabled = viewModel.filtersEnabled,
                showToday = viewModel.showToday,
                showRecentlyModified = viewModel.showRecentlyModified,
                tagsEnabled = viewModel.tagsEnabled,
                hideUnusedTags = viewModel.hideUnusedTags,
                placesEnabled = viewModel.placesEnabled,
                hideUnusedPlaces = viewModel.hideUnusedPlaces,
                onCustomizeDrawer = {
                    startActivity(
                        Intent(requireContext(), NavigationDrawerCustomization::class.java)
                    )
                },
                onFiltersEnabled = { viewModel.updateFiltersEnabled(it) },
                onShowToday = { viewModel.updateShowToday(it) },
                onShowRecentlyModified = { viewModel.updateShowRecentlyModified(it) },
                onTagsEnabled = { viewModel.updateTagsEnabled(it) },
                onHideUnusedTags = { viewModel.updateHideUnusedTags(it) },
                onPlacesEnabled = { viewModel.updatePlacesEnabled(it) },
                onHideUnusedPlaces = { viewModel.updateHideUnusedPlaces(it) },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
        val surfaceColor = theme.themeBase.getSettingsSurfaceColor(requireActivity())
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(surfaceColor)
            (toolbar.parent as? View)?.setBackgroundColor(surfaceColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.content_background)
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(defaultColor)
            (toolbar.parent as? View)?.setBackgroundColor(defaultColor)
        }
    }
}
