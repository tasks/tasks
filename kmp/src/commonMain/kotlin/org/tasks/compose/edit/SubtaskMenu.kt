package org.tasks.compose.edit

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.PlatformBackHandler
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.delete
import tasks.kmp.generated.resources.edit_task
import tasks.kmp.generated.resources.more_options

@Composable
fun SubtaskMenu(onOpen: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    PlatformBackHandler(enabled = expanded) { expanded = false }
    Box {
        IconButton(onClick = { expanded = true }) {
            SymbolIcon(
                name = TasksIcons.MORE_VERT,
                contentDescription = stringResource(Res.string.more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.edit_task)) },
                leadingIcon = { SymbolIcon(TasksIcons.EDIT, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpen()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.delete)) },
                leadingIcon = { SymbolIcon(TasksIcons.DELETE, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}
