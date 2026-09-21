package org.tasks.compose

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.delete

@Composable
fun ClearButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        SymbolIcon(
            name = TasksIcons.CLEAR,
            contentDescription = stringResource(Res.string.delete),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
