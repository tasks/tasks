package org.tasks.compose.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.tasks.viewmodel.ListSettingsCallbacks

@Composable
fun ListSettingsScreen(
    viewModel: ListSettingsCallbacks,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onNavigateBack: () -> Unit,
    onSelectColor: (PickerColor) -> Unit,
    onColorWheelSelected: () -> Unit,
    onSubscribe: () -> Unit,
    headerContent: @Composable () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    ListSettingsScreen(
        state = state,
        onNameChange = viewModel::setName,
        onSave = onSave,
        onDelete = onDelete,
        onNavigateBack = onNavigateBack,
        onDiscardDialogChange = { show ->
            if (show) viewModel.showDiscardDialog() else viewModel.dismissDiscardDialog()
        },
        onDismissSnackbar = viewModel::dismissSnackbar,
        onOpenShareDialog = viewModel::openShareDialog,
        onCloseShareDialog = viewModel::closeShareDialog,
        onShare = { input -> viewModel.share(input) },
        onConfirmRemovePrincipal = viewModel::confirmRemovePrincipal,
        onRemovePrincipal = viewModel::removePrincipal,
        onOpenColorPicker = viewModel::openColorPicker,
        onCloseColorPicker = viewModel::closeColorPicker,
        onSelectColor = onSelectColor,
        onColorWheelSelected = onColorWheelSelected,
        onOpenIconPicker = viewModel::openIconPicker,
        onCloseIconPicker = viewModel::closeIconPicker,
        onSelectIcon = viewModel::selectIcon,
        onSubscribe = onSubscribe,
        headerContent = headerContent,
    )
}
