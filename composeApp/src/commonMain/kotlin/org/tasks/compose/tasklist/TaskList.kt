package org.tasks.compose.tasklist

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.tasks.compose.chips.ChipDataProvider
import org.tasks.compose.rememberDateFormatter
import org.tasks.compose.tasklist.RowState
import org.tasks.compose.tasklist.TaskRow
import org.tasks.compose.tasklist.rowState
import org.tasks.data.SubtaskTreeRegistry
import org.tasks.data.TaskContainer
import org.tasks.data.deletions
import org.tasks.filters.Filter
import org.tasks.filters.key
import org.tasks.tasklist.SectionedDataSource

@Composable
internal fun TaskList(
    tasks: SectionedDataSource,
    filter: Filter,
    chipDataProvider: ChipDataProvider,
    listState: LazyListState = rememberLazyListState(),
    topPadding: Dp = 0.dp,
    onTaskClick: (TaskContainer) -> Unit,
    onCompleteTask: (TaskContainer, Boolean) -> Unit,
    onToggleGroup: (Long) -> Unit = {},
    onToggleSubtasks: (Long, Boolean) -> Unit = { _, _ -> },
    onFilterClick: (Filter) -> Unit = {},
    is24Hour: Boolean = false,
) {
    val dateFormatter = rememberDateFormatter(is24Hour)
    val subtaskTrees = koinInject<SubtaskTreeRegistry>()
    val deletions by remember(subtaskTrees) { subtaskTrees.deletions }
        .collectAsState(initial = emptyMap())
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = topPadding,
            bottom = 88.dp, // floating toolbar clearance
        ),
    ) {
        items(
            count = tasks.size,
            key = { if (tasks.isHeader(it)) -it.toLong() else tasks.getItem(it).id },
        ) { index ->
            if (tasks.isHeader(index)) {
                val section = tasks.getSection(index)
                SectionHeader(
                    header = if (filter.supportsSorting()) section.header else null,
                    collapsed = section.collapsed,
                    onToggle = { onToggleGroup(section.value) },
                )
                return@items
            }
            val task = tasks.getItem(index)
            val state = rowState(deletions, task)
            if (state == RowState.Hidden) {
                return@items
            }
            TaskRow(
                task = task,
                doomed = state == RowState.Doomed,
                filter = filter,
                groupMode = tasks.groupMode,
                chipDataProvider = chipDataProvider,
                is24Hour = is24Hour,
                dateFormatter = dateFormatter,
                onClick = { onTaskClick(task) },
                onToggleComplete = { onCompleteTask(task, !task.isCompleted) },
                onToggleSubtasks = { onToggleSubtasks(task.id, !task.isCollapsed) },
                onFilterClick = onFilterClick,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    header: String?,
    collapsed: Boolean,
    onToggle: () -> Unit,
) {
    if (header == null) {
        return
    }
    val rotation by animateFloatAsState(
        targetValue = if (collapsed) -180f else 0f,
        animationSpec = tween(durationMillis = 250),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = header,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        SymbolIcon(
            name = TasksIcons.KEYBOARD_ARROW_DOWN,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = rotation },
        )
    }
}
