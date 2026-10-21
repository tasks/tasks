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
 * own [PullToRefreshState] from them, so the shared indicator and the shared
 * threshold trigger behave exactly as they do for a touch pull. Anything the list
 * consumed is left alone, so the built-in touch path and the box's own wheel residual
 * handling always win and double-driving is impossible.
 *
 * A wheel notch is far smaller than a touch drag, so the wheel delta is amplified
 * ([WHEEL_GAIN]) before it is mapped through the box's own tension model. This makes a
 * short wheel pull (a few notches) travel the same indicator distance as a full touch
 * pull — i.e. it fills the indicator to the full-pull point (distanceFraction 1) and
 * releases it to the shared threshold trigger — so the indicator comes into view,
 * spins, and retracts exactly as it does on the fling path, instead of stopping part
 * way behind the top bar.
 *
 * Unlike a touch drag, a wheel has no release event, so the pull is not completed by
 * the box's own trigger; the shared threshold trigger (see [CompletePullToRefresh])
 * completes it once the driven state reaches a full pull. A pull that stops short
 * retracts itself once the wheel goes quiet, which is the "release" a wheel never
 * provides.
 *
 * On desktop the box's own pull path is disabled (`enabled = !isDesktop`), because with
 * wheel and trackpad input it triggers on incidental top-edge residuals from normal
 * scrolling; this handler is then the desktop trigger. On Android and iOS the box keeps
 * its native touch path and this handler never fires: a drag is never an unconsumed
 * scroll event, so on touch this handler only ever sees a hardware wheel, on which the
 * list itself is inert.
 *
 * @param state the pull-to-refresh state shared with the box (drives the indicator and
 *   the threshold trigger)
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
    // The raw wheel pull that maps to the indicator's maximum travel (200% overshoot).
    val maxPullPx = 2f.pullPx(thresholdPx)
    val scope = rememberCoroutineScope()
    val refreshingNow by rememberUpdatedState(isRefreshing)

    Box(
        modifier = Modifier.pointerInput(Unit) {
            // Accumulated pull in raw pixels (before the wheel gain and the box's drag
            // multiplier), owned by this pointer session. The pointer event scope is
            // restricted to awaiting events, so the suspending state calls are made
            // from the composition scope, and the accumulation itself reads a local
            // rather than the (possibly mid-flight) state.
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
                    if (pullPx == 0f) {
                        pullPx = state.distanceFraction.pullPx(thresholdPx)
                    }
                    // Wheel up (dy < 0) pulls; wheel down retracts. Capped at the raw
                    // distance that maps to the box's 200% overshoot, so the
                    // indicator never runs off the end of its travel.
                    pullPx = (pullPx - dy).coerceIn(0f, maxPullPx)
                    event.changes.forEach { it.consume() }
                    scope.launch { state.snapTo(pullPx.fraction(thresholdPx)) }
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

private const val WHEEL_GAIN = 5.0f

private const val PULL_TO_REFRESH_BOX_DRAG_MULTIPLIER = 0.5f

private fun Float.fraction(thresholdPx: Float): Float {
    val adjusted = this * WHEEL_GAIN * PULL_TO_REFRESH_BOX_DRAG_MULTIPLIER
    if (adjusted <= thresholdPx) return adjusted / thresholdPx
    val t = (adjusted / thresholdPx - 1f).coerceIn(0f, 2f)
    return 1f + t - t * t / 4f
}

private fun Float.pullPx(thresholdPx: Float): Float {
    val scaled = WHEEL_GAIN * PULL_TO_REFRESH_BOX_DRAG_MULTIPLIER
    if (this <= 1f) {
        return this * thresholdPx / scaled
    }
    val t = 2f - 2f * sqrt((2f - this).coerceAtLeast(0f))
    return (1f + t) * thresholdPx / scaled
}
