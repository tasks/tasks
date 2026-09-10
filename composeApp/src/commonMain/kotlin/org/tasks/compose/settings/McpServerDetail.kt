package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.tasks.mcp.AccessMode
import org.tasks.mcp.McpClientSnippet
import org.tasks.mcp.McpTool
import org.tasks.mcp.McpToolCatalog
import org.tasks.mcp.McpToolGroup
import org.tasks.mcp.McpServerController
import org.tasks.mcp.McpServerState
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.mcp_access
import tasks.kmp.generated.resources.mcp_access_advanced
import tasks.kmp.generated.resources.mcp_access_advanced_summary
import tasks.kmp.generated.resources.mcp_access_read_only
import tasks.kmp.generated.resources.mcp_access_read_only_summary
import tasks.kmp.generated.resources.mcp_access_read_write
import tasks.kmp.generated.resources.mcp_access_read_write_summary
import tasks.kmp.generated.resources.mcp_auth_header
import tasks.kmp.generated.resources.mcp_tools
import tasks.kmp.generated.resources.mcp_tools_enabled
import tasks.kmp.generated.resources.mcp_tools_group_accounts
import tasks.kmp.generated.resources.mcp_tools_group_lists
import tasks.kmp.generated.resources.mcp_tools_group_places
import tasks.kmp.generated.resources.mcp_tools_group_reminders
import tasks.kmp.generated.resources.mcp_tools_group_tags
import tasks.kmp.generated.resources.mcp_tools_group_tasks
import tasks.kmp.generated.resources.mcp_tools_summary
import tasks.kmp.generated.resources.mcp_clipboard_note
import tasks.kmp.generated.resources.mcp_connect
import tasks.kmp.generated.resources.mcp_connect_summary
import tasks.kmp.generated.resources.mcp_copied
import tasks.kmp.generated.resources.mcp_copy
import tasks.kmp.generated.resources.mcp_endpoint
import tasks.kmp.generated.resources.mcp_hide
import tasks.kmp.generated.resources.mcp_manual
import tasks.kmp.generated.resources.mcp_manual_summary
import tasks.kmp.generated.resources.mcp_regenerate_token
import tasks.kmp.generated.resources.mcp_reveal
import tasks.kmp.generated.resources.mcp_regenerate_token_summary
import tasks.kmp.generated.resources.mcp_server
import tasks.kmp.generated.resources.mcp_server_enable
import tasks.kmp.generated.resources.mcp_server_enable_summary
import tasks.kmp.generated.resources.mcp_status_running
import tasks.kmp.generated.resources.mcp_status_starting
import tasks.kmp.generated.resources.mcp_status_stopped
import tasks.kmp.generated.resources.ok

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpServerDetail(
    onNavigateBack: () -> Unit,
) {
    val controller = koinInject<McpServerController>()
    val state by controller.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.mcp_server)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        McpServerContent(
            state = state,
            onEnabledChange = controller::setEnabled,
            onAccessModeChange = controller::setAccessMode,
            onToolsEnabledChange = controller::setToolsEnabled,
            onRegenerateToken = controller::regenerateToken,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

@Composable
fun McpServerContent(
    state: McpServerState,
    onEnabledChange: (Boolean) -> Unit,
    onAccessModeChange: (AccessMode) -> Unit,
    onToolsEnabledChange: (Set<String>, Boolean) -> Unit,
    onRegenerateToken: () -> Unit,
    modifier: Modifier = Modifier,
) {
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    var copiedId by remember { mutableStateOf<String?>(null) }
    var confirmRegenerate by remember { mutableStateOf(false) }

    var clipboardHolds by remember { mutableStateOf<String?>(null) }

    val copy: (String, String) -> Unit = { id, text ->
        clipboard.setText(AnnotatedString(text))
        copiedId = id
        clipboardHolds = text
    }

    if (confirmRegenerate) {
        ConfirmDialog(
            text = stringResource(Res.string.mcp_regenerate_token_summary),
            confirmText = stringResource(Res.string.ok),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = {
                confirmRegenerate = false
                onRegenerateToken()
            },
            onDismiss = { confirmRegenerate = false },
        )
    }

    LaunchedEffect(copiedId) {
        if (copiedId != null) {
            delay(COPIED_FEEDBACK_MS)
            copiedId = null
        }
    }

    LaunchedEffect(clipboardHolds) {
        val copied = clipboardHolds ?: return@LaunchedEffect
        delay(CLIPBOARD_LIFETIME_MS)
        if (clipboard.getText()?.text == copied) {
            clipboard.setText(AnnotatedString(""))
        }
        clipboardHolds = null
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Column(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            SettingsItemCard(position = CardPosition.Only) {
                SwitchPreferenceRow(
                    title = stringResource(Res.string.mcp_server_enable),
                    summary = state.error
                        ?: when {
                            !state.enabled -> stringResource(Res.string.mcp_server_enable_summary)
                            state.isRunning ->
                                stringResource(Res.string.mcp_status_running, state.port)
                            state.status == McpServerState.Status.Starting ->
                                stringResource(Res.string.mcp_status_starting)
                            else -> stringResource(Res.string.mcp_status_stopped)
                        },
                    checked = state.enabled,
                    onCheckedChange = onEnabledChange,
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        SectionHeader(
            title = stringResource(Res.string.mcp_access),
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                AccessRow(
                    title = stringResource(Res.string.mcp_access_read_only),
                    summary = stringResource(Res.string.mcp_access_read_only_summary),
                    selected = state.mode == AccessMode.ReadOnly,
                    onClick = { onAccessModeChange(AccessMode.ReadOnly) },
                )
            }
            SettingsItemCard(position = CardPosition.Middle) {
                AccessRow(
                    title = stringResource(Res.string.mcp_access_read_write),
                    summary = stringResource(Res.string.mcp_access_read_write_summary),
                    selected = state.mode == AccessMode.ReadWrite,
                    onClick = { onAccessModeChange(AccessMode.ReadWrite) },
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                AccessRow(
                    title = stringResource(Res.string.mcp_access_advanced),
                    summary = stringResource(Res.string.mcp_access_advanced_summary),
                    selected = state.mode == AccessMode.Advanced,
                    onClick = { onAccessModeChange(AccessMode.Advanced) },
                )
            }
        }

        if (state.mode == AccessMode.Advanced) {
            ToolSection(
                enabled = state.enabledTools,
                onToolsEnabledChange = onToolsEnabledChange,
            )
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        SectionHeader(
            title = stringResource(Res.string.mcp_connect),
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Caption(stringResource(Res.string.mcp_connect_summary))
        Caption(stringResource(Res.string.mcp_clipboard_note))
        Spacer(modifier = Modifier.height(SettingsSectionGap))
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            state.clients.forEachIndexed { index, client ->
                SettingsItemCard(
                    position = CardPosition.forIndex(index, state.clients.size),
                ) {
                    ClientCard(
                        client = client,
                        copied = copiedId == client.id,
                        onCopy = { copy(client.id, client.snippet) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        SectionHeader(
            title = stringResource(Res.string.mcp_manual),
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
        )
        Caption(stringResource(Res.string.mcp_manual_summary))
        Spacer(modifier = Modifier.height(SettingsSectionGap))
        Column(
            modifier = Modifier.padding(horizontal = SettingsContentPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                FieldCard(
                    label = stringResource(Res.string.mcp_endpoint),
                    value = state.endpoint,
                    copied = copiedId == ENDPOINT_ID,
                    onCopy = { copy(ENDPOINT_ID, state.endpoint) },
                )
            }
            SettingsItemCard(position = CardPosition.Last) {
                FieldCard(
                    label = stringResource(Res.string.mcp_auth_header),
                    value = state.authorizationHeader,
                    masked = state.maskedAuthorizationHeader,
                    copied = copiedId == HEADER_ID,
                    onCopy = { copy(HEADER_ID, state.authorizationHeader) },
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))

        Column(modifier = Modifier.padding(horizontal = SettingsContentPadding)) {
            SettingsItemCard(position = CardPosition.Only) {
                PreferenceRow(
                    title = stringResource(Res.string.mcp_regenerate_token),
                    summary = stringResource(Res.string.mcp_regenerate_token_summary),
                    summaryMaxLines = Int.MAX_VALUE,
                    icon = Icons.Outlined.Refresh,
                    onClick = { confirmRegenerate = true },
                )
            }
        }

        Spacer(modifier = Modifier.height(SettingsContentPadding))
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = SettingsContentPadding),
    )
}

@Composable
private fun ToolSection(
    enabled: Set<String>,
    onToolsEnabledChange: (Set<String>, Boolean) -> Unit,
) {
    Spacer(modifier = Modifier.height(SettingsContentPadding))

    SectionHeader(
        title = stringResource(Res.string.mcp_tools),
        modifier = Modifier.padding(horizontal = SettingsContentPadding),
    )
    Caption(stringResource(Res.string.mcp_tools_summary))
    Caption(
        stringResource(Res.string.mcp_tools_enabled, enabled.size, McpToolCatalog.names.size)
    )
    Spacer(modifier = Modifier.height(SettingsSectionGap))

    McpToolCatalog.byGroup.forEach { (group, tools) ->
        val names = tools.map { it.name }.toSet()
        Column(
            modifier = Modifier.padding(
                start = SettingsContentPadding,
                end = SettingsContentPadding,
                bottom = SettingsCardGap * 4,
            ),
            verticalArrangement = Arrangement.spacedBy(SettingsCardGap),
        ) {
            SettingsItemCard(position = CardPosition.First) {
                val allOn = enabled.containsAll(names)
                PreferenceRow(
                    title = stringResource(group.label()),
                    indent = false,
                    onClick = { onToolsEnabledChange(names, !allOn) },
                    trailing = {
                        Switch(
                            checked = allOn,
                            onCheckedChange = { onToolsEnabledChange(names, it) },
                            modifier = Modifier.padding(end = SettingsContentPadding),
                        )
                    },
                )
            }
            tools.forEachIndexed { index, tool ->
                SettingsItemCard(
                    position = if (index == tools.lastIndex) CardPosition.Last
                    else CardPosition.Middle,
                ) {
                    ToolRow(
                        tool = tool,
                        checked = tool.name in enabled,
                        onCheckedChange = { onToolsEnabledChange(setOf(tool.name), it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolRow(
    tool: McpTool,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    PreferenceRow(
        title = tool.name,
        leading = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.padding(start = SettingsContentPadding),
            )
        },
        onClick = { onCheckedChange(!checked) },
    )
}

private fun McpToolGroup.label() = when (this) {
    McpToolGroup.Tasks -> Res.string.mcp_tools_group_tasks
    McpToolGroup.Lists -> Res.string.mcp_tools_group_lists
    McpToolGroup.Tags -> Res.string.mcp_tools_group_tags
    McpToolGroup.Places -> Res.string.mcp_tools_group_places
    McpToolGroup.Reminders -> Res.string.mcp_tools_group_reminders
    McpToolGroup.Accounts -> Res.string.mcp_tools_group_accounts
}

@Composable
private fun AccessRow(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    PreferenceRow(
        title = title,
        summary = summary,
        summaryMaxLines = Int.MAX_VALUE,
        leading = {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.padding(start = SettingsContentPadding),
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun ClientCard(
    client: McpClientSnippet,
    copied: Boolean,
    onCopy: () -> Unit,
) {
    Column {
        PreferenceRow(title = client.name, summary = client.target)
        CodeBlock(text = client.displaySnippet, copied = copied, onCopy = onCopy)
    }
}

@Composable
private fun FieldCard(
    label: String,
    value: String,
    copied: Boolean,
    onCopy: () -> Unit,
    masked: String? = null,
) {
    var revealed by remember { mutableStateOf(false) }
    Column {
        PreferenceRow(
            title = label,
            trailing = if (masked == null) null else {
                {
                    IconButton(onClick = { revealed = !revealed }) {
                        Icon(
                            imageVector = if (revealed) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = stringResource(
                                if (revealed) Res.string.mcp_hide else Res.string.mcp_reveal
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
        CodeBlock(
            text = if (masked == null || revealed) value else masked,
            copied = copied,
            onCopy = onCopy,
        )
    }
}

@Composable
private fun CopyButton(copied: Boolean, onCopy: () -> Unit) {
    IconButton(onClick = onCopy) {
        Icon(
            imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
            contentDescription = stringResource(
                if (copied) Res.string.mcp_copied else Res.string.mcp_copy
            ),
            tint = if (copied) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun CodeBlock(text: String, copied: Boolean, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(
                start = SettingsContentPadding,
                end = SettingsContentPadding,
                bottom = SettingsContentPadding,
            )
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(CodeBlockRadius),
            )
            .clickable(onClick = onCopy),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false,
                modifier = Modifier.padding(CodeBlockPadding),
            )
        }
        CopyButton(copied = copied, onCopy = onCopy)
    }
}

private val CodeBlockRadius = 8.dp
private val CodeBlockPadding = 12.dp

private const val ENDPOINT_ID = "endpoint"
private const val HEADER_ID = "auth-header"
private const val COPIED_FEEDBACK_MS = 2_000L
private const val CLIPBOARD_LIFETIME_MS = 60_000L
