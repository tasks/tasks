package org.tasks.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.paging.items
import org.jetbrains.compose.resources.stringResource
import org.tasks.GrpcProto
import org.tasks.kmp.org.tasks.themes.ColorProvider
import org.tasks.presentation.components.CollapsibleHeader
import org.tasks.presentation.components.EmptyCard
import org.tasks.presentation.components.TaskCard
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.add_task

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun TaskListScreen(
    uiItems: LazyPagingItems<GrpcProto.UiItem>,
    toggleGroup: (Long, Boolean) -> Unit,
    onComplete: (Long, Boolean) -> Unit,
    openTask: (Long) -> Unit,
    addTask: () -> Unit,
    openMenu: () -> Unit,
    openSettings: () -> Unit,
    toggleSubtasks: (Long, Boolean) -> Unit,
) {
    val columnState = rememberResponsiveColumnState()
    ScreenScaffold(
        scrollState = columnState,
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            columnState = columnState,
        ) {
            item {
                ButtonHeader(
                    openMenu = openMenu,
                    addTask = addTask,
                    openSettings = openSettings,
                )
//                TitleHeader(
//                    title = "My Tasks",
//                    openMenu = openMenu,
//                    addTask = addTask,
//                )
            }
            items(
                items = uiItems,
                key = { item -> "${item.type}_${item.id}_${item.completed}" },
            ) { item ->
                if (item == null) {
                    EmptyCard()
                } else {
                    when (item.type) {
                        GrpcProto.ListItemType.Item ->
                            Row {
                                if (item.indent > 0) {
                                    Spacer(modifier = Modifier.width(20.dp * item.indent))
                                }
                                TaskCard(
                                    text = item.title,
                                    hidden = item.hidden,
                                    subtasksCollapsed = item.collapsed,
                                    numSubtasks = item.numSubtasks,
                                    icon = {
                                        Button(
                                            onClick = { onComplete(item.id, !item.completed) },
                                            colors = ButtonDefaults.iconButtonColors(),
                                        ) {
                                            Icon(
                                                imageVector = when {
                                                    item.completed -> Icons.Outlined.CheckBox
                                                    item.repeating -> Icons.Outlined.Repeat
                                                    else -> Icons.Outlined.CheckBoxOutlineBlank
                                                },
                                                tint = Color(
                                                    ColorProvider.priorityColor(
                                                        item.priority,
                                                        isDarkMode = true,
                                                        desaturate = true
                                                    )
                                                ),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    onClick = { openTask(item.id) },
                                    toggleSubtasks = { toggleSubtasks(item.id, !item.collapsed) },
                                )
                            }

                        GrpcProto.ListItemType.Header ->
                            CollapsibleHeader(
                                title = item.title,
                                collapsed = item.collapsed,
                                onClick = { toggleGroup(item.id, !item.collapsed) },
                            )

                        else -> {
                            throw IllegalStateException("Unknown item type: ${item.type}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TitleHeader(
    title: String,
    openMenu: () -> Unit,
    addTask: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = openMenu,
            colors = ButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colors.onSurface,
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = null,
            )
        }
        Text(
            text = title,
            maxLines = 2,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.title3,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = addTask,
            colors = ButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colors.onSurface,
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(Res.string.add_task),
            )
        }
    }
}

@Composable
fun ButtonHeader(
    openMenu: () -> Unit,
    addTask: () -> Unit,
    openSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Button(
            onClick = openMenu,
            colors = ButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colors.onSurface,
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = null,
            )
        }
        Button(
            onClick = addTask,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(Res.string.add_task),
            )
        }
        Button(
            onClick = openSettings,
            colors = ButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colors.onSurface,
            )

        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
            )
        }
    }
}
