package org.tasks.compose.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate

@Composable
fun Chevron(collapsed: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (collapsed) 0f else 180f,
        animationSpec = tween(250),
        label = "arrow rotation",
    )
    Icon(
        modifier = Modifier.rotate(rotation),
        imageVector = Icons.Outlined.ExpandMore,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
    )
}