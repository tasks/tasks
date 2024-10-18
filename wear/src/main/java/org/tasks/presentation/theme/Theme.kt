package org.tasks.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.MaterialTheme
import org.tasks.kmp.org.tasks.themes.ColorProvider

@Composable
fun TasksTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = Color(ColorProvider.BLUE_500),
            onPrimary = Color.White,
        ),
        content = content
    )
}