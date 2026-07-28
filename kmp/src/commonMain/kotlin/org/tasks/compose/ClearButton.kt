package org.tasks.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
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
        Icon(
            imageVector = Icons.Outlined.Clear,
            contentDescription = stringResource(Res.string.delete),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
