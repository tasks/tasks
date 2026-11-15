package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.compose.components.SymbolIcon
import org.tasks.themes.TasksIcons
import org.tasks.viewmodel.TaskListOptionsViewModel
import tasks.kmp.generated.resources.EPr_fullTask_title
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.chip_appearance
import tasks.kmp.generated.resources.chip_appearance_icon_only
import tasks.kmp.generated.resources.chip_appearance_text_and_icon
import tasks.kmp.generated.resources.chip_appearance_text_only
import tasks.kmp.generated.resources.chips
import tasks.kmp.generated.resources.font_size
import tasks.kmp.generated.resources.linkify
import tasks.kmp.generated.resources.linkify_description
import tasks.kmp.generated.resources.lists
import tasks.kmp.generated.resources.per_list_sort_summary
import tasks.kmp.generated.resources.per_list_sort_title
import tasks.kmp.generated.resources.places
import tasks.kmp.generated.resources.row_spacing
import tasks.kmp.generated.resources.show_description
import tasks.kmp.generated.resources.show_full_description
import tasks.kmp.generated.resources.start_date
import tasks.kmp.generated.resources.subtasks
import tasks.kmp.generated.resources.tags
import tasks.kmp.generated.resources.task_list_options

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListOptionsScreen(onNavigateBack: () -> Unit) {
    val viewModel = koinViewModel<TaskListOptionsViewModel>()
    var showChipAppearanceDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.task_list_options)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(
                            name = TasksIcons.ARROW_BACK,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            TaskListOptionsContent(
                viewModel = viewModel,
                showChipAppearanceDialog = showChipAppearanceDialog,
                onShowChipAppearanceDialog = {
                    viewModel.openChipAppearanceDialog()
                    showChipAppearanceDialog = true
                },
                onDismissChipAppearanceDialog = {
                    viewModel.dismissChipAppearanceDialog()
                    showChipAppearanceDialog = false
                },
            )
        }
    }

    if (showChipAppearanceDialog) {
        ChipAppearanceDialog(
            entries = viewModel.chipEntries,
            values = viewModel.chipValues,
            selectedValue = viewModel.chipValues.getOrNull(viewModel.chipAppearanceIndex.coerceAtLeast(0)),
            onSelect = { value ->
                viewModel.setChipAppearance(value.toIntOrNull() ?: 0)
                showChipAppearanceDialog = false
            },
            onDismiss = { showChipAppearanceDialog = false },
        )
    }
}

@Composable
private fun TaskListOptionsContent(
    viewModel: TaskListOptionsViewModel,
    showChipAppearanceDialog: Boolean,
    onShowChipAppearanceDialog: () -> Unit,
    onDismissChipAppearanceDialog: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SliderPreferenceRow(
                    title = stringResource(Res.string.font_size),
                    value = viewModel.fontSize,
                    min = 10,
                    max = 48,
                    onValueChange = { viewModel.updateFontSize(it.toInt()) },
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                SliderPreferenceRow(
                    title = stringResource(Res.string.row_spacing),
                    value = viewModel.rowSpacing,
                    min = 0,
                    max = 16,
                    onValueChange = { viewModel.updateRowSpacing(it.toInt()) },
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.EPr_fullTask_title),
                    checked = viewModel.showFullTitle,
                    onCheckedChange = viewModel::updateShowFullTitle,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.show_description),
                    checked = viewModel.showDescription,
                    onCheckedChange = viewModel::updateShowDescription,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.show_full_description),
                    checked = viewModel.showFullDescription,
                    enabled = viewModel.showDescription,
                    onCheckedChange = viewModel::updateShowFullDescription,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.linkify),
                    summary = stringResource(Res.string.linkify_description),
                    checked = viewModel.showLinks,
                    onCheckedChange = viewModel::updateShowLinks,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.Only) {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.per_list_sort_title),
                    summary = stringResource(Res.string.per_list_sort_summary),
                    checked = viewModel.perListSort,
                    onCheckedChange = viewModel::updatePerListSort,
                )
            }
        }

        SectionHeader(
            stringResource(Res.string.chips),
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                PreferenceRow(
                    title = stringResource(Res.string.chip_appearance),
                    summary = viewModel.chipAppearanceSummary,
                    onClick = onShowChipAppearanceDialog,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.subtasks),
                    checked = viewModel.subtaskChips,
                    onCheckedChange = viewModel::updateSubtaskChips,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.start_date),
                    checked = viewModel.startDateChips,
                    onCheckedChange = viewModel::updateStartDateChips,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.places),
                    checked = viewModel.placeChips,
                    onCheckedChange = viewModel::updatePlaceChips,
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.lists),
                    checked = viewModel.listChips,
                    onCheckedChange = viewModel::updateListChips,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.tags),
                    checked = viewModel.tagChips,
                    onCheckedChange = viewModel::updateTagChips,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
        Spacer(modifier = Modifier.height(16.dp))
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
        modifier =
            Modifier
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ChipAppearanceDialog(
    entries: List<String>,
    values: List<String>,
    selectedValue: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Column {
            entries.forEachIndexed { index, entry ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(values[index]) }
                            .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = values[index] == selectedValue,
                        onClick = { onSelect(values[index]) },
                    )
                    Text(
                        text = entry,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
