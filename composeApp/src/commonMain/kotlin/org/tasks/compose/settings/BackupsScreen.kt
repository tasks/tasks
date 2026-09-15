package org.tasks.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.tasks.viewmodel.ExportState

@Composable
fun BackupsScreen(
    exportState: ExportState,
    onExport: () -> Unit,
    onDismissResult: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onExport,
            enabled = exportState !is ExportState.Exporting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Backup now")
        }

        when (val state = exportState) {
            is ExportState.Exporting -> {
                CircularProgressIndicator()
                Text(
                    text = "Exporting tasks...",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            is ExportState.Done -> {
                Text(
                    text = "Backed up ${state.count} tasks to ${state.path}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Button(onClick = onDismissResult) {
                    Text("OK")
                }
            }
            is ExportState.Error -> {
                Text(
                    text = "Export failed: ${state.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = onDismissResult) {
                    Text("OK")
                }
            }
            is ExportState.Idle -> {
                Text(
                    text = "Export all tasks and settings to a JSON backup file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
