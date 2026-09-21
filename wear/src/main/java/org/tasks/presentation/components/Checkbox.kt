package org.tasks.presentation.components

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import org.tasks.kmp.org.tasks.themes.ColorProvider

@Composable
fun Checkbox(
    completed: Boolean,
    repeating: Boolean,
    priority: Int,
    toggleComplete: () -> Unit,
) {
    Button(
        onClick = { toggleComplete() },
        colors = ButtonDefaults.iconButtonColors(),
    ) {
        SymbolIcon(
            name = when {
                completed -> TasksIcons.CHECK_BOX
                repeating -> TasksIcons.REPEAT
                else -> TasksIcons.CHECK_BOX_OUTLINE_BLANK
            },
            tint = Color(
                ColorProvider.priorityColor(priority, isDarkMode = true)
            ),
            contentDescription = null,
        )
    }

}