package org.tasks.compose.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.chips.Chip
import org.tasks.compose.drag.rememberBlockDragState
import org.tasks.compose.drag.BlockDragState
import org.tasks.compose.drag.IndentDrag
import org.tasks.compose.drag.IndentStep
import org.tasks.compose.drag.rememberIndentDrag
import org.tasks.compose.drag.tabToIndent
import org.tasks.compose.chips.SubtaskChip
import org.tasks.kmp.formatNumber
import org.tasks.compose.priorityColor
import org.tasks.compose.settings.CardPosition
import org.tasks.data.SubtaskNode
import org.tasks.data.SubtaskRow
import org.tasks.data.dropRange
import org.tasks.data.travellingWith
import org.tasks.data.isHidden
import org.tasks.themes.TasksIcons
import sh.calvin.reorderable.ReorderableListItemScope
import sh.calvin.reorderable.ReorderableColumn
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.TEA_add_subtask
import tasks.kmp.generated.resources.edit_task
import tasks.kmp.generated.resources.no_title
import tasks.kmp.generated.resources.subtasks
import tasks.kmp.generated.resources.delete
import tasks.kmp.generated.resources.undo_delete

private const val DisabledAlpha = 0.38f

internal const val DRAG_HANDLE_TAG = "subtask-drag-handle:"

private val FirstLineOffset = 12.dp

private val CompactRowWidth = 400.dp

private val IconButtonInset = 12.dp

@Composable
fun SubtasksSection(
    subtasks: List<SubtaskRow>,
    focusSubtask: String?,
    unsupportedMessage: String?,
    flattenWarning: String?,
    allowsNesting: Boolean,
    bottomInset: Dp,
    onAddSubtask: () -> Unit,
    onAddAfter: (SubtaskNode) -> Unit,
    onOpenSubtask: (SubtaskNode) -> Unit,
    onCompleteSubtask: (SubtaskNode) -> Unit,
    onToggleCollapsed: (SubtaskNode) -> Unit,
    onMoveSubtask: (fromKey: String, toKey: String, indent: Int?) -> Unit,
    onIndentSubtask: (SubtaskNode, steps: Int) -> Unit,
    onTitleChange: (SubtaskNode, String) -> Unit,
    onRemoveSubtask: (SubtaskNode) -> Unit,
    onBackspaceSubtask: (SubtaskNode) -> Unit,
    onRestoreSubtask: (SubtaskNode) -> Unit,
    onSubtaskFocused: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val compact = maxWidth < CompactRowWidth
        Column(
            modifier = Modifier.revealAbove(bottomInset),
            verticalArrangement = Arrangement.spacedBy(TaskEditCardGap),
        ) {
            val position = if (subtasks.isEmpty()) CardPosition.Only else CardPosition.First
            val canAdd = unsupportedMessage == null
            if (unsupportedMessage != null) {
                TaskEditCard(position = position) {
                    TaskEditCardRowContent(
                        value = unsupportedMessage,
                        valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        title = stringResource(Res.string.subtasks),
                        icon = TasksIcons.SUBTASK,
                    )
                }
            } else {
                TaskEditCard(onClick = onAddSubtask, position = position) {
                    TaskEditCardRowContent(
                        value = stringResource(Res.string.TEA_add_subtask),
                        valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        title = stringResource(Res.string.subtasks),
                        icon = TasksIcons.SUBTASK,
                    )
                }
            }
            if (flattenWarning != null) {
                TaskEditCard(position = CardPosition.Middle) {
                    TaskEditCardRowContent(
                        value = flattenWarning,
                        valueColor = MaterialTheme.colorScheme.error,
                        icon = TasksIcons.WARNING,
                        iconTint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (subtasks.isEmpty()) {
                return@Column
            }
            val block = rememberBlockDragState()
            val caret = rememberCaretHandoff(subtasks)
            TaskEditCard(position = CardPosition.Last) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    key(block.rebuildKey) {
                        ReorderableColumn(
                            list = subtasks,
                            onSettle = { _, _ -> },
                        ) { index, row, _ ->
                            val node = row.node
                            ReorderableItem(
                                modifier = Modifier.onGloballyPositioned {
                                    block.bounds.put(index, it.positionInParent().y, it.size.height)
                                },
                            ) {
                                if (block.isCarried(index)) {
                                    Spacer(modifier = Modifier.fillMaxWidth())
                                    return@ReorderableItem
                                }
                                key(node.key) {
                                    val drag = rememberSubtaskDrag(
                                        row = row,
                                        index = index,
                                        rows = subtasks,
                                        allowsNesting = allowsNesting,
                                        block = block,
                                        onIndentSubtask = onIndentSubtask,
                                        onMoveSubtask = onMoveSubtask,
                                    )
                                    SubtaskRowContent(
                                        node = node,
                                        completed = row.completed,
                                        indent = drag.indent,
                                        indentRange = { drag.range },
                                        children = row.children,
                                        chipCount = row.chipCount,
                                        collapsed = row.collapsed,
                                        onToggleCollapsed = { onToggleCollapsed(node) },
                                        carrying = if (block.isDragging(index)) row.children else 0,
                                        focused = node.key == focusSubtask,
                                        compact = compact,
                                        handleModifier = drag.handle,
                                        onOpen = { onOpenSubtask(node) },
                                        onTitleChange = { title -> onTitleChange(node, title) },
                                        onComplete = { onCompleteSubtask(node) },
                                        onRemove = {
                                            subtasks
                                                .caretAfterRemoving(index, keepsNested = node.isNew)
                                                ?.let { (key, at) -> caret.handTo(key, at) }
                                            onBackspaceSubtask(node)
                                        },
                                        onDelete = { onRemoveSubtask(node) },
                                        onRestore = { onRestoreSubtask(node) },
                                        onAddAnother = { if (canAdd) onAddAfter(node) },
                                        onIndentChange = drag.reNest,
                                        onMoveFocus = { step ->
                                            subtasks.caretAfterMoving(index, step)?.let { (key, at) ->
                                                caret.handTo(key, at)
                                            }
                                        },
                                        caretLanding = caret.landingFor(node.key),
                                        onCaretPlaced = caret::placed,
                                        onFocused = { onSubtaskFocused(node.key) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (block.reservedPx > 0) {
                Spacer(
                    modifier = Modifier.height(
                        (with(LocalDensity.current) { block.reservedPx.toDp() } - TaskEditCardGap)
                            .coerceAtLeast(0.dp)
                    )
                )
            }
        }
    }
}

@Stable
private class SubtaskDrag(
    private val drag: IndentDrag,
    val handle: Modifier,
    val reNest: (Int) -> Unit,
) {
    val indent: Int get() = drag.indent

    val range: IntRange get() = drag.range
}

@Composable
private fun ReorderableListItemScope.rememberSubtaskDrag(
    row: SubtaskRow,
    index: Int,
    rows: List<SubtaskRow>,
    allowsNesting: Boolean,
    block: BlockDragState,
    onIndentSubtask: (SubtaskNode, Int) -> Unit,
    onMoveSubtask: (fromKey: String, toKey: String, indent: Int?) -> Unit,
): SubtaskDrag {
    val node = row.node
    val indent = row.indent
    val rangeAt: (Int) -> IntRange = { over ->
        rows.dropRange(from = index, to = over, allowsNesting = allowsNesting)
    }
    val reNest: (Int) -> Unit = { onIndentSubtask(node, it - indent) }
    val drag = rememberIndentDrag(
        key = node.key,
        index = index,
        indent = indent,
        bounds = block.bounds,
        rowCount = rows.size,
        rangeAt = rangeAt,
        landingOf = block.landingOf,
        onReNest = reNest,
        onDrop = { over, dropIndent -> onMoveSubtask(node.key, rows[over].key, dropIndent) },
    )
    val handle = Modifier.draggableHandle(
        onDragStarted = {
            block.started(index = index, block = rows.travellingWith(index + 1, row))
            drag.onDragStarted()
        },
        onDragStopped = {
            block.stopped(drag.onDragStopped())
        },
        dragGestureDetector = drag.detector,
    )
    return SubtaskDrag(drag = drag, handle = handle, reNest = reNest)
}

@Composable
private fun SubtaskRowContent(
    node: SubtaskNode,
    completed: Boolean,
    indent: Int,
    indentRange: () -> IntRange,
    children: Int,
    chipCount: Int,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    carrying: Int,
    focused: Boolean,
    compact: Boolean,
    handleModifier: Modifier,
    onOpen: () -> Unit,
    onTitleChange: (String) -> Unit,
    onComplete: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onAddAnother: () -> Unit,
    onIndentChange: (Int) -> Unit,
    onMoveFocus: (Int) -> Unit,
    caretLanding: CaretLanding?,
    onCaretPlaced: () -> Unit,
    onFocused: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    val doomed = node.deleted
    val iconTint = if (doomed) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .tabToIndent(
                indent = indent,
                range = indentRange,
                onIndentChange = onIndentChange,
                swallowWhenBlocked = editing,
            )
            .then(
                if (doomed) {
                    Modifier.background(MaterialTheme.colorScheme.errorContainer)
                } else {
                    Modifier
                }
            )
            .padding(
                start = TaskEditCardHorizontalPadding + IndentStep * indent,
                end = (TaskEditCardHorizontalPadding - IconButtonInset).coerceAtLeast(0.dp),
            ),
    ) {
        if (doomed) {
            Spacer(modifier = Modifier.padding(top = FirstLineOffset).size(24.dp))
        } else {
            DragHandle(
                modifier = handleModifier
                    .testTag("$DRAG_HANDLE_TAG${node.key}")
                    .padding(top = FirstLineOffset),
                tint = iconTint.copy(alpha = DisabledAlpha),
            )
        }
        CompleteButton(
            completed = completed,
            priority = node.task.priority,
            onClick = onComplete,
            enabled = !doomed,
            modifier = Modifier.padding(top = FirstLineOffset - 8.dp),
        )
        val titleStyle = MaterialTheme.typography.bodyLarge.copy(
            color = when {
                doomed -> MaterialTheme.colorScheme.onErrorContainer
                completed || node.task.isHidden -> MaterialTheme.colorScheme.outline
                else -> MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (completed || doomed) {
                TextDecoration.LineThrough
            } else {
                null
            },
            textDirection = TextDirection.Content,
        )
        if (doomed) {
            Text(
                text = node.title?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.no_title),
                style = titleStyle,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
            )
        } else {
            SubtaskTitleField(
                key = node.key,
                title = node.title,
                textStyle = titleStyle,
                focused = focused,
                caretLanding = caretLanding,
                onEditingChange = { editing = it },
                onTitleChange = onTitleChange,
                onAddAnother = onAddAnother,
                onRemove = onRemove,
                onMoveFocus = onMoveFocus,
                onCaretPlaced = onCaretPlaced,
                onFocused = onFocused,
            )
        }
        CarryingBadge(carrying)
        if (children > 0 && carrying == 0 && !doomed) {
            AgainstFirstLine {
                SubtaskChip(
                    collapsed = collapsed,
                    children = chipCount,
                    onClick = onToggleCollapsed,
                )
            }
        }
        SubtaskRowActions(
            doomed = doomed,
            compact = compact,
            tint = iconTint,
            onOpen = onOpen,
            onDelete = onDelete,
            onRestore = onRestore,
        )
    }
}

@Composable
private fun SubtaskRowActions(
    doomed: Boolean,
    compact: Boolean,
    tint: Color,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
) {
    when {
        doomed -> IconButton(onClick = onRestore) {
            Icon(
                imageVector = Icons.Outlined.RestoreFromTrash,
                contentDescription = stringResource(Res.string.undo_delete),
                tint = tint,
            )
        }
        compact -> SubtaskMenu(onOpen = onOpen, onDelete = onDelete)
        else -> {
            IconButton(onClick = onOpen) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(Res.string.edit_task),
                    tint = tint,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(Res.string.delete),
                    tint = tint,
                )
            }
        }
    }
}

@Composable
private fun CompleteButton(
    completed: Boolean,
    priority: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(40.dp)) {
        Icon(
            imageVector = if (completed) {
                Icons.Filled.CheckCircle
            } else {
                Icons.Outlined.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = if (completed) MaterialTheme.colorScheme.outline else priorityColor(priority),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun DragHandle(modifier: Modifier, tint: Color) {
    Icon(
        imageVector = Icons.Outlined.DragIndicator,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(24.dp),
    )
}

@Composable
private fun CarryingBadge(count: Int) {
    if (count <= 0) {
        return
    }
    AgainstFirstLine {
        Chip(
            text = "+${formatNumber(count)}",
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun AgainstFirstLine(content: @Composable () -> Unit) {
    val lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
    val height = if (lineHeight.isSp) {
        with(LocalDensity.current) { lineHeight.toDp() }
    } else {
        FirstLineOffset * 2
    }
    Box(
        modifier = Modifier.height(FirstLineOffset * 2 + height),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.revealAbove(inset: Dp): Modifier = composed {
    val insetPx = with(LocalDensity.current) { inset.toPx() }
    val responder = remember(insetPx) {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect =
                localRect.copy(bottom = localRect.bottom + insetPx)

            override suspend fun bringChildIntoView(localRect: () -> Rect?) = Unit
        }
    }
    bringIntoViewResponder(responder)
}
