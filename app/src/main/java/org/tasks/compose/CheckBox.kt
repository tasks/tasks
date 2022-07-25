package org.tasks.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.todoroo.astrid.data.Task
import org.tasks.themes.ColorProvider
import org.tasks.ui.CheckBoxProvider.Companion.getCheckboxRes

@Composable
fun CheckBox(
    task: Task,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    desaturate: Boolean,
) {
    IconButton(onClick = onCompleteClick, modifier = modifier) {
        Icon(
            painter = painterResource(id = task.getCheckboxRes()),
            tint = Color(
                ColorProvider.priorityColor(
                    priority = task.priority,
                    isDarkMode = isSystemInDarkTheme(),
                    desaturate = desaturate,
                )
            ),
            contentDescription = null,
        )
    }
}
