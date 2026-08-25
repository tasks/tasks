package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PermIdentity
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.ai_api_key
import tasks.kmp.generated.resources.ai_disclosure_body
import tasks.kmp.generated.resources.ai_model
import tasks.kmp.generated.resources.ai_model_none
import tasks.kmp.generated.resources.ai_privacy_policy
import tasks.kmp.generated.resources.ai_task_creation_enable
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.save

/**
 * One model the user may pick, flattened for the UI so this screen stays free of transport DTOs
 * (which live in `jvmCommonMain` and are not visible from `commonMain`).
 */
data class AiModelOption(
    val id: String,
    val label: String,
)

data class AiTaskCreationState(
    val enabled: Boolean = false,
    val keyInput: String = "",
    val hasStoredKey: Boolean = false,
    val keyError: StringResource? = null,
    val models: List<AiModelOption> = emptyList(),
    val selectedModel: String? = null,
    val loadingModels: Boolean = false,
)

@Composable
fun AiTaskCreationScreen(
    state: AiTaskCreationState,
    onEnabledChange: (Boolean) -> Unit,
    onKeyChange: (String) -> Unit,
    onKeySubmit: () -> Unit,
    onModelSelected: (String) -> Unit,
    onPrivacyPolicy: () -> Unit,
) {
    var showModelPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        if (state.loadingModels) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Column(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            SettingsItemCard {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.ai_task_creation_enable),
                    checked = state.enabled,
                    onCheckedChange = onEnabledChange,
                    icon = Icons.Outlined.AutoAwesome,
                    indent = false,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            TextInputCard(
                value = state.keyInput,
                onValueChange = onKeyChange,
                label = stringResource(Res.string.ai_api_key),
                placeholder = if (state.hasStoredKey) "••••••••" else null,
                error = state.keyError?.let { stringResource(it) },
                position = CardPosition.First,
                contentType = ContentType.Password,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    capitalization = KeyboardCapitalization.None,
                ),
                visualTransformation = PasswordVisualTransformation(),
            )
            SettingsItemCard(position = CardPosition.Middle) {
                PreferenceRow(
                    title = stringResource(Res.string.save),
                    icon = Icons.Outlined.Save,
                    enabled = state.keyInput.isNotBlank() && !state.loadingModels,
                    onClick = onKeySubmit,
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                PreferenceRow(
                    title = stringResource(Res.string.ai_model),
                    summary = state.selectedModel ?: stringResource(Res.string.ai_model_none),
                    enabled = state.models.isNotEmpty(),
                    showChevron = state.models.isNotEmpty(),
                    onClick = { showModelPicker = true },
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Text(
            text = stringResource(Res.string.ai_disclosure_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Column(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            SettingsItemCard {
                PreferenceRow(
                    title = stringResource(Res.string.ai_privacy_policy),
                    icon = Icons.Outlined.PermIdentity,
                    onClick = onPrivacyPolicy,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
    }

    if (showModelPicker && state.models.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showModelPicker = false },
            title = { Text(stringResource(Res.string.ai_model)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(state.models, key = { it.id }) { model ->
                        val select = {
                            onModelSelected(model.id)
                            showModelPicker = false
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = select)
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = model.id == state.selectedModel,
                                onClick = select,
                            )
                            Column {
                                Text(
                                    text = model.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = model.id,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelPicker = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}
