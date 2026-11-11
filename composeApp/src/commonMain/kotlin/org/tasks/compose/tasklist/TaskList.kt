package org.tasks.compose.tasklist

import org.tasks.themes.TasksIcons
import org.tasks.compose.components.SymbolIcon
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.tasks.compose.chips.ChipDataProvider
import org.tasks.compose.rememberDateFormatter
import org.tasks.compose.tasklist.RowState
import org.tasks.compose.tasklist.TaskRow
import org.tasks.compose.tasklist.rowState
import org.tasks.data.INDENT_STEP_DP
import org.tasks.data.SubtaskTreeRegistry
import org.tasks.data.TaskContainer
import org.tasks.data.deletions
import org.tasks.filters.Filter
import org.tasks.filters.key
import org.tasks.tasklist.SectionedDataSource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Live debug overlay for the drag-to-reparent pipeline: pointer X vs grab X and the
 * half-step threshold, the indent/outdent decision, the drag state, the computed
 * reparent target and the live display order. Delete with the chip.
 */

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
    onTaskReparent: (taskId: Long, targetTaskId: Long) -> Unit = { _, _ -> },
    onTaskOutdent: (taskId: Long, dropIndex: Int) -> Unit = { _, _ -> },
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

    // Drag-to-reparent state, shared across rows: the release decision needs the live
    // display and the pointer position, neither of which any single row owns. The
    // reorderable library reports no pointer on release, so the last pressed position
    // is observed non-consumingly; it is already in the LazyColumn's own space (the
    // pointerInput node), which is the space the row indents below use.
    val density = LocalDensity.current
    var dragTask by remember { mutableStateOf<ReparentDrag?>(null) }
    var pointerX by remember { mutableFloatStateOf(Float.NaN) }
    var dragStartPointerX by remember { mutableFloatStateOf(Float.NaN) }
    // The live indent indicator level while dragging: +1 (will become a
    // subtask), -1 (will outdent), 0 (sibling reorder). Drives the dragged row's
    // horizontal shift; the release decision commits the same level via the same
    // half-step threshold (isIndented/isOutdented).
    var dragIndentLevel by remember { mutableIntStateOf(0) }

    // The reorderable library's draggable modifier captures the drag callbacks in a
    // pointer-input scope that is NOT re-run on recomposition (verified in the
    // library bytecode: the callback is a final field of the captured lambda), so a
    // callback created in an earlier composition keeps reading that composition's
    // `tasks` forever. After a commit replaces the data source, a row that has not
    // moved position keeps its stale callback - the drop decision then sees the
    // pre-commit task (stale indent) and no-ops. The fix is the standard holder
    // pattern: the library gets a stable wrapper, the holder is re-assigned on every
    // recomposition, and `tasksRef` always reads the current data source.
    val tasksRef by rememberUpdatedState(tasks)

    /** Half an indent step in list pixels: the fixed half-step threshold of the native app's drag commit. */
    val indentStepPx = with(density) { (INDENT_STEP_DP / 2).dp.toPx() }

    /**
     * Whether the pointer has travelled more than half an indent step to the right of
     * where the drag started. A drag with no horizontal travel is a plain sibling
     * reorder regardless of the vertical drop point (the native app's two-item case);
     * only an indented drag can turn the task into a subtask. The delta is measured
     * from the grab point, not against the row's indent edge, because the drag handle
     * sits to the right of the edge: a straight-down drag from the handle must stay a
     * sibling reorder.
     */
    fun isIndented(pointerX: Float): Boolean =
        !pointerX.isNaN() &&
            !dragStartPointerX.isNaN() &&
            pointerX - dragStartPointerX > indentStepPx

    /**
     * Whether the pointer has travelled more than half an indent step to the left of
     * where the drag started - the outdent gesture. Only meaningful for tasks that
     * already have a parent; releasing an outdented drag moves the task one indent
     * level shallower at the settled position.
     */
    fun isOutdented(pointerX: Float): Boolean =
        !pointerX.isNaN() &&
            !dragStartPointerX.isNaN() &&
            dragStartPointerX - pointerX > indentStepPx

    /**
     * The task that would become the new parent if the drag were released now, with
     * the live display settling the task onto row [dropIndex], or null for a plain
     * sibling settle. The horizontal test gates on [isIndented]; the parent is the
     * native `findParentIndex` nearest shallower-or-equal row above the settled
     * position ([reparentTargetForDrop]).
     */
    fun reparentTarget(task: TaskContainer, dropIndex: Int): Long? {
        if (!isIndented(pointerX)) return null
        return tasksRef.reparentTargetForDrop(deletions, task.id, dropIndex)
    }

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

    Box {
        LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // Non-consuming observation: the reorderable library owns the
                        // gesture; here the pointer is only tracked for the drop-zone
                        // decision and the live "into" indicator.
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (!change.pressed) continue
                        pointerX = change.position.x
                        val drag = dragTask ?: continue
                        val pos = display.indexOfFirst { sectionIndex ->
                            !tasksRef.isHeader(sectionIndex) &&
                                tasksRef.getItem(sectionIndex).id == drag.taskId
                        }
                        val dragIndent =
                            if (pos in display.indices) tasksRef.getItem(display[pos]).indent else 0
                        // The native app's "into" cue, without its colour: the dragged row's
                        // shift level, from the same half-step threshold the commit uses.
                        val level = when {
                            isIndented(pointerX) -> 1
                            isOutdented(pointerX) && dragIndent > 0 -> -1
                            else -> 0
                        }
                        if (level != dragIndentLevel) dragIndentLevel = level
                    }
                }
            },
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
                // Live indent indicator: while the pointer is past the half-step
                // threshold, the dragged row shifts one indent step toward its drop
                // level (the native app's "about to become a subtask" cue; there is
                // no colour highlight). Implemented as animated start padding so the
                // row genuinely indents (handle and content shift together). The
                // animated value is only applied while the drag of THIS task is
                // active: on release the commit changes task.indent instantly, so
                // letting the animation run out afterwards would add a stale
                // negative offset to an already-outdented row (negative padding).
                val dragIndentOffset by animateDpAsState(
                    targetValue = if (isDragging && dragTask?.taskId == task.id) {
                        (dragIndentLevel * INDENT_STEP_DP).dp
                    } else {
                        0.dp
                    },
                    label = "drag-indent",
                )
                // The handle modifier carries the library's gesture; it must be applied
                // to the handle node inside DragHandle (the TaskRow passes its own
                // bare modifier for layout, appended after the gesture). The library
                // captures these callbacks once (pointer-input scope, no recomposition
                // re-run), so the real bodies live in holders that are re-assigned on
                // every recomposition with fresh task/tasks/display values; the
                // wrappers handed to the library read the current holder at call time.
                val dragStartedRef = remember { mutableStateOf<(Any) -> Unit>({ _ -> }) }
                val dragStoppedRef = remember { mutableStateOf<() -> Unit>({}) }
                dragStartedRef.value = { _ ->
                    // The pointer has been observed at every pressed event since
                    // the down; the last value is the grab position the indent
                    // delta is measured from.
                    dragStartPointerX = pointerX
                    dragTask = ReparentDrag(task.id, display.indexOf(sectionIndex))
                }
                dragStoppedRef.value = {
                    dragIndentLevel = 0
                    val drag = dragTask
                    dragTask = null
                    // Look the task and its settled position up FRESH from the
                    // current display and data source, keyed by the task id (the
                    // holder makes this run in the current composition's scope even
                    // when the library fires a callback it captured earlier).
                    val pos = if (drag != null) display.indexOfFirst { keyOf(it) == drag.taskId } else -1
                    if (drag != null && pos >= 0) {
                        val freshTask = tasksRef.getItem(display[pos])
                        val target = reparentTarget(freshTask, pos)
                        when {
                            // Indented release (pointer more than half an indent
                            // step right of the grab point): the drop makes the
                            // task a subtask of the nearest shallower-or-equal row
                            // above the settled position, appended to its
                            // children. Legal also in place: a straight horizontal
                            // drag reparents the task in its own slot (the native
                            // app's two-item case). A reparent the planner rejects
                            // (own subtree, cross-calendar) settles back.
                            target != null -> onTaskReparent(freshTask.id, target)
                            // Outdented release (pointer more than half an indent
                            // step left of the grab point): the task moves one
                            // indent level shallower at the settled position.
                            isOutdented(pointerX) && freshTask.indent > 0 ->
                                onTaskOutdent(freshTask.id, pos)
                            // A non-indented, non-outdented release keeps the
                            // settled sibling position, exactly as the plain
                            // reorder path does; a release that settles back on
                            // the same row is a no-op.
                            pos != drag.startPos -> onTaskDrop(freshTask.id, pos)
                        }
                    }
                }
                val handle = Modifier.draggableHandle(
                    onDragStarted = { dragStartedRef.value(it) },
                    onDragStopped = { dragStoppedRef.value() },
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
                        dragIndentOffset = if (dragTask?.taskId == task.id) {
                            dragIndentOffset
                        } else {
                            0.dp
                        },
                    )
                }
            }
        }
        }
    }
}

/** The task being dragged and the display position it left from. */
private data class ReparentDrag(val taskId: Long, val startPos: Int)

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
