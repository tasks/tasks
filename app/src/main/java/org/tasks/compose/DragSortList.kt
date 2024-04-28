package org.tasks.compose

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * Drag - drop to reorder elements of LazyColumn
 *
 * Implementation is based on:
 *      https://github.com/realityexpander/DragDropColumnCompose
 *
 * Scheme of use:
 *      1. Hoist state of the LazyColumn (create your own and set it as LazyColumn parameter)
 *      2. Create and remember DragDropState object by call to "rememberDragDropState"
 *      3. Use Modifier.doDrag in the LazyColumn
 *      4. enclose LazyList items into DraggableItem
 * **/

class DragDropState internal constructor(
    val state: LazyListState,
    private val scope: CoroutineScope,
    private val confirmDrag: (Int) -> Boolean,
    private val onSwap: (Int, Int) -> Unit
) {
    /* primary ID of the item being dragged */
    var draggedItemIndex by mutableStateOf<Int?>(null)

    private var draggedDistance by mutableFloatStateOf(0f)
    private var draggingElementOffset: Int = 0 // cached drugged element offset and size
    private var draggingElementSize: Int = -1  // size must not be negative when dragging is in progress

    private var overscrollJob by mutableStateOf<Job?>( null )

    /* sibling of draggingElementOffset, not cached, for use in animation  */
    internal val draggingItemOffset: Float
        get() = state.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == draggedItemIndex }
            ?.let { item ->
                draggingElementOffset + draggedDistance - item.offset
            } ?: 0f

    fun itemAtOffset(offsetY: Float): LazyListItemInfo? =
        state.layoutInfo.visibleItemsInfo.firstOrNull {
                item -> offsetY.toInt() in item.offset..(item.offset + item.size)
        }

    fun startDragging(item: LazyListItemInfo?) {
        //Log.d("HADY", "start dragging ${item}")
        if (item != null && confirmDrag(item.index)) {
            draggedItemIndex = item.index
            draggingElementOffset = item.offset
            draggingElementSize = item.size
            assert(item.size >= 0) { "Invalid size of element ${item.size}" }
        }
    }

    fun stopDragging() {
        draggedDistance = 0f
        draggedItemIndex = null
        draggingElementOffset = 0
        draggingElementSize = -1
        overscrollJob?.cancel()
    }

    fun onDrag(offset: Offset) {

        draggedDistance += offset.y

        //if (draggedDistance != 0f) {
        if (draggedItemIndex != null) {
            assert(draggingElementSize >= 0) { "FATAL: Invalid dragging element" }

            val startOffset = draggingElementOffset + draggedDistance
            val endOffset = startOffset + draggingElementSize

            val draggedIndex = draggedItemIndex
            val dragged = draggedIndex?.let { index ->
                state.layoutInfo.visibleItemsInfo.getOrNull(
                    index - state.layoutInfo.visibleItemsInfo.first().index
                )
            }
            if (dragged != null) {
                val up = (startOffset - dragged.offset) > 0
                val hovered =
                    if (up) itemAtOffset(startOffset + 0.1f * draggingElementSize)
                    else itemAtOffset(endOffset - 0.1f * draggingElementSize)

                if (hovered != null) {
                    scope.launch { onSwap(draggedIndex, hovered.index) }
                    draggedItemIndex = hovered.index
                }

                if (overscrollJob?.isActive != true) {
                    val overscroll = when {
                        draggedDistance > 0 -> max(endOffset - state.layoutInfo.viewportEndOffset+50f, 0f)
                        draggedDistance < 0 -> min(startOffset - state.layoutInfo.viewportStartOffset-50f, 0f)
                        else -> 0f
                    }
                    if (overscroll != 0f) {
                        overscrollJob = scope.launch {
                            state.animateScrollBy(
                                overscroll * 1.3f, tween(easing = FastOutLinearInEasing)
                            )
                        }
                    }
                }
            }
        }
    } /* end onDrag */
} /* end DragDropState */

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    confirmDrag: (Int) -> Boolean = { true },
    onSwap: (Int, Int) -> Unit
): DragDropState {
    val scope = rememberCoroutineScope()
    val state = remember(lazyListState) {
        DragDropState(
            state = lazyListState,
            onSwap = onSwap,
            scope = scope,
            confirmDrag = confirmDrag
        )
    }
    return state
}

fun Modifier.doDrag(dragDropState: DragDropState): Modifier =
    this.pointerInput(dragDropState) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                dragDropState.startDragging(dragDropState.itemAtOffset(offset.y))
            },
            onDrag = { change, offset ->
                change.consume()
                dragDropState.onDrag(offset)
            },
            onDragEnd = { dragDropState.stopDragging() },
            onDragCancel = { dragDropState.stopDragging() }
        )
    }

@ExperimentalFoundationApi
@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val current: Float by animateFloatAsState(
        targetValue = dragDropState.draggingItemOffset * 0.67f
    )

    val dragging = index == dragDropState.draggedItemIndex

    val draggingModifier = if (dragging) {
        Modifier
            .zIndex(1f)
            .graphicsLayer { translationY = current }
    } else {
        Modifier.animateItemPlacement(
            tween(easing = FastOutLinearInEasing)
        )
    }
    Box(modifier = modifier.then(draggingModifier)) {
        content(dragging)
    }
}
