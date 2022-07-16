package org.tasks.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TaskEditRow(
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    Row(modifier = Modifier
        .clickable(
            enabled = onClick != null,
            onClick = { onClick?.invoke() }
        )
    ) {
        icon()
        content()
    }
}