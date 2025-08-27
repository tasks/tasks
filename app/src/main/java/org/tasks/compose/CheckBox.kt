package org.tasks.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import org.tasks.data.entity.Task
import org.tasks.kmp.org.tasks.themes.ColorProvider.priorityColor
import org.tasks.ui.CheckBoxProvider.Companion.getCheckboxRes

import androidx.compose.material3.Checkbox
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import org.tasks.preferences.Preferences

@Composable
fun CheckBox(
    task: Task,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    preferences: Preferences? = null,
) {
    val style = preferences?.checkboxStyle ?: "default"
    if (style == "round") {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onCompleteClick() },
            modifier = modifier.clip(CircleShape),
            enabled = true
        )
    } else {
        CheckBox(
            isCompleted = task.isCompleted,
            isRecurring = task.isRecurring,
            priority = task.priority,
            onCompleteClick = onCompleteClick,
            modifier = modifier,
        )
    }
}

@Composable
fun CheckBox(
    isCompleted: Boolean,
    isRecurring: Boolean,
    priority: Int,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onCompleteClick, modifier = modifier) {
        Icon(
            painter = painterResource(id = getCheckboxRes(isCompleted, isRecurring)),
            tint = Color(
                priorityColor(
                    priority = priority,
                    isDarkMode = isSystemInDarkTheme(),
                )
            ),
            contentDescription = null,
        )
    }
}