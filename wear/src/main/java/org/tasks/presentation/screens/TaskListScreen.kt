package org.tasks.presentation.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import org.tasks.GrpcProto
import org.tasks.kmp.org.tasks.themes.ColorProvider

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun TaskListScreen(
    tasks: MutableList<GrpcProto.Task>,
    onComplete: (Long) -> Unit,
    onClick: (Long) -> Unit,
) {
    val columnState = rememberResponsiveColumnState()
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            columnState = columnState,
        ) {
            items(tasks.size) { index ->
                val task = tasks[index]
                key(task.id) {
                    TaskCard(
                        task = task,
                        onComplete = { onComplete(task.id) },
                        onClick = { onClick(task.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: GrpcProto.Task,
    onComplete: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        backgroundPainter = ColorPainter(MaterialTheme.colors.surface),
        contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 12.dp, bottom = 0.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.iconButtonColors(),
            ) {
                Icon(
                    imageVector = when {
                        task.completed -> Icons.Outlined.CheckBox
                        task.repeating -> Icons.Outlined.Repeat
                        else -> Icons.Outlined.CheckBoxOutlineBlank
                    },
                    tint = Color(
                        ColorProvider.priorityColor(task.priority, isDarkMode = true, desaturate = true)
                    ),
                    contentDescription = null,
                )
            }
            Text(
                text = task.title,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}
