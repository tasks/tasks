package org.tasks.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
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
                    start = 16.dp,
                    top = 20.dp,
                    end = 32.dp,
                    bottom = 20.dp
                )
        )
    },
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