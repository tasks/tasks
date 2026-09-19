package org.tasks.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun TaskEditIcon(@DrawableRes id: Int, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(id = id),
        contentDescription = null,
        modifier = modifier
            .alpha(ContentAlpha.medium)
            .padding(12.dp)
            .size(24.dp),
        tint = MaterialTheme.colorScheme.onSurface,
    )
}