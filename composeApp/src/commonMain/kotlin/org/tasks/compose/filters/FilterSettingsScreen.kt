package org.tasks.compose.filters

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.components.SymbolIcon
import org.tasks.compose.pickers.IconPicker
import org.tasks.compose.pickers.IconPickerViewModel
import org.tasks.compose.settings.CardPosition
import org.tasks.compose.settings.ColorIconCards
import org.tasks.compose.settings.ColorPickerDialog
import org.tasks.compose.settings.DangerCard
import org.tasks.compose.settings.ConfirmDialog
import org.tasks.compose.settings.PreferenceRow
import org.tasks.compose.settings.SectionHeader
import org.tasks.compose.settings.SettingsCardGap
import org.tasks.compose.settings.SettingsContentPadding
import org.tasks.compose.settings.SettingsItemCard
import org.tasks.compose.settings.TextInputCard
import org.tasks.data.entity.Filter
import org.tasks.filters.BooleanCriterion
import org.tasks.filters.CriterionInstance
import org.tasks.filters.FilterSettingsViewModel
import org.tasks.filters.MultipleSelectCriterion
import org.tasks.filters.TextInputCriterion
import org.tasks.themes.TasksIcons
import sh.calvin.reorderable.ReorderableColumn
import tasks.kmp.generated.resources.CFA_button_add
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.custom_filter_and
import tasks.kmp.generated.resources.custom_filter_criteria
import tasks.kmp.generated.resources.custom_filter_not
import tasks.kmp.generated.resources.custom_filter_or
import tasks.kmp.generated.resources.delete
import tasks.kmp.generated.resources.name
import tasks.kmp.generated.resources.name_cannot_be_empty
import tasks.kmp.generated.resources.new_filter
import tasks.kmp.generated.resources.ok
import tasks.kmp.generated.resources.save

/**
 * Full-screen desktop filter editor — port of the Android
 * `FilterSettingsActivity` content + `FilterCondition` composables. Created (new or
 * pre-filled from a template) and edited in one surface: title, colour + icon, the
 * criterion rows (universe locked first; each row a draggable AND/OR/NOT entry with a
 * live task-count), an "add criterion" affordance, and save/delete.
 *
 * The view-model is passed in (Koin-bound); save/delete/back are reported to the caller
 * via callbacks, which wires the drawer refresh and (for new filters) selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
// A full-screen settings surface that also hosts every dialog; splitting it would
// mean passing the whole view model plus a dozen callbacks around, so the
// length/parameter findings are suppressed rather than refactored for the linter.
@Suppress("LongMethod", "LongParameterList")
@Composable
fun FilterSettingsScreen(
    viewModel: FilterSettingsViewModel,
    onSave: (Filter) -> Unit,
    onDelete: (Filter) -> Unit,
    onNavigateBack: () -> Unit,
    onSubscribe: (String) -> Unit = {},
    onHelp: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var openTypeRow by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val emptyNameError = stringResource(Res.string.name_cannot_be_empty)
    val iconPickerViewModel = remember { IconPickerViewModel() }
    val iconState by iconPickerViewModel.viewState.collectAsState()
    val iconSearch by iconPickerViewModel.searchResults.collectAsState()

    val save: () -> Unit = {
        scope.launch {
            try {
                onSave(viewModel.save())
            } catch (e: IllegalArgumentException) {
                viewModel.setError(emptyNameError)
            }
        }
    }

    // Width/centering come from the hosting BasicAlertDialog (the common M3 dialog min/max
    // width convention, same as TagSettingsDialog); this screen just paints the surface
    // (light grey) so the white cards float on it.
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsContentPadding, vertical = 8.dp),
        ) {
            IconButton(onClick = onNavigateBack) {
                SymbolIcon(name = TasksIcons.ARROW_BACK, contentDescription = stringResource(Res.string.cancel))
            }
            Text(
                text = if (viewModel.isNew) stringResource(Res.string.new_filter) else state.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            if (viewModel.isNew) {
                IconButton(onClick = onHelp) {
                    SymbolIcon(name = TasksIcons.HELP, contentDescription = stringResource(Res.string.name))
                }
            }
        }
        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Column(
                Modifier
                    .padding(top = 8.dp)
                    .padding(horizontal = SettingsContentPadding),
            ) {
                TextInputCard(
                    value = state.title,
                    onValueChange = viewModel::setTitle,
                    label = stringResource(Res.string.name),
                    error = state.error,
                    position = CardPosition.Only,
                )
            }

            Spacer(modifier = Modifier.height(SettingsContentPadding))

            ColorIconCards(
                color = state.color,
                icon = state.icon,
                pickerColors = state.pickerColors,
                onColorClick = viewModel::openColorPicker,
                onClearColor = { viewModel.setColor(0) },
                onIconClick = viewModel::openIconPicker,
            )

            Spacer(modifier = Modifier.height(SettingsContentPadding))

            SectionHeader(
                title = stringResource(Res.string.custom_filter_criteria),
                modifier = Modifier.padding(horizontal = 25.dp),
            )

            Column(Modifier.padding(horizontal = SettingsContentPadding)) {
                CriteriaList(
                    criteria = state.criteria,
                    onRowClick = { index -> if (index > 0) openTypeRow = index },
                    onMove = viewModel::move,
                )
            }

            Spacer(modifier = Modifier.height(SettingsCardGap))

            Column(Modifier.padding(horizontal = SettingsContentPadding)) {
                SettingsItemCard {
                    PreferenceRow(
                        title = stringResource(Res.string.CFA_button_add),
                        icon = TasksIcons.ADD,
                        onClick = viewModel::openAddCriterion,
                    )
                }
            }

            Spacer(modifier = Modifier.height(SettingsContentPadding))

            Column(Modifier.padding(horizontal = SettingsContentPadding)) {
                SettingsItemCard {
                    PreferenceRow(
                        title = stringResource(Res.string.save),
                        icon = TasksIcons.SAVE,
                        enabled = viewModel.hasChanges(),
                        onClick = save,
                    )
                }
            }

            if (!viewModel.isNew) {
                Spacer(modifier = Modifier.height(SettingsContentPadding))
                Column(Modifier.padding(horizontal = SettingsContentPadding)) {
                    DangerCard(
                        icon = TasksIcons.DELETE,
                        title = stringResource(Res.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { showDeleteConfirm = true },
                    )
                }
            }
        }
    }

    // AND/OR/NOT for a clicked row
    openTypeRow?.let { index ->
        if (index in state.criteria.indices) {
            val instance = state.criteria[index]
            SelectCriterionType(
                title = instance.titleFromCriterion,
                selected = when (instance.type) {
                    CriterionInstance.TYPE_INTERSECT -> 0
                    CriterionInstance.TYPE_ADD -> 1
                    else -> 2
                },
                types = listOf(
                    stringResource(Res.string.custom_filter_and),
                    stringResource(Res.string.custom_filter_or),
                    stringResource(Res.string.custom_filter_not),
                ),
                onCancel = { openTypeRow = null },
            ) { selected ->
                val type = when (selected) {
                    0 -> CriterionInstance.TYPE_INTERSECT
                    1 -> CriterionInstance.TYPE_ADD
                    else -> CriterionInstance.TYPE_SUBTRACT
                }
                if (type != instance.type) {
                    val updated = CriterionInstance(instance).apply { this.type = type }
                    viewModel.setCriterion(index, updated)
                }
                openTypeRow = null
            }
        }
    }

    // Pick a criterion type to add
    if (state.showTypePicker) {
        SelectFromList(
            names = state.newCriterionTypes.map { it.name.orEmpty() },
            title = stringResource(Res.string.CFA_button_add),
            onCancel = viewModel::cancelTypePicker,
        ) { which -> viewModel.onTypePicked(which) }
    }

    // Value picker for a pending criterion
    state.newCriterionOptions?.let { instance ->
        when (val c = instance.criterion) {
            is MultipleSelectCriterion -> {
                val titles = c.entryTitles?.map { it.orEmpty() } ?: emptyList()
                SelectFromList(
                    names = titles,
                    title = c.name.orEmpty(),
                    onCancel = viewModel::cancelValuePicker,
                ) { which -> viewModel.onValuePicked(which) }
            }

            is TextInputCriterion -> {
                InputTextOption(
                    title = c.prompt,
                    onCancel = viewModel::cancelValuePicker,
                ) { text -> viewModel.onTextDone(text) }
            }

            else -> Unit
        }
    }

    if (state.showColorPicker) {
        ColorPickerDialog(
            hasPro = state.hasPro,
            colors = state.pickerColors,
            onDismiss = viewModel::closeColorPicker,
            onColorSelected = viewModel::selectColor,
            onSubscribe = { onSubscribe("filter_colors") },
        )
    }

    if (state.showIconPicker) {
        BasicAlertDialog(
            onDismissRequest = viewModel::closeIconPicker,
            modifier = Modifier.padding(vertical = 32.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.background,
            ) {
                IconPicker(
                    icons = iconState.icons,
                    searchResults = iconSearch,
                    collapsed = iconState.collapsed,
                    query = iconState.query,
                    onQueryChange = iconPickerViewModel::onQueryChange,
                    onSelected = { viewModel.selectIcon(it.name) },
                    toggleCollapsed = iconPickerViewModel::setCollapsed,
                    hasPro = state.hasPro,
                    subscribe = { onSubscribe("icons") },
                )
            }
        }
    }

    if (showDeleteConfirm) {
        val filter = viewModel.filter
        ConfirmDialog(
            text = stringResource(Res.string.delete) + " " + (filter?.title.orEmpty()),
            confirmText = stringResource(Res.string.delete),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = {
                showDeleteConfirm = false
                if (filter != null) {
                    // The view model performs the DB delete (same as the tag settings
                    // template); the callback only closes the dialog afterwards.
                    scope.launch { viewModel.delete { onDelete(filter) } }
                }
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun CriteriaList(
    criteria: List<CriterionInstance>,
    onRowClick: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
) {
    // The draggable list of criterion rows. The first row (the universe) is locked;
    // the rest can be reordered by dragging and opened (AND/OR/NOT) by clicking.
    ReorderableColumn(
        list = criteria,
        onSettle = { from, to -> onMove(from, to) },
    ) { index, item, isDragging ->
        ReorderableItem() {
            CriterionRow(
                instance = item,
                isUniverse = index == 0,
                dragging = isDragging,
                handleModifier = Modifier.draggableHandle(enabled = index != 0),
                onClick = { onRowClick(index) },
            )
        }
    }
}

@Composable
private fun CriterionRow(
    instance: CriterionInstance,
    isUniverse: Boolean,
    dragging: Boolean,
    handleModifier: Modifier,
    onClick: () -> Unit,
) {
    SettingsItemCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(handleModifier)
                .clickable(enabled = !isUniverse, onClick = onClick)
                .padding(horizontal = SettingsContentPadding, vertical = 12.dp)
                .let {
                    if (dragging) it.background(Color.LightGray.copy(alpha = 0.4f)) else it
                },
        ) {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                if (isUniverse) {
                    // locked universe row marker
                    Text(
                        text = "≡",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = CombinatorBadgeGlyph(instance.type),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (instance.type == CriterionInstance.TYPE_ADD) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Text(
                text = instance.titleFromCriterion,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            if (instance.max > 0) {
                Text(
                    text = instance.end.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun CombinatorBadgeGlyph(type: Int): String = when (type) {
    CriterionInstance.TYPE_ADD -> "∨"
    CriterionInstance.TYPE_SUBTRACT -> "¬"
    else -> "∧"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectCriterionType(
    title: String,
    selected: Int,
    types: List<String>,
    onCancel: () -> Unit,
    onSelected: (Int) -> Unit,
) {
    val selected = remember { mutableIntStateOf(selected) }
    BasicAlertDialog(onDismissRequest = onCancel) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    types.forEachIndexed { index, label ->
                        val highlight = index == selected.intValue
                        TextButton(
                            onClick = { selected.intValue = index },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (highlight) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                contentColor = if (highlight) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            ),
                        ) {
                            Text(label)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) }
                    TextButton(onClick = { onSelected(selected.intValue) }) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectFromList(
    names: List<String>,
    title: String,
    onCancel: () -> Unit,
    onSelected: (Int) -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onCancel) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                names.forEachIndexed { index, label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(index) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputTextOption(
    title: String,
    onCancel: () -> Unit,
    onDone: (String) -> Unit,
) {
    val text = remember { mutableStateOf("") }
    BasicAlertDialog(onDismissRequest = onCancel) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text.value,
                    onValueChange = { text.value = it },
                    label = { Text(title) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    leadingIcon = {
                        SymbolIcon(name = TasksIcons.ABC, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) }
                    TextButton(onClick = { onDone(text.value) }) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            }
        }
    }
}
