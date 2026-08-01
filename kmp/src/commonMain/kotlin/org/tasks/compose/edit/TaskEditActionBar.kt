package org.tasks.compose.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.PlatformBackHandler
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.delete_task
import tasks.kmp.generated.resources.mark_completed
import tasks.kmp.generated.resources.menu_discard_changes
import tasks.kmp.generated.resources.more_options

val TaskEditActionBarHeight = 64.dp

private val BarPadding = 8.dp
private val BarElevation = 6.dp
private val IconTextGap = 8.dp

@Composable
fun TaskEditActionBar(
    onMarkCompleted: () -> Unit,
    onDiscardChanges: () -> Unit,
    onDeleteTask: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            expanded = false
        }
    }
    PlatformBackHandler(enabled = expanded) { expanded = false }
    Surface(
        modifier = modifier.height(TaskEditActionBarHeight),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = BarElevation,
    ) {
        Row(
            modifier = Modifier.padding(BarPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BarPadding),
        ) {
            TextButton(
                onClick = onMarkCompleted,
                enabled = enabled,
                shape = CircleShape,
                modifier = Modifier.fillMaxHeight(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(Res.string.mark_completed),
                    modifier = Modifier.padding(start = IconTextGap),
                )
            }
            Box {
                IconButton(onClick = { expanded = true }, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(Res.string.more_options),
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.menu_discard_changes)) },
                        onClick = {
                            expanded = false
                            onDiscardChanges()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.delete_task)) },
                        onClick = {
                            expanded = false
                            onDeleteTask()
                        },
                    )
                }
            }
        }
    }
}
