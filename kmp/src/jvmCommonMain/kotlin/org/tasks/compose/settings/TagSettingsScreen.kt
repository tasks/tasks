package org.tasks.compose.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.PlatformBackHandler
import org.tasks.compose.pickers.IconPicker
import org.tasks.compose.pickers.IconPickerViewModel
import org.tasks.viewmodel.TagSettingsViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.color
import tasks.kmp.generated.resources.delete
import tasks.kmp.generated.resources.delete_tag_confirmation
import tasks.kmp.generated.resources.delete_tag_warning
import tasks.kmp.generated.resources.discard
import tasks.kmp.generated.resources.discard_changes
import tasks.kmp.generated.resources.display_name
import tasks.kmp.generated.resources.icon
import tasks.kmp.generated.resources.new_tag
import tasks.kmp.generated.resources.save

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagSettingsScreen(
    viewModel: TagSettingsViewModel,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onNavigateBack: () -> Unit,
    onSubscribe: (String) -> Unit,
    onColorWheelSelected: () -> Unit = {},
    onAddShortcut: (() -> Unit)? = null,
    onAddWidget: (() -> Unit)? = null,
) {
    val state by viewModel.viewState.collectAsState()
    val isNew = viewModel.isNew
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    val navigateBackOrPrompt: () -> Unit = {
        if (viewModel.hasChanges) viewModel.showDiscardDialog() else onNavigateBack()
    }

    PlatformBackHandler(enabled = true, onBack = navigateBackOrPrompt)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
            .union(WindowInsets.systemBars)
            .union(WindowInsets.ime),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = navigateBackOrPrompt) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(SettingsContentPadding))

            Column(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
                TextInputCard(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = stringResource(Res.string.display_name),
                    placeholder = if (isNew) stringResource(Res.string.new_tag) else null,
                    error = state.nameError,
                )
            }

            Spacer(modifier = Modifier.height(SettingsContentPadding))

            ColorIconCards(
                color = state.color,
                icon = state.icon,
                pickerColors = state.pickerColors,
                onColorClick = viewModel::openColorPicker,
                onClearColor = { viewModel.selectColor(0) },
                onIconClick = viewModel::openIconPicker,
            )

            ShortcutWidgetCards(
                onAddShortcut = onAddShortcut,
                onAddWidget = onAddWidget,
                enabled = state.name.isNotBlank() && !state.isLoading,
            )

            Spacer(modifier = Modifier.height(SettingsContentPadding))

            Column(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
                SettingsItemCard {
                    PreferenceRow(
                        title = stringResource(Res.string.save),
                        icon = Icons.Outlined.Save,
                        enabled = state.name.isNotBlank() && viewModel.hasChanges && !state.isLoading,
                        onClick = onSave,
                    )
                }
            }

            if (!isNew) {
                Spacer(modifier = Modifier.height(SettingsContentPadding))

                Column(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
                    DangerCard(
                        icon = Icons.Outlined.DeleteOutline,
                        title = stringResource(Res.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { showDeleteDialog = true },
                    )
                }
            }

            Spacer(modifier = Modifier.height(SettingsContentPadding))
        }
    }

    if (state.showColorPicker) {
        ColorPickerDialog(
            hasPro = state.hasPro,
            colors = state.pickerColors,
            onDismiss = viewModel::closeColorPicker,
            onColorSelected = { viewModel.selectColor(it.originalColor) },
            onSubscribe = { onSubscribe("list_colors") },
            onColorWheelSelected = onColorWheelSelected,
            showColorWheel = state.hasColorWheel,
        )
    }

    if (state.showIconPicker) {
        val iconPickerViewModel = remember { IconPickerViewModel() }
        val iconState = iconPickerViewModel.viewState.collectAsState().value
        val searchResults = iconPickerViewModel.searchResults.collectAsState().value
        BasicAlertDialog(
            onDismissRequest = viewModel::closeIconPicker,
            modifier = Modifier.padding(vertical = 32.dp),
        ) {
            IconPicker(
                icons = iconState.icons,
                query = iconState.query,
                searchResults = searchResults,
                collapsed = iconState.collapsed,
                onQueryChange = iconPickerViewModel::onQueryChange,
                onSelected = { viewModel.selectIcon(it.name) },
                toggleCollapsed = iconPickerViewModel::setCollapsed,
                hasPro = state.hasPro,
                subscribe = { onSubscribe("icons") },
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(Res.string.delete_tag_confirmation, state.name),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(text = stringResource(Res.string.delete_tag_warning))
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(text = stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
    }

    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDiscardDialog,
            title = {
                Text(
                    text = stringResource(Res.string.discard_changes),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissDiscardDialog()
                    onNavigateBack()
                }) {
                    Text(text = stringResource(Res.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDiscardDialog) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
    }
}
