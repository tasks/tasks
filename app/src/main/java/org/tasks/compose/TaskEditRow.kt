package org.tasks.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

@Composable
fun TaskEditRow(
    iconRes: Int = 0,
    icon: @Composable () -> Unit = {
        TaskEditIcon(
            id = iconRes,
            modifier = Modifier
                .alpha(ContentAlpha.medium)
                .padding(
                    start = 4.dp,
                    top = 8.dp,
                    end = 20.dp,
                    bottom = 8.dp
                )
        )
    },
    content: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable(
            enabled = onClick != null,
            onClick = { onClick?.invoke() }
        )
        .background(MaterialTheme.colorScheme.surface),
    ) {
        icon()
        content()
    }
}