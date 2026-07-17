package org.tasks.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import org.tasks.data.entity.Task
import org.tasks.kmp.org.tasks.themes.ColorProvider.priorityColor as priorityColorInt

@Composable
@ReadOnlyComposable
fun priorityColor(@Task.Priority priority: Int): Color =
    Color(priorityColorInt(priority, isSystemInDarkTheme()))
