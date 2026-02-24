package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.tasks.R

@Composable
fun TaskListScreen(
    fontSize: Int,
    rowSpacing: Int,
    showFullTitle: Boolean,
    showDescription: Boolean,
    showFullDescription: Boolean,
    showLinks: Boolean,
    chipAppearanceSummary: String,
    subtaskChips: Boolean,
    startDateChips: Boolean,
    placeChips: Boolean,
    listChips: Boolean,
    tagChips: Boolean,
    onFontSize: (Int) -> Unit,
    onRowSpacing: (Int) -> Unit,
    onShowFullTitle: (Boolean) -> Unit,
    onShowDescription: (Boolean) -> Unit,
    onShowFullDescription: (Boolean) -> Unit,
    onShowLinks: (Boolean) -> Unit,
    onChipAppearance: () -> Unit,
    onSubtaskChips: (Boolean) -> Unit,
    onStartDateChips: (Boolean) -> Unit,
    onPlaceChips: (Boolean) -> Unit,
    onListChips: (Boolean) -> Unit,
    onTagChips: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Font size & row spacing island
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SliderPreferenceRow(
                    title = stringResource(R.string.font_size),
                    value = fontSize,
                    min = 10,
                    max = 48,
                    onValueChange = onFontSize,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                SliderPreferenceRow(
                    title = stringResource(R.string.row_spacing),
                    value = rowSpacing,
                    min = 0,
                    max = 16,
                    onValueChange = onRowSpacing,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        // Display options island
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.EPr_fullTask_title),
                    checked = showFullTitle,
                    onCheckedChange = onShowFullTitle,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.show_description),
                    checked = showDescription,
                    onCheckedChange = onShowDescription,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.show_full_description),
                    checked = showFullDescription,
                    enabled = showDescription,
                    onCheckedChange = onShowFullDescription,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.linkify),
                    summary = stringResource(R.string.linkify_description),
                    checked = showLinks,
                    onCheckedChange = onShowLinks,
                )
            }
        }

        // Chips section
        SectionHeader(
            R.string.chips,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(R.string.chip_appearance),
                    summary = chipAppearanceSummary,
                    onClick = onChipAppearance,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.subtasks),
                    checked = subtaskChips,
                    onCheckedChange = onSubtaskChips,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.start_date),
                    checked = startDateChips,
                    onCheckedChange = onStartDateChips,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.places),
                    checked = placeChips,
                    onCheckedChange = onPlaceChips,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.lists),
                    checked = listChips,
                    onCheckedChange = onListChips,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                SwitchPreferenceRow(
                    title = stringResource(R.string.tags),
                    checked = tagChips,
                    onCheckedChange = onTagChips,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun SliderPreferenceRow(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SettingsContentPadding + SettingsIconSize + SettingsContentPadding,
                end = SettingsContentPadding,
                top = SettingsRowPadding,
                bottom = SettingsRowPadding,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = max - min - 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
