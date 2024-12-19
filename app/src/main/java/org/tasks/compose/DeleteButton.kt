package org.tasks.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.tasks.R
import org.tasks.compose.settings.PromptAction

@Composable
fun DeleteButton(
    title: String,
    onDelete: () -> Unit
) {
    var promptDelete by remember { mutableStateOf(false) }
    IconButton(onClick = { promptDelete = true }) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = stringResource(id = R.string.delete),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
    PromptAction(
        showDialog = promptDelete,
        title = stringResource(id = R.string.delete_tag_confirmation, title),
        onAction = { onDelete() },
        onCancel = { promptDelete = false },
    )
}
