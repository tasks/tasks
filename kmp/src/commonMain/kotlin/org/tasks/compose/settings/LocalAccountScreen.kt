package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.PlatformBackHandler
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.delete
import tasks.kmp.generated.resources.delete_tag_confirmation
import tasks.kmp.generated.resources.delete_tasks_warning
import tasks.kmp.generated.resources.discard
import tasks.kmp.generated.resources.discard_changes
import tasks.kmp.generated.resources.display_name
import tasks.kmp.generated.resources.local_lists
import tasks.kmp.generated.resources.logout_warning
import tasks.kmp.generated.resources.save
import tasks.kmp.generated.resources.task_count

@Composable
fun LocalAccountScreen(
    displayName: String,
    nameError: String?,
    taskCount: Int,
    accountName: String,
    hasChanges: Boolean,
    showDiscardDialog: Boolean,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onNavigateBack: () -> Unit,
    onDiscardDialogChange: (Boolean) -> Unit,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    PlatformBackHandler(enabled = hasChanges) {
        onDiscardDialogChange(true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        ) {
            TextInputCard(
                value = displayName,
                onValueChange = onNameChange,
                label = stringResource(Res.string.display_name),
                placeholder = stringResource(Res.string.local_lists),
                error = nameError,
            )
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        ) {
            SettingsItemCard {
                PreferenceRow(
                    title = stringResource(Res.string.save),
                    icon = Icons.Outlined.Save,
                    enabled = hasChanges,
                    onClick = onSave,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        ) {
            DangerCard(
                icon = Icons.Outlined.DeleteOutline,
                title = stringResource(Res.string.delete),
                tint = MaterialTheme.colorScheme.error,
                onClick = { showDeleteDialog = true },
            )
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
    }

    if (showDeleteDialog) {
        val taskCountString = pluralStringResource(Res.plurals.task_count, taskCount, taskCount)
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(Res.string.delete_tag_confirmation, accountName),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = if (taskCount > 0) {
                        stringResource(Res.string.delete_tasks_warning, taskCountString)
                    } else {
                        stringResource(Res.string.logout_warning)
                    },
                )
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

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { onDiscardDialogChange(false) },
            title = {
                Text(
                    text = stringResource(Res.string.discard_changes),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDiscardDialogChange(false)
                    onNavigateBack()
                }) {
                    Text(text = stringResource(Res.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDiscardDialogChange(false) }) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
    }
}
