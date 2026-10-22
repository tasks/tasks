package org.tasks.compose.tasklist

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun TaskList(
    tasks: SectionedDataSource,
    filter: Filter,
    chipDataProvider: ChipDataProvider,
    listState: LazyListState = rememberLazyListState(),
    topPadding: Dp = 0.dp,
    selectedTaskId: Long? = null,
    dragEnabled: Boolean = false,
    onTaskDrop: (taskId: Long, dropIndex: Int) -> Unit = { _, _ -> },
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

    // Display order of section indices. The sh.calvin reorderable library mutates it
    // live while a drag is in flight (that mutation is what the LazyColumn animates);
    // it is rebuilt whenever the underlying data changes, so a drop that the planner
    // rejects simply settles back to the data's order on the next reload.
    var display by remember(tasks) { mutableStateOf((0 until tasks.size).toList()) }
    fun keyOf(sectionIndex: Int): Any =
        if (tasks.isHeader(sectionIndex)) -sectionIndex.toLong() else tasks.getItem(sectionIndex).id

    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        // from/to keys are the LazyColumn item keys (stable across header offsets),
        // so the data list is located by key, exactly as the library's demo does.
        val d = display.toMutableList()
        val fromIndex = d.indexOfFirst { keyOf(it) == from.key }
        val toIndex = d.indexOfFirst { keyOf(it) == to.key }
        if (fromIndex in 0 until d.size && toIndex in 0 until d.size && fromIndex != toIndex) {
            d.add(toIndex, d.removeAt(fromIndex))
            display = d
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            top = topPadding,
            bottom = 88.dp, // floating toolbar clearance
        ),
    ) {
        items(
            count = display.size,
            key = { i -> keyOf(display[i]) },
        ) { index ->
            val sectionIndex = display[index]
            if (tasks.isHeader(sectionIndex)) {
                val section = tasks.getSection(sectionIndex)
                SectionHeader(
                    header = if (filter.supportsSorting()) section.header else null,
                    collapsed = section.collapsed,
                    onToggle = { onToggleGroup(section.value) },
                )
                return@items
            }
            val task = tasks.getItem(sectionIndex)
            val state = rowState(deletions, task)
            if (state == RowState.Hidden) {
                return@items
            }
            if (!dragEnabled) {
                TaskRow(
                    task = task,
                    selected = selectedTaskId == task.id,
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
                return@items
            }
            // Manual-sort drag: the reorderable library owns the gesture, the live
            // reordering and the animation; on release we report the row the task
            // settled on and the caller commits it through the planner + shared
            // CalDAV path. Section/parent/subtree legality is enforced there.
            ReorderableItem(reorderableState, task.id) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                // Display position the task left from; a release that settles back on
                // the same row is a no-op and is not reported.
                var dragStartPos by remember { mutableStateOf(-1) }
                // The handle modifier carries the library's gesture; it must be applied
                // to the handle node inside DragHandle (the TaskRow passes its own
                // bare modifier for layout, appended after the gesture).
                val handle = Modifier.draggableHandle(
                    onDragStarted = {
                        dragStartPos = display.indexOf(sectionIndex)
                    },
                    onDragStopped = {
                        // The settled display position is exactly the planner's
                        // dropIndex convention (row the task occupies after the move).
                        val pos = display.indexOf(sectionIndex)
                        if (pos >= 0 && pos != dragStartPos) onTaskDrop(task.id, pos)
                        dragStartPos = -1
                    },
                )
                Surface(
                    shadowElevation = elevation,
                    color = if (isDragging) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        Color.Transparent
                    },
                ) {
                    TaskRow(
                        task = task,
                        selected = selectedTaskId == task.id,
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
                        dragHandle = { modifier -> DragHandle(handleModifier = handle.then(modifier)) },
                        dragged = isDragging,
                    )
                }
            }
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
