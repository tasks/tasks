/**
 * OfflineSettingsScreen.kt — Settings screen for the Wear OS Tasks app.
 *
 * Provides two toggles:
 * - **Show completed tasks** — when off, completed tasks are hidden from the list.
 * - **Show hidden tasks** — when off, tasks flagged as hidden are filtered out.
 *
 * Also displays a read-only connection-status row so the user knows whether
 * the watch is currently paired with the phone.
 *
 * All settings are persisted to the local Room database via
 * [OfflineSettingsViewModel] / [SettingsRepository] and take effect immediately.
 */
package org.tasks.presentation.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.material.ToggleChip
import com.google.android.horologist.compose.material.ToggleChipToggleControl
import org.jetbrains.compose.resources.stringResource
import org.tasks.data.sync.SyncState
import org.tasks.presentation.theme.TasksTheme
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.show_completed
import tasks.kmp.generated.resources.show_unstarted

/**
 * Settings screen that works offline.
 * Shows connection status and syncs with phone when available.
 */
@OptIn(ExperimentalHorologistApi::class)
@Composable
fun OfflineSettingsScreen(
    viewState: OfflineSettingsViewState,
    toggleShowHidden: (Boolean) -> Unit,
    toggleShowCompleted: (Boolean) -> Unit,
) {
    val columnState = rememberResponsiveColumnState()
    ScreenScaffold(
        scrollState = columnState,
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            columnState = columnState,
        ) {
            // Connection status indicator
            item(key = "offline_settings_connection_status") {
                ConnectionStatusChip(
                    isConnected = viewState.isConnected,
                    syncState = viewState.syncState,
                )
            }

            // Show hidden (unstarted) toggle
            item(key = "offline_settings_show_hidden") {
                ToggleChip(
                    checked = viewState.showHidden,
                    onCheckedChanged = { toggleShowHidden(it) },
                    label = stringResource(Res.string.show_unstarted),
                    toggleControl = ToggleChipToggleControl.Switch,
                )
            }

            // Show completed toggle
            item(key = "offline_settings_show_completed") {
                ToggleChip(
                    checked = viewState.showCompleted,
                    onCheckedChanged = { toggleShowCompleted(it) },
                    label = stringResource(Res.string.show_completed),
                    toggleControl = ToggleChipToggleControl.Switch,
                )
            }
        }
    }
}

/**
 * Chip showing connection status.
 */
@Composable
private fun ConnectionStatusChip(
    isConnected: Boolean,
    syncState: SyncState,
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            syncState == SyncState.SYNCING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Syncing...",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
            isConnected -> {
                Icon(
                    imageVector = Icons.Outlined.Cloud,
                    contentDescription = "Connected",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colors.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Connected",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.primary,
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = "Offline",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colors.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Offline mode",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OfflineSettingsScreenPreview() {
    TasksTheme {
        OfflineSettingsScreen(
            viewState = OfflineSettingsViewState(
                isConnected = true,
                syncState = SyncState.IDLE,
                showHidden = true,
                showCompleted = false,
            ),
            toggleShowHidden = {},
            toggleShowCompleted = {},
        )
    }
}
