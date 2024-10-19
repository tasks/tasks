package org.tasks.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
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
import com.google.android.horologist.compose.paging.items
import org.jetbrains.compose.resources.stringResource
import org.tasks.GrpcProto
import org.tasks.kmp.org.tasks.themes.ColorProvider
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
                key = { item -> "${item.type}_${item.id}" },
            ) { item ->
                if (item == null) {
                    TaskCard(
                        text = "",
                        icon = {},
                        onClick = {},
                    )
                } else {
                    when (item.type) {
                        GrpcProto.UiItemType.Task ->
                            TaskCard(
                                text = item.title,
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
                            )

                        GrpcProto.UiItemType.Header ->
                            GroupSeparator(
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
fun GroupSeparator(
    title: String,
    collapsed: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(
            text = title,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Chevron(collapsed = collapsed)
    }
}

@Composable
private fun Chevron(collapsed: Boolean) {
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

@Composable
fun TaskCard(
    text: String,
    icon: @Composable () -> Unit = {},
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = MaterialTheme.colors.onSurface,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        backgroundPainter = ColorPainter(backgroundColor),
        contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 12.dp, bottom = 0.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Text(
                text = text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = contentColor,
            )
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
                contentDescription = org.jetbrains.compose.resources.stringResource(Res.string.add_task),
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
