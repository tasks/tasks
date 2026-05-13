package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.tasks.R

@Composable
fun NavigationDrawerScreen(
    filtersEnabled: Boolean,
    showToday: Boolean,
    showRecentlyModified: Boolean,
    tagsEnabled: Boolean,
    hideUnusedTags: Boolean,
    placesEnabled: Boolean,
    hideUnusedPlaces: Boolean,
    onCustomizeDrawer: () -> Unit,
    onFiltersEnabled: (Boolean) -> Unit,
    onShowToday: (Boolean) -> Unit,
    onShowRecentlyModified: (Boolean) -> Unit,
    onTagsEnabled: (Boolean) -> Unit,
    onHideUnusedTags: (Boolean) -> Unit,
    onPlacesEnabled: (Boolean) -> Unit,
    onHideUnusedPlaces: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Customize drawer
        SettingsItemCard(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            PreferenceRow(
                title = stringResource(R.string.customize_drawer),
                summary = stringResource(R.string.customize_drawer_summary),
                showChevron = true,
                onClick = onCustomizeDrawer,
            )
        }

        // Filters section
        SectionHeader(
            R.string.filters,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.enabled),
                    checked = filtersEnabled,
                    onCheckedChange = onFiltersEnabled,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.today),
                    checked = showToday,
                    enabled = filtersEnabled,
                    onCheckedChange = onShowToday,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.BFE_Recent),
                    checked = showRecentlyModified,
                    enabled = filtersEnabled,
                    onCheckedChange = onShowRecentlyModified,
                )
            }
        }

        // Tags section
        SectionHeader(
            R.string.tags,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.enabled),
                    checked = tagsEnabled,
                    onCheckedChange = onTagsEnabled,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.hide_unused_tags),
                    checked = hideUnusedTags,
                    enabled = tagsEnabled,
                    onCheckedChange = onHideUnusedTags,
                )
            }
        }

        // Places section
        SectionHeader(
            R.string.places,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.enabled),
                    checked = placesEnabled,
                    onCheckedChange = onPlacesEnabled,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.hide_unused_places),
                    checked = hideUnusedPlaces,
                    enabled = placesEnabled,
                    onCheckedChange = onHideUnusedPlaces,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
