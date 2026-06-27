/**
 * OfflineTaskListScreen.kt — Main task list screen for the Wear OS app.
 *
 * ## Layout (top → bottom inside a [ScalingLazyColumn]):
 *
 * 1. **Connection banner** — shows "Connected" / "Offline" / "Syncing…"
 *    with a cloud icon and an optional manual-sync button.
 * 2. **Task rows** — each task is rendered via [TaskRow] ([TaskCard] +
 *    [Checkbox]). Tapping a row navigates to [OfflineTaskEditScreen];
 *    tapping the checkbox toggles completion.
 * 3. **Add button** — a prominent "Add task" chip at the bottom.
 * 4. **Settings button** — opens [OfflineSettingsScreen].
 *
 * ## Offline-first
 * Tasks are loaded from the local Room database via
 * [OfflineTaskListViewModel]. The connection banner is purely
 * informational — all features work without a phone connection.
 *
 * ## State
 * The screen is fully stateless; all state lives in
 * [OfflineTaskListViewState] which is collected from the ViewModel's
 * [StateFlow] in [MainActivity].
 */
package org.tasks.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import org.jetbrains.compose.resources.stringResource
import org.tasks.data.sync.SyncState
import org.tasks.presentation.components.EmptyCard
import org.tasks.presentation.components.TaskRow
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.add_task

/**
 * Task list screen that works fully offline.
 * Shows tasks from local database and syncs when connected.
 */
@OptIn(ExperimentalHorologistApi::class)
@Composable
fun OfflineTaskListScreen(
    viewState: OfflineTaskListState,
    onTaskClick: (String) -> Unit,
    onToggleComplete: (String, Boolean) -> Unit,
    onAddTask: () -> Unit,
    openSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    val columnState = rememberResponsiveColumnState()
    ScreenScaffold(
        scrollState = columnState,
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            columnState = columnState,
        ) {
            // Header with buttons
            item(key = "header_buttons") {
                OfflineButtonHeader(
                    addTask = onAddTask,
                    openSettings = openSettings,
                    onRefresh = onRefresh,
                )
            }

            // Connection status indicator
            item(key = "connection_status") {
                ConnectionStatusRow(
                    isConnected = viewState.isConnected,
                    syncState = viewState.syncState,
                    hasPendingSync = viewState.hasPendingSync,
                )
            }

            if (viewState.isLoading) {
                item(key = "loading_indicator") {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else if (viewState.tasks.isEmpty()) {
                item(key = "empty_card") { EmptyCard() }
            } else {
                // Display tasks - use stringId for unique keys
                viewState.tasks.forEachIndexed { index, task ->
                    item(key = "task_${task.stringId}_${task.completed}_$index") {
                        TaskRow(
                            task = task,
                            onClick = { onTaskClick(task.stringId) },
                            onToggleComplete = {
                                onToggleComplete(task.stringId, !task.completed)
                            },
                            onToggleSubtasks = { /* no-op for now */ },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Header with action buttons for the offline task list.
 */
@Composable
private fun OfflineButtonHeader(
    addTask: () -> Unit,
    openSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colors.onSurface,
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Sync,
                contentDescription = "Refresh",
            )
        }
        Button(
            onClick = addTask,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(Res.string.add_task),
            )
        }
        Button(
            onClick = openSettings,
            colors = ButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colors.onSurface,
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
            )
        }
    }
}

/**
 * Row showing connection status.
 */
@Composable
private fun ConnectionStatusRow(
    isConnected: Boolean,
    syncState: SyncState,
    hasPendingSync: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            syncState == SyncState.SYNCING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Syncing...",
                    style = MaterialTheme.typography.caption3,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
            isConnected -> {
                Icon(
                    imageVector = Icons.Outlined.Cloud,
                    contentDescription = "Connected",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colors.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Connected",
                    style = MaterialTheme.typography.caption3,
                    color = MaterialTheme.colors.primary,
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = "Offline",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colors.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (hasPendingSync) "Offline (pending sync)" else "Offline",
                    style = MaterialTheme.typography.caption3,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}
