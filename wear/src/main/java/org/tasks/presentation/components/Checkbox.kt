package org.tasks.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Repeat
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
        Icon(
            imageVector = when {
                completed -> Icons.Outlined.CheckBox
                repeating -> Icons.Outlined.Repeat
                else -> Icons.Outlined.CheckBoxOutlineBlank
            },
            tint = Color(
                ColorProvider.priorityColor(priority, isDarkMode = true, desaturate = true)
            ),
            contentDescription = null,
        )
    }

}