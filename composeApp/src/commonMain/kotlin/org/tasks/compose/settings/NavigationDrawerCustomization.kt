package org.tasks.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.tasks.compose.PlatformBackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import org.jetbrains.compose.resources.stringResource
import org.tasks.filters.CaldavFilter
import org.tasks.filters.CustomFilter
import org.tasks.filters.FilterListItem
import org.tasks.filters.NavigationDrawerSubheader
import org.tasks.filters.PlaceFilter
import org.tasks.filters.TagFilter
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.customize_drawer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawerCustomization(
    items: List<FilterListItem>,
    collapsedSections: Set<String>,
    onToggleCollapse: (String?) -> Unit,
    onCreateNew: (NavigationDrawerSubheader) -> Unit = {},
    onBack: () -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onItemClick: (FilterListItem) -> Unit,
) {
    val listState = rememberLazyListState()

    PlatformBackHandler(enabled = true, onBack = onBack)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                items = items,
                key = { _, item ->
                    when (item) {
                        is CaldavFilter -> "caldav_${item.calendar.id}"
                        is TagFilter -> "tag_${item.tagData.id}"
                        is CustomFilter -> "filter_${item.id}"
                        is PlaceFilter -> "place_${item.place.id}"
                        is NavigationDrawerSubheader -> "subheader_${item.title}"
                        else -> "item_$item"
                    }
                }
            ) { index, item ->
                when (item) {
                    is NavigationDrawerSubheader -> {
                        SectionHeader(
                            title = item.title ?: "",
                            isCollapsed = collapsedSections.contains(item.title ?: ""),
                            onCreateClick = if (item.addIntentRc != 0) {
                                { onCreateNew(item) }
                            } else null,
                            onToggleCollapse = { onToggleCollapse(item.title) },
                        )
                    }
                    else -> {
                        // Find nearest preceding subheader by scanning backwards
                        var sectionTitle: String? = null
                        for (i in index - 1 downTo 0) {
                            val prev = items.getOrNull(i)
                            if (prev is NavigationDrawerSubheader) {
                                sectionTitle = prev.title
                                break
                            }
                        }
                        if (sectionTitle == null || !collapsedSections.contains(sectionTitle)) {
                            NavigationDrawerCustomizationRow(
                                item = item,
                                onClick = { onItemClick(item) },
                                onReorder = { from, to -> onReorder(from, to) },
                                index = index,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    isCollapsed: Boolean,
    onCreateClick: (() -> Unit)? = null,
    onToggleCollapse: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleCollapse() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        if (onCreateClick != null) {
            IconButton(
                onClick = { onCreateClick() },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                SymbolIcon(
                    name = TasksIcons.ADD,
                    contentDescription = "Add new item",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        SymbolIcon(
            name = if (isCollapsed) TasksIcons.EXPAND_MORE else TasksIcons.KEYBOARD_ARROW_UP,
            contentDescription = if (isCollapsed) "Expand" else "Collapse",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun NavigationDrawerCustomizationRow(
    item: FilterListItem,
    onClick: () -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    index: Int,
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    var totalDrag by remember { mutableStateOf(0f) }
    val currentIndex = rememberUpdatedState(index)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.itemType == FilterListItem.Type.ITEM) {
            SymbolIcon(
                name = TasksIcons.DRAG_INDICATOR,
                contentDescription = null,
                tint = if (isDragging) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(48.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset: androidx.compose.ui.geometry.Offset ->
                                isDragging = true
                                totalDrag = 0f
                            },
                            onDragEnd = {
                                isDragging = false
                                totalDrag = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                totalDrag = 0f
                            }
                        ) { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: androidx.compose.ui.geometry.Offset ->
                            totalDrag += dragAmount.y
                            val newIndex = currentIndex.value + (totalDrag / 60).toInt()
                            if (newIndex != currentIndex.value && newIndex >= 0) {
                                onReorder(currentIndex.value, newIndex)
                                totalDrag = 0f
                            }
                        }
                    }
            )
        } else {
            Spacer(modifier = Modifier.width(40.dp))
        }
        Text(
            text = getItemTitle(item),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun getItemTitle(item: FilterListItem): String {
    return when (item) {
        is CaldavFilter -> item.title
        is TagFilter -> item.title
        is CustomFilter -> item.title
        is PlaceFilter -> item.title
        is NavigationDrawerSubheader -> item.title ?: ""
        else -> ""
    }
}