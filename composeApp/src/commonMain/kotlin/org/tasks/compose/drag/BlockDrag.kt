package org.tasks.compose.drag

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.tasks.data.INDENT_STEP_DP
import sh.calvin.reorderable.DragGestureDetector
import kotlin.math.roundToInt

internal val IndentStep = INDENT_STEP_DP.dp

internal fun Modifier.tabToIndent(
    indent: Int,
    range: () -> IntRange,
    onIndentChange: (Int) -> Unit,
    swallowWhenBlocked: Boolean = false,
): Modifier = composed {
    val currentRange by rememberUpdatedState(range)
    val currentIndent by rememberUpdatedState(indent)
    val onChange by rememberUpdatedState(onIndentChange)
    val swallow by rememberUpdatedState(swallowWhenBlocked)
    onPreviewKeyEvent { event ->
        if (event.key != Key.Tab) {
            return@onPreviewKeyEvent false
        }
        val target = tabTarget(currentIndent, event.isShiftPressed, currentRange())
        val moves = target != currentIndent
        if (moves && event.type == KeyEventType.KeyDown) {
            onChange(target)
        }
        moves || swallow
    }
}

internal fun tabTarget(indent: Int, shift: Boolean, range: IntRange): Int =
    (indent + if (shift) -1 else 1).coerceIn(range)

internal fun indentPreview(base: Int, draggedX: Float, step: Float, range: IntRange): Int =
    (base + (draggedX / step).roundToInt()).coerceIn(range)

internal sealed interface IndentDrop {
    data object Nothing : IndentDrop
    data class ReNest(val indent: Int) : IndentDrop
    data class Move(val landing: Int, val indent: Int?) : IndentDrop
}

internal fun indentDrop(landing: Int?, target: Int?, base: Int): IndentDrop = when {
    landing != null -> IndentDrop.Move(landing, target)
    target != null && target != base -> IndentDrop.ReNest(target)
    else -> IndentDrop.Nothing
}

@Composable
internal fun rememberIndentDrag(
    key: Any,
    index: Int,
    indent: Int,
    bounds: RowBounds,
    rowCount: Int,
    rangeAt: (landing: Int) -> IntRange,
    landingOf: (from: Int, to: Int) -> Int?,
    onReNest: (Int) -> Unit,
    onDrop: (landing: Int, indent: Int?) -> Unit,
): IndentDrag {
    val step = with(LocalDensity.current) { IndentStep.toPx() }
    val currentIndex = rememberUpdatedState(index)
    val currentIndent = rememberUpdatedState(indent)
    val currentCount = rememberUpdatedState(rowCount)
    val currentRange = rememberUpdatedState(rangeAt)
    val landing = rememberUpdatedState(landingOf)
    val reNest = rememberUpdatedState(onReNest)
    val drop = rememberUpdatedState(onDrop)
    return remember(key, step) {
        IndentDrag(
            step = step,
            index = currentIndex,
            base = currentIndent,
            rowCount = currentCount,
            rangeAt = currentRange,
            landingOf = landing,
            bounds = bounds,
            onReNest = reNest,
            onDrop = drop,
        )
    }
}

@Stable
internal class IndentDrag(
    private val step: Float,
    private val index: State<Int>,
    private val base: State<Int>,
    private val rowCount: State<Int>,
    private val rangeAt: State<(Int) -> IntRange>,
    private val landingOf: State<(Int, Int) -> Int?>,
    private val bounds: RowBounds,
    private val onReNest: State<(Int) -> Unit>,
    private val onDrop: State<(Int, Int?) -> Unit>,
) {
    private var preview by mutableStateOf<Int?>(null)
    private var liveRange by mutableStateOf<IntRange?>(null)
    private var draggedX = 0f
    private var draggedY = 0f

    val indent: Int get() = preview ?: base.value

    val range: IntRange get() = liveRange ?: rangeAt.value(index.value)

    val detector = object : DragGestureDetector {
        override suspend fun PointerInputScope.detect(
            onDragStart: (Offset) -> Unit,
            onDragEnd: () -> Unit,
            onDragCancel: () -> Unit,
            onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
        ) {
            with(DragGestureDetector.Press) {
                detect(onDragStart, onDragEnd, onDragCancel) { change, amount ->
                    draggedX += amount.x
                    draggedY += amount.y
                    val over = bounds.targetIndex(index.value, draggedY, rowCount.value)
                    val landing = landingOf.value(index.value, over) ?: index.value
                    val range = rangeAt.value(landing)
                    liveRange = range
                    preview = indentPreview(base.value, draggedX, step, range)
                    onDrag(change, amount)
                }
            }
        }
    }

    fun onDragStarted() {
        draggedX = 0f
        draggedY = 0f
        preview = null
        liveRange = null
    }

    fun onDragStopped(): IndentDrop {
        val target = preview
        val over = bounds.targetIndex(index.value, draggedY, rowCount.value)
        val landing = landingOf.value(index.value, over)
        preview = null
        liveRange = null
        draggedX = 0f
        draggedY = 0f
        val drop = indentDrop(landing, target, base.value)
        when (drop) {
            is IndentDrop.Move -> onDrop.value(drop.landing, drop.indent)
            is IndentDrop.ReNest -> onReNest.value(drop.indent)
            IndentDrop.Nothing -> Unit
        }
        return drop
    }
}

@Stable
internal class RowBounds {
    private val tops = mutableMapOf<Int, Float>()
    private val sizes = mutableMapOf<Int, Int>()

    internal fun put(index: Int, top: Float, size: Int) {
        tops[index] = top
        sizes[index] = size
    }

    internal fun heightOf(rows: IntRange): Int = rows.sumOf { sizes[it] ?: 0 }

    private fun center(index: Int): Float? {
        val top = tops[index] ?: return null
        val size = sizes[index] ?: return null
        return top + size / 2f
    }

    internal fun targetIndex(from: Int, dy: Float, count: Int): Int {
        val top = tops[from] ?: return from
        val size = sizes[from] ?: return from
        val currentStart = top + dy
        val currentEnd = currentStart + size
        val originalEnd = top + size
        if (dy < 0) {
            for (i in 0 until count) {
                if (i == from) continue
                val center = center(i) ?: continue
                if (center >= currentStart && center < top) {
                    return i
                }
            }
        } else if (dy > 0) {
            var last = from
            for (i in 0 until count) {
                if (i == from) continue
                val center = center(i) ?: continue
                if (center >= originalEnd && center < currentEnd) {
                    last = i
                }
            }
            return last
        }
        return from
    }
}

@Stable
internal class BlockDragState {
    internal val bounds = RowBounds()

    internal var rebuildKey by mutableIntStateOf(0)
        private set

    internal var carried by mutableStateOf(IntRange.EMPTY)
        private set

    internal var reservedPx by mutableIntStateOf(0)
        private set

    internal var draggedIndex by mutableIntStateOf(-1)
        private set

    internal fun isCarried(index: Int): Boolean = index in carried

    internal fun isDragging(index: Int): Boolean = index == draggedIndex

    internal val landingOf: (from: Int, to: Int) -> Int? = { from, to ->
        when {
            to == from -> null
            to in carried -> null
            else -> to
        }
    }

    internal fun started(index: Int, block: IntRange) {
        reservedPx = bounds.heightOf(block)
        carried = block
        draggedIndex = index
    }

    internal fun stopped(drop: IndentDrop) {
        draggedIndex = -1
        carried = IntRange.EMPTY
        reservedPx = 0
        if (drop == IndentDrop.Nothing) {
            rebuildKey++
        }
    }
}

@Composable
internal fun rememberBlockDragState(): BlockDragState = remember { BlockDragState() }
