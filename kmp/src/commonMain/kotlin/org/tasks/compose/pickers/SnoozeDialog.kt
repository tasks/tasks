package org.tasks.compose.pickers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.kmp.formatTime
import org.tasks.compose.CustomDialog
import org.tasks.compose.DialogRow
import org.tasks.reminders.snoozeOptions
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.cancel
import tasks.kmp.generated.resources.pick_a_date_and_time
import tasks.kmp.generated.resources.rmd_NoA_snooze

@Composable
fun SnoozeDialog(
    visible: Boolean,
    loadTimes: suspend () -> QuickPickTimes,
    is24Hour: Boolean,
    onSelected: (Long) -> Unit,
    onPickDateTime: () -> Unit,
    onDismiss: () -> Unit,
) {
    val defaults = remember { snoozeOptions(QuickPickTimes()) }
    val options by produceState(defaults, visible) {
        if (visible) {
            runCatching { loadTimes() }.getOrNull()?.let { value = snoozeOptions(it) }
        }
    }
    CustomDialog(visible = visible, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = stringResource(Res.string.rmd_NoA_snooze),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            options.forEach { option ->
                DialogRow(
                    text = "${stringResource(option.label)} (${formatTime(option.timestamp, is24Hour)})",
                    onClick = { onSelected(option.timestamp) },
                )
            }
            DialogRow(
                text = Res.string.pick_a_date_and_time,
                onClick = onPickDateTime,
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(text = stringResource(Res.string.cancel))
            }
        }
    }
}
