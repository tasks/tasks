package org.tasks.presentation.components

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme

@Composable
fun Chevron(collapsed: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (collapsed) 0f else 180f,
        animationSpec = tween(250),
        label = "arrow rotation",
    )
    SymbolIcon(
        modifier = Modifier.rotate(rotation),
        name = TasksIcons.EXPAND_MORE,
        contentDescription = null,
        tint = MaterialTheme.colors.onSurface,
    )
}
