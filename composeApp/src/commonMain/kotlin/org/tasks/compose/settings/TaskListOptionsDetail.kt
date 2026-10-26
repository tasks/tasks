package org.tasks.compose.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.tasks.compose.components.SymbolIcon
import org.tasks.themes.TasksIcons
import org.tasks.viewmodel.TaskListPreferencesViewModel
import org.tasks.viewmodel.TaskListPreferencesViewModel.ChipOption
import org.tasks.viewmodel.TaskListPreferencesViewModel.SliderOption
import org.tasks.viewmodel.TaskListPreferencesViewModel.State
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.EPr_fullTask_title
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

/**
 * The desktop "Task list options" settings pane - a mirror of the native
 * Android TaskListPreferences screen (same rows, same order, same
 * defaults), storing values in the shared TasksPreferences DataStore.
 *
 * All rows except linkify are live: per-list sorting, font size, row
 * spacing, full title, description visibility, chip appearance and the
 * per-chip toggles are consumed by the task rows.
 */
@Composable
fun TaskListOptionsDetail(
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<TaskListPreferencesViewModel>()
    val state by viewModel.state.collectAsState()
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
        // Hold off rendering until the persisted values have loaded, so the
        // rows don't flash their defaults (and animate) before the real
        // state arrives.
        if (state.loaded) {
            TaskListOptionsScreen(
                state = state,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

@Composable
fun TaskListOptionsScreen(
    state: State,
    viewModel: TaskListPreferencesViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SettingsContentPadding),
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))
        AppearanceCard(
            state = state,
            onFontSizeChange = { viewModel.updateSlider(SliderOption.FONT_SIZE, it) },
            onRowSpacingChange = { viewModel.updateSlider(SliderOption.ROW_SPACING, it) },
        )
        Spacer(modifier = Modifier.height(SettingsContentPadding))
        DisplayCard(
            state = state,
            onShowFullTitleChange = viewModel::updateShowFullTitle,
            onShowDescriptionChange = viewModel::updateShowDescription,
            onShowFullDescriptionChange = viewModel::updateShowFullDescription,
        )
        Spacer(modifier = Modifier.height(SettingsContentPadding))
        PerListSortCard(
            state = state,
            onPerListSortChange = viewModel::updatePerListSort,
        )
        ChipsCard(
            state = state,
            viewModel = viewModel,
        )
        Spacer(modifier = Modifier.height(SettingsContentPadding))
    }
}

@Composable
private fun AppearanceCard(
    state: State,
    onFontSizeChange: (Int) -> Unit,
    onRowSpacingChange: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
    ) {
        SettingsItemCard(position = CardPosition.First) {
            SliderPreferenceRow(
                title = stringResource(Res.string.font_size),
                value = state.fontSize,
                min = 10,
                max = 48,
                onValueChange = onFontSizeChange,
            )
        }
        SettingsItemCard(position = CardPosition.Last) {
            SliderPreferenceRow(
                title = stringResource(Res.string.row_spacing),
                value = state.rowSpacing,
                min = 0,
                max = 16,
                onValueChange = onRowSpacingChange,
            )
        }
    }
}

@Composable
private fun DisplayCard(
    state: State,
    onShowFullTitleChange: (Boolean) -> Unit,
    onShowDescriptionChange: (Boolean) -> Unit,
    onShowFullDescriptionChange: (Boolean) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
    ) {
        SettingsItemCard(position = CardPosition.First) {
            SwitchPreferenceRow(
                title = stringResource(Res.string.EPr_fullTask_title),
                checked = state.showFullTitle,
                onCheckedChange = onShowFullTitleChange,
            )
        }
        SettingsItemCard(position = CardPosition.Middle) {
            SwitchPreferenceRow(
                title = stringResource(Res.string.show_description),
                checked = state.showDescription,
                onCheckedChange = onShowDescriptionChange,
            )
        }
        SettingsItemCard(position = CardPosition.Middle) {
            SwitchPreferenceRow(
                title = stringResource(Res.string.show_full_description),
                checked = state.showFullDescription,
                enabled = state.showDescription,
                onCheckedChange = onShowFullDescriptionChange,
            )
        }
        SettingsItemCard(position = CardPosition.Last) {
            // Always on on desktop: the markdown renderer auto-linkifies
            // bare URLs with no way to switch it off, so the row is
            // shown on but locked, for parity with the native pane.
            SwitchPreferenceRow(
                title = stringResource(Res.string.linkify),
                summary = stringResource(Res.string.linkify_description),
                checked = true,
                onCheckedChange = {},
                enabled = false,
            )
        }
    }
}

@Composable
private fun PerListSortCard(
    state: State,
    onPerListSortChange: (Boolean) -> Unit,
) {
    SettingsItemCard(position = CardPosition.Only) {
        SwitchPreferenceRow(
            title = stringResource(Res.string.per_list_sort_title),
            summary = stringResource(Res.string.per_list_sort_summary),
            checked = state.perListSort,
            onCheckedChange = onPerListSortChange,
        )
    }
}

@Composable
private fun ChipsCard(
    state: State,
    viewModel: TaskListPreferencesViewModel,
) {
    var showChipAppearanceDialog by remember { mutableStateOf(false) }
    SectionHeader(
        title = stringResource(Res.string.chips),
        modifier = Modifier.padding(vertical = SettingsSectionGap),
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
    ) {
        SettingsItemCard(position = CardPosition.First) {
            PreferenceRow(
                title = stringResource(Res.string.chip_appearance),
                summary = when (state.chipAppearance) {
                    TaskListPreferencesViewModel.CHIP_APPEARANCE_TEXT_ONLY -> stringResource(Res.string.chip_appearance_text_only)
                    TaskListPreferencesViewModel.CHIP_APPEARANCE_ICON_ONLY -> stringResource(Res.string.chip_appearance_icon_only)
                    else -> stringResource(Res.string.chip_appearance_text_and_icon)
                },
                showChevron = true,
                onClick = { showChipAppearanceDialog = true },
            )
        }
        ChipToggleRow(
            title = stringResource(Res.string.subtasks),
            checked = state.subtaskChips,
        ) { viewModel.updateChipVisibility(ChipOption.SUBTASKS, it) }
        ChipToggleRow(
            title = stringResource(Res.string.start_date),
            checked = state.startDateChips,
        ) { viewModel.updateChipVisibility(ChipOption.START_DATE, it) }
        ChipToggleRow(
            title = stringResource(Res.string.places),
            checked = state.placeChips,
        ) { viewModel.updateChipVisibility(ChipOption.PLACES, it) }
        ChipToggleRow(
            title = stringResource(Res.string.lists),
            checked = state.listChips,
        ) { viewModel.updateChipVisibility(ChipOption.LISTS, it) }
        ChipToggleRow(
            title = stringResource(Res.string.tags),
            checked = state.tagChips,
            position = CardPosition.Last,
        ) { viewModel.updateChipVisibility(ChipOption.TAGS, it) }
    }
    if (showChipAppearanceDialog) {
        ChipAppearanceDialog(
            selected = state.chipAppearance,
            onSelect = { choice ->
                viewModel.updateChipAppearance(choice)
                showChipAppearanceDialog = false
            },
            onDismiss = { showChipAppearanceDialog = false },
        )
    }
}

@Composable
private fun ChipToggleRow(
    title: String,
    checked: Boolean,
    position: CardPosition = CardPosition.Middle,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsItemCard(position = position) {
        SwitchPreferenceRow(
            title = title,
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
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
    // Mirrors the native Android slider row: title + current value above a
    // full-width M3 slider.
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
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = max - min - 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipAppearanceDialog(
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        TaskListPreferencesViewModel.CHIP_APPEARANCE_TEXT_AND_ICON to stringResource(Res.string.chip_appearance_text_and_icon),
        TaskListPreferencesViewModel.CHIP_APPEARANCE_TEXT_ONLY to stringResource(Res.string.chip_appearance_text_only),
        TaskListPreferencesViewModel.CHIP_APPEARANCE_ICON_ONLY to stringResource(Res.string.chip_appearance_icon_only),
    )
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.chip_appearance),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == value,
                            onClick = { onSelect(value) },
                        )
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
