/**
 * Chevron.kt — Expand/collapse chevron indicator.
 *
 * Displays a small down-pointing or up-pointing chevron icon
 * to indicate whether a group of subtasks is collapsed or expanded.
 *
 * Used inside [TaskCard] when the task has subtasks ([numSubtasks] > 0).
 *
 * @param collapsed `true` → shows ExpandMore (▼); `false` → shows ExpandLess (▲).
 */
package org.tasks.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
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
    Icon(
        modifier = Modifier.rotate(rotation),
        imageVector = Icons.Outlined.ExpandMore,
        contentDescription = null,
        tint = MaterialTheme.colors.onSurface,
    )
}
