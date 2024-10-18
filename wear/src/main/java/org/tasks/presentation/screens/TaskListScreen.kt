package org.tasks.presentation.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onComplete: (Long) -> Unit,
    onClick: (Long) -> Unit,
    addTask: () -> Unit,
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
                TaskCard(
                    text = stringResource(Res.string.add_task),
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            tint = MaterialTheme.colors.onPrimary,
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp),
                        )
                    },
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = MaterialTheme.colors.onPrimary,
                    onClick = addTask,
                )
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
                                        onClick = { onComplete(item.id) },
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
                                onClick = { onClick(item.id) },
                            )

                        GrpcProto.UiItemType.Header ->
                            GroupSeparator(header = item)

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
    header: GrpcProto.UiItem,
) {
    Text(
        text = header.title,
        modifier = Modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth(),
        textAlign = TextAlign.Center,
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
