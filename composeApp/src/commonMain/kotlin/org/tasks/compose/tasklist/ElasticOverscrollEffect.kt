package org.tasks.compose.tasklist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A simple elastic overscroll effect for scrollable lists, used on platforms whose composition
 * provides no platform overscroll of its own (Compose Multiplatform desktop: the default
 * overscroll factory is null, so lists have no edge behaviour at all).
 *
 * Follows the library's documented [OverscrollEffect] contract (see the `OverscrollSample` in
 * the androidx.compose.foundation sources): the scrollable calls [applyToScroll] after its own
 * nested-scroll pass, and whatever delta was left over is absorbed into a damped offset that
 * translates the content. Scrolling against the tension relaxes it first, and the offset
 * springs back to zero when the gesture ends ([applyToFling] for touch, or the settle timeout
 * below, which also covers mouse wheels - those never fling).
 */
internal class ElasticOverscrollEffect(
    private val scope: CoroutineScope,
    private val maxOffsetPx: Float,
    private val tension: Float = 0.3f,
    private val settleMillis: Long = 200L,
) : OverscrollEffect {

    private val offset = Animatable(0f)
    private var settleJob: Job? = null

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        // Relaxation: if the user scrolls against the existing tension, shrink the tension
        // first and only hand the remainder to the scrollable.
        var forScroll = delta
        var relaxed = Offset.Zero
        val current = offset.value
        if (abs(current) > 0.5f && sign(forScroll.y) != 0f && sign(forScroll.y) != sign(current)) {
            val reduce = abs(current).coerceAtMost(abs(forScroll.y))
            scope.launch { offset.snapTo(current - reduce * sign(current)) }
            relaxed = Offset(0f, reduce * sign(forScroll.y))
            forScroll = forScroll - relaxed
        }

        val consumed = performScroll(forScroll)
        val leftover = forScroll - consumed

        // Anything the scrollable (and its nested-scroll connections) could not consume
        // becomes edge tension. User input only: fling/animation deltas are not re-overscrolled.
        if (abs(leftover.y) > 0.5f && source == NestedScrollSource.UserInput) {
            scheduleSettle()
            scope.launch {
                offset.snapTo((offset.value + leftover.y * tension).coerceIn(-maxOffsetPx, maxOffsetPx))
            }
        }
        return relaxed + consumed
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        performFling(velocity)
        settleJob?.cancel()
        settleJob = null
        offset.animateTo(targetValue = 0f, initialVelocity = velocity.y, animationSpec = spring())
    }

    override val isInProgress: Boolean
        get() = offset.value != 0f

    private fun scheduleSettle() {
        settleJob?.cancel()
        settleJob = scope.launch {
            delay(settleMillis)
            offset.animateTo(targetValue = 0f, animationSpec = spring())
        }
    }

    /** Translates the content by the current tension offset. */
    override val node: DelegatableNode =
        object : Modifier.Node(), LayoutModifierNode {
            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints,
            ): MeasureResult {
                val placeable = measurable.measure(constraints)
                return layout(placeable.width, placeable.height) {
                    placeable.place(0, offset.value.roundToInt())
                }
            }
        }
}

/**
 * Returns the [OverscrollEffect] to use for the task list: the platform effect if the
 * composition provides one (Android's edge glow, iOS's platform effect), otherwise an
 * [ElasticOverscrollEffect] - the Compose Multiplatform desktop default factory is null,
 * so without this the list would have no overscroll at all and no edge behaviour to pull
 * against when the content fits the viewport.
 */
@Composable
internal fun rememberTaskListOverscrollEffect(): OverscrollEffect {
    val platformEffect = rememberOverscrollEffect()
    val scope = rememberCoroutineScope()
    val maxOffsetPx = with(LocalDensity.current) { 80.dp.toPx() }
    val elastic = remember(scope, maxOffsetPx) { ElasticOverscrollEffect(scope, maxOffsetPx) }
    return platformEffect ?: elastic
}
