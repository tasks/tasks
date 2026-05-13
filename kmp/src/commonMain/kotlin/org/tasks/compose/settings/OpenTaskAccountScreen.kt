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
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.PlatformBackHandler
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.discard
import tasks.kmp.generated.resources.discard_changes
import tasks.kmp.generated.resources.display_name
import tasks.kmp.generated.resources.save

@Composable
fun OpenTaskAccountScreen(
    displayName: String,
    nameError: String?,
    serverType: Int,
    hasChanges: Boolean,
    showDiscardDialog: Boolean,
    accountError: String?,
    onNameChange: (String) -> Unit,
    onServerTypeChange: (Int) -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit,
    onDiscardDialogChange: (Boolean) -> Unit,
) {
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

        accountError?.takeIf { it.isNotBlank() }?.let { error ->
            AccountErrorBanner(
                error = error,
                modifier = Modifier.padding(horizontal = SettingsContentPadding),
            )
            Spacer(modifier = Modifier.height(SettingsContentPadding))
        }

        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        ) {
            TextInputCard(
                value = displayName,
                onValueChange = onNameChange,
                label = stringResource(Res.string.display_name),
                error = nameError,
            )
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        ) {
            ServerTypeSelector(
                selected = serverType,
                onSelected = onServerTypeChange,
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
