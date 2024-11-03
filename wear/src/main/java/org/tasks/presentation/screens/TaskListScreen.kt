package org.tasks.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
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
import org.tasks.presentation.components.Checkbox
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
        var initialLoad by remember { mutableStateOf(true) }
        var hasError by remember { mutableStateOf(true) }
        LaunchedEffect(uiItems.loadState) {
            hasError = uiItems.loadState.hasError
            if (initialLoad && uiItems.loadState.refresh is LoadState.NotLoading) {
                initialLoad = false
            }
        }
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
            if (hasError) {
                item {
                    Text(
                        text = "Error loading tasks",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else if (initialLoad && uiItems.loadState.refresh is LoadState.Loading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
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
                                        timestamp = item.timestamp,
                                        hidden = item.hidden,
                                        subtasksCollapsed = item.collapsed,
                                        numSubtasks = item.numSubtasks,
                                        icon = {
                                            Checkbox(
                                                completed = item.completed,
                                                repeating = item.repeating,
                                                priority = item.priority,
                                                toggleComplete = {
                                                    onComplete(item.id, !item.completed)
                                                }
                                            )
                                        },
                                        onClick = { openTask(item.id) },
                                        toggleSubtasks = {
                                            toggleSubtasks(
                                                item.id,
                                                !item.collapsed
                                            )
                                        },
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
