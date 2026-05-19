package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.tasks.viewmodel.GoogleTaskListSettingsViewModel

@Composable
fun GoogleTaskListSettingsScreen(
    viewModel: GoogleTaskListSettingsViewModel,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onNavigateBack: () -> Unit,
    onSelectColor: (PickerColor) -> Unit,
    onColorWheelSelected: () -> Unit,
    onSubscribe: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    CaldavCalendarSettingsScreen(
        state = state,
        onNameChange = viewModel::setName,
        onSave = onSave,
        onDelete = onDelete,
        onNavigateBack = onNavigateBack,
        onDiscardDialogChange = { show ->
            if (show) viewModel.showDiscardDialog() else viewModel.dismissDiscardDialog()
        },
        onDismissSnackbar = viewModel::dismissSnackbar,
        onOpenShareDialog = {},
        onCloseShareDialog = {},
        onShare = {},
        onConfirmRemovePrincipal = {},
        onRemovePrincipal = {},
        onOpenColorPicker = viewModel::openColorPicker,
        onCloseColorPicker = viewModel::closeColorPicker,
        onSelectColor = onSelectColor,
        onColorWheelSelected = onColorWheelSelected,
        onOpenIconPicker = viewModel::openIconPicker,
        onCloseIconPicker = viewModel::closeIconPicker,
        onSelectIcon = viewModel::selectIcon,
        onSubscribe = onSubscribe,
    )
}
