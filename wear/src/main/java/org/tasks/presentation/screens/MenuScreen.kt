package org.tasks.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.paging.items
import org.tasks.GrpcProto.ListItem
import org.tasks.GrpcProto.ListItemType
import org.tasks.compose.components.imageVectorByName
import org.tasks.presentation.components.Card
import org.tasks.presentation.components.EmptyCard
import org.tasks.presentation.components.Header

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun MenuScreen(
    items: LazyPagingItems<ListItem>,
    selectFilter: (ListItem) -> Unit,
) {
    val columnState = rememberResponsiveColumnState()
    ScreenScaffold(
        scrollState = columnState,
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            columnState = columnState,
        ) {
            items(
                items = items,
                key = { item -> "${item.type}_${item.id}" },
            ) { item ->
                if (item == null) {
                    EmptyCard()
                } else {
                    when (item.type) {
                        ListItemType.Header ->
                            Header(text = item.title)

                        ListItemType.Item -> {
                            Card(
                                icon = {
                                    val icon = imageVectorByName(item.icon)
                                    Box(
                                        modifier = Modifier.size(48.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (icon != null) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = when (item.color) {
                                                    0 -> MaterialTheme.colors.onSurface
                                                    else -> Color(color = item.color)
                                                },
                                            )
                                        }
                                    }
                                },
                                onClick = { selectFilter(item) },
                            ) {
                                Text(
                                    item.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(end = 12.dp).weight(1f),
                                )
                                if (item.taskCount > 0) {
                                    Text(
                                        item.taskCount.toString(), // TODO: localize
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                }
                            }
                        }

                        else -> throw IllegalArgumentException()
                    }
                }
            }
        }
    }
}