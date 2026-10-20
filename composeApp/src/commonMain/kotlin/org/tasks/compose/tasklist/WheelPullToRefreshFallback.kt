package org.tasks.compose.tasklist

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pull-to-refresh for wheel and trackpad input, covering the case the built-in
 * [androidx.compose.material3.pulltorefresh.PullToRefreshBox] cannot reach.
 *
 * The box is a nested-scroll connection: it only sees scroll residuals from a
 * scrollable that has already claimed and scrolled. Foundation's wheel handling, in
 * both upstream and the Compose Multiplatform fork (verified in foundation-desktop
 * 1.12.2 and 1.13.0-alpha02), claims a wheel event only when the list can scroll in
 * its direction, and the desktop overscroll factory returns null — so at the top of a
 * list that fits the viewport, wheel-up is claimed by nobody, the list never scrolls,
 * and the box's pull never starts. The same is true of any wheel that does not
 * straddle an edge.
 *
 * This observes the raw pointer events the list left unconsumed — checked on the main
 * pass, after every descendant scrollable has had its chance — and drives the box's
 * own [PullToRefreshState] from them, mapped through the box's own tension model so a
 * wheel pull travels exactly the distance a touch pull would. Anything the list
 * consumed is left alone, so the built-in touch path and the box's own wheel
 * residual handling always win and double-driving is impossible.
 *
 * A pull that fills the threshold is completed into a refresh by
 * [CompletePullToRefresh], which watches the same state; a pull that stops short (or
 * one that does not complete) retracts itself once the wheel goes quiet, which is the
 * "release" a wheel never provides.
 *
 * Touch is unaffected: a drag is never an unconsumed scroll event, so on Android and
 * iOS this handler only ever sees a hardware wheel, on which the list itself is inert.
 *
 * @param state the pull-to-refresh state shared with the box
 * @param isRefreshing whether a refresh is already in progress (the box's
 *   `isRefreshing`); while true the box owns the state and this handler ignores the
 *   wheel
 * @param threshold the pull distance that counts as a full pull; pass the same value
 *   as the box's `threshold`
 * @param retractIdleMillis how long the wheel may be quiet before the pull retracts
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun WheelPullToRefreshFallback(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    threshold: Dp,
    retractIdleMillis: Long = 400,
    content: @Composable () -> Unit,
) {
    val thresholdPx = with(LocalDensity.current) { threshold.roundToPx() }.toFloat()
    val scope = rememberCoroutineScope()
    val refreshingNow by rememberUpdatedState(isRefreshing)

    Box(
        modifier = Modifier.pointerInput(Unit) {
            // Accumulated pull in raw pixels (before the box's drag multiplier), owned
            // by this pointer session. The pointer event scope is restricted to
            // awaiting events, so the suspending state calls are made from the
            // composition scope, and the accumulation itself reads a local rather
            // than the (possibly mid-flight) state.
            var pullPx = 0f
            var retractJob: Job? = null
            awaitPointerEventScope {
                while (true) {
                    // Main pass: the list (and any other scrollable below) has had
                    // its chance, so a scroll left unconsumed here is an orphaned
                    // edge scroll.
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (event.type != PointerEventType.Scroll) continue
                    if (event.changes.all { it.isConsumed }) continue
                    if (refreshingNow) continue
                    val dy = event.changes.sumOf { it.scrollDelta.y.toDouble() }.toFloat()
                    val dx = event.changes.sumOf { it.scrollDelta.x.toDouble() }.toFloat()
                    // Vertical wheel only; ignore horizontal-dominant events.
                    if (dy == 0f || abs(dy) < abs(dx)) continue
                    // Re-sync with the shared state when not mid-pull, so a pull
                    // started by touch (or left mid-animation) is continued rather
                    // than overwritten.
                    if (pullPx == 0f) pullPx = state.distanceFraction.pullPx(thresholdPx)
                    // Wheel up (dy < 0) pulls; wheel down retracts. Capped at the
                    // raw distance that maps to the box's 200% overshoot, so the
                    // indicator never runs off the end of its travel.
                    pullPx = (pullPx - dy).coerceIn(0f, 4f * thresholdPx)
                    event.changes.forEach { it.consume() }
                    val fraction = pullPx.fraction(thresholdPx)
                    scope.launch { state.snapTo(fraction) }
                    retractJob?.cancel()
                    retractJob = scope.launch {
                        delay(retractIdleMillis)
                        if (!refreshingNow) {
                            state.animateToHidden()
                            pullPx = 0f
                        }
                    }
                }
            }
        },
    ) {
        content()
    }
}

/**
 * Maps a raw pull distance in pixels to the box's `PullToRefreshState` fraction,
 * replicating the box's own mapping: raw pull is multiplied by the drag multiplier
 * (0.5), applied linearly up to the threshold, and through a quadratic tension curve
 * beyond it, where the indicator's travel caps at 200% of the threshold.
 */
private fun Float.fraction(thresholdPx: Float): Float {
    val adjusted = this * DRAG_MULTIPLIER
    if (adjusted <= thresholdPx) return adjusted / thresholdPx
    val t = (adjusted / thresholdPx - 1f).coerceIn(0f, 2f)
    return 1f + t - t * t / 4f
}

/** Inverse of [fraction]: the raw pixel distance that maps to [this] box fraction. */
private fun Float.pullPx(thresholdPx: Float): Float {
    if (this <= 1f) return this / DRAG_MULTIPLIER * thresholdPx
    val t = 2f - 2f * sqrt((2f - this).coerceAtLeast(0f))
    return (1f + t) / DRAG_MULTIPLIER * thresholdPx
}

/** The box's `DragMultiplier` (its private constant, mirrored here). */
private const val DRAG_MULTIPLIER = 0.5f
